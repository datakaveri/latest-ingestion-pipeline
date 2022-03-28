package iudx.ingestion.pipeline.redis;

import static iudx.ingestion.pipeline.common.Constants.REDIS_SERVICE_ADDRESS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;

public class RedisVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(RedisVerticle.class);

  private RedisService redisService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  private RedisClient client;

  @Override
  public void start() throws Exception {
    new RedisClient(vertx, config())
        .start()
        .onSuccess(handler -> {
          client = handler;
          redisService = new RedisServiceImpl(client);

          binder = new ServiceBinder(vertx);
          consumer = binder
              .setAddress(REDIS_SERVICE_ADDRESS)
              .register(RedisService.class, redisService);

        }).onFailure(handler -> {
          LOGGER.error("failed to start redis client");
        });

    WebClientOptions options = new WebClientOptions();
    options.setTrustAll(true)
        .setVerifyHost(false)
        .setSsl(true);
  }

  @Override
  public void stop() {
    if (client != null) {
      client.close();
    }
    binder.unregister(consumer);
  }

}
