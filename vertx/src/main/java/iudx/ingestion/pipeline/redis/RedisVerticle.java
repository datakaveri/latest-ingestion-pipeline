package iudx.ingestion.pipeline.redis;

import static iudx.ingestion.pipeline.common.Constants.REDIS_SERVICE_ADDRESS;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;

public class RedisVerticle extends AbstractVerticle {

  private RedisService redisService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;


  @Override
  public void start() throws Exception {
    RedisClient client = new RedisClient(vertx, config());

    redisService = new RedisServiceImpl(client);
    binder = new ServiceBinder(vertx);

    consumer = binder
        .setAddress(REDIS_SERVICE_ADDRESS)
        .register(RedisService.class, redisService);


  }

}
