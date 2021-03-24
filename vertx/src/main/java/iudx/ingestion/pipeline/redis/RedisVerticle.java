package iudx.ingestion.pipeline.redis;

import io.vertx.core.AbstractVerticle;
import iudx.ingestion.pipeline.common.IProducer;

public class RedisVerticle extends AbstractVerticle {


  private IProducer redisProducer;


  @Override
  public void start() throws Exception {
    RedisClient client =
        new RedisClient(vertx, config().getString("redisIp"), config().getInteger("redisPort"));

    redisProducer=new RedisProducer(vertx, client);
    redisProducer.start();
  }

}
