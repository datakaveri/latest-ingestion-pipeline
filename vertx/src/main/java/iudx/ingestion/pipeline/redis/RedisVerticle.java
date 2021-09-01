package iudx.ingestion.pipeline.redis;

import static iudx.ingestion.pipeline.common.Constants.REDIS_SERVICE_ADDRESS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ingestion.pipeline.common.service.CatalogueService;
import iudx.ingestion.pipeline.common.service.impl.CatalogueServiceImpl;

public class RedisVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(RedisVerticle.class);

  private RedisService redisService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  private RedisClient client;

  private CatalogueService catalogueService;
  private long keysCheckTimer;
  private WebClient webClient;

  @Override
  public void start() throws Exception {
    new RedisClient(vertx, config()).start()
        .onSuccess(handler -> {
          client = handler;
          redisService = new RedisServiceImpl(client);

          binder = new ServiceBinder(vertx);
          consumer = binder
              .setAddress(REDIS_SERVICE_ADDRESS)
              .register(RedisService.class, redisService);
          // initialize and check whether all catalogue RG corresponding keys are present or not, if not then
          // create a key in redis.
          initialize();
        }).onFailure(handler -> {
          LOGGER.error("failed to start redis client");
        });

    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true)
        .setVerifyHost(false)
        .setSsl(true);
    webClient = WebClient.create(vertx, options);
    catalogueService = new CatalogueServiceImpl(webClient, config());

    // check periodically, whether new RG is available in catalogue
    keysCheckTimer = vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), handler -> {
      initialize();
    });
  }

  /**
   * initialize redis with keys for every resource group in catalogue service.
   * 
   * @param client
   */
  private void initialize() {

    Future<List<String>> availableKeysFuture = client.getAllKeys();
    Future<List<String>> availableRgFuture = catalogueService.getAllAvailableRG();

    CompositeFuture.all(List.of(availableRgFuture))
        .onSuccess(handler -> {
          List<String> availableKeys = availableKeysFuture.result();
          List<String> availableRg = availableRgFuture.result();

          Set<String> redisKeyNeededList = difference(availableRg, availableKeys);

          List<Future> createKeysCompositeFuture = new ArrayList<>();
          if (redisKeyNeededList.size() > 0) {
            for (String s : redisKeyNeededList) {
              JsonObject origin = new JsonObject();
              JsonObject pathJson = new JsonObject();
              pathJson.put("_init_d", new JsonObject());
              origin.put(s, pathJson);
              createKeysCompositeFuture.add(client.put(s, ".", origin.toString()));
            }
            CompositeFuture.all(createKeysCompositeFuture).onSuccess(createHandler -> {
              LOGGER.info("all keys created.");
            });
          }
        }).onFailure(handler -> {
          LOGGER.error(handler.getMessage());
        });
  }

  /**
   * difference of keys in two seprate list, get keys not present in redis.
   * 
   * @param availableRgList
   * @param availableKeysList
   * @return
   */
  private Set<String> difference(List<String> availableRgList, List<String> availableKeysList) {

    return availableRgList.stream()
        .map(rg -> rg.split("/")[3])
        .map(key -> key.replaceAll("-", "_"))
        .filter(key -> !availableKeysList.contains(key))
        .collect(Collectors.toSet());
  }


  public void stop() {
    if (client != null) {
      client.close();
    }
  }

}
