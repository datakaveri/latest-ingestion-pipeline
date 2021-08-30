package iudx.ingestion.pipeline.processor;

import static iudx.ingestion.pipeline.common.Constants.MSG_PROCESS_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.REDIS_SERVICE_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.RMQ_SERVICE_ADDRESS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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
import iudx.ingestion.pipeline.rabbitmq.RabbitMQService;
import iudx.ingestion.pipeline.redis.RedisService;

public class ProcessorVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ProcessorVerticle.class);
  private MessageProcessService processor;
  private RabbitMQService rabbitMQService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  private CatalogueService catalogueService;
  private long keysCheckTimer;
  private RedisService redisService;
  private WebClient webClient;

  @Override
  public void start() throws Exception {

    Map<String, Object> mappings = loadMappingFile();
    LOGGER.debug("mapings file :" + mappings);


    rabbitMQService = RabbitMQService.createProxy(vertx, RMQ_SERVICE_ADDRESS);
    processor = new MessageProcessorImpl(vertx, mappings, rabbitMQService);

    binder = new ServiceBinder(vertx);

    consumer = binder
        .setAddress(MSG_PROCESS_ADDRESS)
        .register(MessageProcessService.class, processor);


    WebClientOptions options =
        new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true)
            .setKeyStoreOptions(
                new JksOptions()
                    .setPath(config().getString("keystore"))
                    .setPassword(config().getString("keystorePassword")));
    webClient = WebClient.create(vertx, options);
    catalogueService = new CatalogueServiceImpl(webClient, config());
    redisService = RedisService.createProxy(vertx, REDIS_SERVICE_ADDRESS);

    // check periodically, whether new RG is available in catalogue
    keysCheckTimer = vertx.setPeriodic(TimeUnit.DAYS.toMillis(1), handler -> {
      initialize(redisService);
    });

    // initialize and check whether all catalogue RG corresponding keys are present or not, if not then
    // create a key in redis.
    initialize(redisService);

  }



  private Map<String, Object> loadMappingFile() {
    try {
      String config = new String(Files.readAllBytes(Paths.get("secrets/attribute-mapping.json")),
          StandardCharsets.UTF_8);
      JsonObject json = new JsonObject(config);
      return json.getMap();
    } catch (IOException e) {
      LOGGER.error("failed to load file");
    }
    return new HashMap<String, Object>();
  }

  /**
   * initialize redis with keys for every resource group in catalogue service.
   * 
   * @param client
   */
  private void initialize(RedisService service) {

    Future<List<String>> availableKeysFuture = getAllKeys(service);
    Future<List<String>> availableRgFuture = catalogueService.getAllAvailableRG();

    CompositeFuture.all(List.of(availableRgFuture))
        .onSuccess(handler -> {
          List<String> availableKeys = availableKeysFuture.result();
          List<String> availableRg = availableRgFuture.result();

          Set<String> redisKeyNeededList = difference(availableRg, availableKeys);

          LOGGER.info("following keys required in redis : " + redisKeyNeededList);

          if (redisKeyNeededList.size() > 0) {
            for (String s : redisKeyNeededList) {
              JsonObject origin = new JsonObject();
              JsonObject pathJson = new JsonObject();
              pathJson.put("_init_d", new JsonObject());
              origin.put(s, pathJson);
              service.put(s, "", origin.toString(), createhandler -> {
                if (createhandler.succeeded()) {
                  LOGGER.info("key :" + s + " created");
                } else {
                  LOGGER.error("failed to create key : " + s);
                }
              });
            }
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

  private Future<List<String>> getAllKeys(RedisService service) {
    Promise<List<String>> promise = Promise.promise();
    service.getAllkeys(handler -> {
      if (handler.succeeded()) {
        promise.complete(handler.result());
      } else {
        promise.fail("failed to retreive all keys");
      }
    });

    return promise.future();
  }
}
