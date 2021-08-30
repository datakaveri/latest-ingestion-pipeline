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

  @Override
  public void start() throws Exception {
    client = new RedisClient(vertx, config());

    redisService = new RedisServiceImpl(client);
    binder = new ServiceBinder(vertx);

    consumer = binder
        .setAddress(REDIS_SERVICE_ADDRESS)
        .register(RedisService.class, redisService);

  }


  public void stop() {
    if (client != null) {
      client.close();
    }
  }

}
