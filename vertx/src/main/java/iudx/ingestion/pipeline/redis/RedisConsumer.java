package iudx.ingestion.pipeline.redis;

import io.vertx.core.Vertx;
import iudx.ingestion.pipeline.common.IConsumer;

public class RedisConsumer implements IConsumer{
  
  //private final RedisConnection redisConnection;
  private final Vertx vertx;
  
  public RedisConsumer(Vertx vertx) {
    //this.redisConnection=redisConnection;
    this.vertx=vertx;
  }

  @Override
  public void start() {
  }
 

}
