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
    redisClient.get(key, path)
        .onSuccess(successHandler -> {
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

      redisClient.get(key, pathParam.toString()).onComplete(redisHandler -> {
        if (redisHandler.succeeded()) {
          // key found
          LOGGER.debug("key found : ");
          JsonObject fromRedis = redisHandler.result();
          LOGGER.debug("data : " + fromRedis.toString());
          if (isValidMessage2Push(fromRedis, new JsonObject(data))) {

            redisClient.put(key, pathParam.toString(), data).onComplete(res -> {
              if (res.failed()) {
                LOGGER.error(res.cause());
              } else {
                handler.handle(Future.succeededFuture(response));
              }
            });
          } else {
            LOGGER.info("message rejected for being older than json in redis.");
            handler.handle(Future.failedFuture("message rejected for being older than json in redis."));
          }
        } else {
          LOGGER.debug("key not found : " + key);
          redisClient.put(key, pathParam.toString(), data).onComplete(res -> {
            if (res.failed()) {
              LOGGER.error(res.cause());
              handler.handle(Future.failedFuture("failed to publish message."));
            } else {
              handler.handle(Future.succeededFuture(response));
            }
          });
        }
      });
    } else {
      handler.handle(Future.failedFuture("null/empty message rejected."));
    }
    return this;
  }


  /**
   * check if message is valid through 'ObservationDateTime' field's in both messages.
   * 
   * @param fromRedis Json from Redis Cache.
   * @param latestJson Json from EB.
   * @return
   */
  private boolean isValidMessage2Push(JsonObject fromRedis, JsonObject latestJson) {
    String dateFromRedisData = fromRedis.getString("observationDateTime");
    String dateFromLatestData = latestJson.getString("observationDateTime");
    boolean result = false;
    LOGGER.debug("from Redis : " + dateFromRedisData + " from Latest : " + dateFromLatestData);
    try {
      LocalDateTime fromRedisData = LocalDateTime.parse(dateFromRedisData);
      LocalDateTime fromLatestData = LocalDateTime.parse(dateFromLatestData);

      if (fromLatestData.isAfter(fromRedisData)) {
        LOGGER.info(result);
        result = true;
      }
    } catch (DateTimeParseException e) {
      LOGGER.error("parse exception : " + e.getMessage());
      result = false;
    }
    return result;
  }



}
