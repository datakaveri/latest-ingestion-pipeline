package iudx.ingestion.pipeline.redis;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class RedisServiceImpl implements RedisService {

  private static final Logger LOGGER = LogManager.getLogger(RedisServiceImpl.class);
  private final RedisClient redisClient;

  public RedisServiceImpl(RedisClient redisClient) {
    this.redisClient = redisClient;
  }


  @Override
  public RedisService get(String key, String path, Handler<AsyncResult<JsonObject>> handler) {
    redisClient.get(key, path).onSuccess(successHandler -> {
      handler.handle(Future.succeededFuture(successHandler));
    }).onFailure(failureHandler -> {
      handler.handle(Future.failedFuture(failureHandler));
    });
    return this;
  }

  @Override
  public RedisService put(String key, String path, String data, Handler<AsyncResult<JsonObject>> handler) {
    if (data != null) {
      StringBuilder pathParam = new StringBuilder();
      pathParam
          .append(".")
          .append(path);

      LOGGER.debug("path param : {}" , pathParam);

      JsonObject response = new JsonObject().put("result", "published");
      LOGGER.debug("pushing to redis");
      redisClient.put(key, pathParam.toString(), data).onComplete(res -> {
        if (res.failed()) {
          LOGGER.error("Failed to publish latest value for key : {} to redis",key);
          handler.handle(Future.failedFuture("failed to publish to redis"));
        } else {
          handler.handle(Future.succeededFuture(response));
        }
      });
    } else {
      handler.handle(Future.failedFuture("null/empty message rejected."));
    }
    return this;
  }


  @Override
  public RedisService getAllkeys(Handler<AsyncResult<Set<String>>> handler) {
    redisClient.getAllKeys().onComplete(res -> {
      if (res.succeeded()) {
        handler.handle(Future.succeededFuture(res.result()));
      } else {
        LOGGER.error(res.cause());
      }
    });
    return this;
  }

}
