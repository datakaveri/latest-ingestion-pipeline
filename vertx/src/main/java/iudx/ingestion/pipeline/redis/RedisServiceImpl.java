package iudx.ingestion.pipeline.redis;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

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

  // TODO :
  // 1.) Ensure a key entry is there for all resource groups available in catalogue (through init
  // method @startup)
  // 2.) Design a system to periodically matches key from Redis to Resource Groups from catalogue
  // service to
  // discover any new key available which needs to be created in Redis,
  // CHALLENGES :
  // - where this cron service should reside in muti verticle system since only one call is needed to
  // this cron service.
  @Override
  public RedisService put(String key, String path, String data, Handler<AsyncResult<JsonObject>> handler) {

    if (data != null) {
      StringBuilder pathParam = new StringBuilder();
      pathParam.append(".")
          .append(key)
          .append(".")
          .append(path);

      LOGGER.info("path param : " + pathParam);

      JsonObject response = new JsonObject().put("result", "published");

            redisClient.put(key, pathParam.toString(), data).onComplete(res -> {
              if (res.failed()) {
                LOGGER.error(res.cause());
              } else {
                handler.handle(Future.succeededFuture(response));
              }
            });

    } else {
      handler.handle(Future.failedFuture("null/empty message rejected."));
    }
    return this;
  }


}
