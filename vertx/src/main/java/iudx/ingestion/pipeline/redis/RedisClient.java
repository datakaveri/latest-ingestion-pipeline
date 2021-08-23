package iudx.ingestion.pipeline.redis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisSlaves;

public class RedisClient {

  private Redis ClusteredClient;
  private RedisAPI redis;
  private static final Command JSONGET = Command.create("JSON.GET", -1, 1, 1, 1, true, false);
  private static final Command JSONSET = Command.create("JSON.SET", -1, 1, 1, 1, false, false);
  private static final Logger LOGGER = LogManager.getLogger(RedisClient.class);

  public RedisClient(Vertx vertx, JsonObject config) {
    StringBuilder RedisURI = new StringBuilder();
    RedisOptions options;
    RedisURI.append("redis://").append(config.getString("redisUsername")).append(":")
        .append(config.getString("redisPassword")).append("@").append(config.getString("redisHost"))
        .append(":")
        .append(config.getInteger("redisPort").toString());
    String mode = config.getString("redisMode");
    if (mode.equals("CLUSTER")) {
      options =
          new RedisOptions().setType(RedisClientType.CLUSTER).setUseSlave(RedisSlaves.SHARE);
    } else if (mode.equals("STANDALONE")) {
      options =
          new RedisOptions().setType(RedisClientType.STANDALONE);
    } else {
      LOGGER.error("Invalid/Unsupported mode");
      return;
    }
    options.setMaxPoolSize(config.getInteger("redisMaxPoolSize"))
        .setMaxPoolWaiting(config.getInteger("redisMaxPoolWaiting"))
        .setMaxWaitingHandlers(config.getInteger("redisMaxWaitingHandlers"))
        .setPoolRecycleTimeout(config.getInteger("redisPoolRecycleTimeout"))
        .setConnectionString(RedisURI.toString());

    ClusteredClient = Redis.createClient(vertx, options);
    redis = RedisAPI.api(ClusteredClient);

  }

  public Future<JsonObject> get(String key) {
    return get(key, ".".toString());
  }


  public Future<JsonObject> get(String key, String path) {
    Promise<JsonObject> promise = Promise.promise();
    redis.send(JSONGET, key, path).onFailure(res -> {
      promise.fail(String.format("JSONGET did not work: %s", res.getCause()));
    }).onSuccess(redisResponse -> {
      if (redisResponse == null) {
        promise.fail(String.format(" %s key not found", key));
      } else {
        promise.complete(new JsonObject(redisResponse.toString()));
      }
    });

    return promise.future();
  }

  public Future<Boolean> put(String key, String path, String data) {
    Promise<Boolean> promise = Promise.promise();
    LOGGER.debug(String.format("setting data: %s", data));
    redis.send(JSONSET, key, path, data).onFailure(res -> {
      LOGGER.error(String.format("JSONSET did not work: %s", res.getMessage()));
      promise.fail(String.format("JSONSET did not work: %s", res.getCause()));
    }).onSuccess(redisResponse -> {
      promise.complete();
    });
    return promise.future();
  }

}
