package iudx.ingestion.pipeline.redis;

import io.vertx.core.AbstractVerticle;
import iudx.ingestion.pipeline.common.IProducer;

public class RedisVerticle extends AbstractVerticle {


  private IProducer redisProducer;


  @Override
  public void start() throws Exception {
    RedisClient client =
        new RedisClient(vertx, config().getString("redisIp"), config().getInteger("redisPort"));
    /*
     * client.get("varanasi_env_aqm").onComplete(handler -> { if (handler.succeeded()) {
     * System.out.println("found"); } else { System.out.println("not found"); } });
     */
    redisProducer=new RedisProducer(vertx, client);
    redisProducer.start();
  }

}
