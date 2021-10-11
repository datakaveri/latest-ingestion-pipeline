package iudx.ingestion.pipeline.redis;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

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
  private final Vertx vertx;
  private final JsonObject config;
  private static final Command JSONGET = Command.create("JSON.GET", -1, 1, 1, 1, true, false);
  private static final Command JSONSET = Command.create("JSON.SET", -1, 1, 1, 1, false, false);

  private static final Logger LOGGER = LogManager.getLogger(RedisClient.class);

  public RedisClient(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
  }


  public Future<RedisClient> start() {
    Promise<RedisClient> promise = Promise.promise();
    StringBuilder RedisURI = new StringBuilder();
    RedisOptions options = null;
    RedisURI.append("redis://").append(config.getString("redisUsername")).append(":")
        .append(config.getString("redisPassword")).append("@").append(config.getString("redisHost"))
        .append(":").append(config.getInteger("redisPort").toString());
    String mode = config.getString("redisMode");
    if (mode.equals("CLUSTER")) {
      options = new RedisOptions().setType(RedisClientType.CLUSTER).setUseSlave(RedisSlaves.NEVER);
    } else if (mode.equals("STANDALONE")) {
      options = new RedisOptions().setType(RedisClientType.STANDALONE);
    } else {
      LOGGER.error("Invalid/Unsupported mode");
      promise.fail("Invalid/Unsupported mode");
    }
    options.setMaxWaitingHandlers(config.getInteger("redisMaxWaitingHandlers"))
        .setConnectionString(RedisURI.toString());

    ClusteredClient = Redis.createClient(vertx, options);
    ClusteredClient.connect(conn -> {
      redis = RedisAPI.api(conn.result());
      promise.complete(this);
    });
    return promise.future();
  }

  public Future<JsonObject> get(String key) {
    return get(key, ".".toString());
  }


  public Future<JsonObject> get(String key, String path) {
    Promise<JsonObject> promise = Promise.promise();
    redis.send(JSONGET, key, path).onFailure(res -> {
      promise.fail(String.format("JSONGET did not work: %s", res.getMessage()));
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
    redis.send(JSONSET, key, path, data)
        .onFailure(res -> {
          LOGGER.error(String.format("JSONSET did not work: %s", res.getMessage()));
          promise.fail(String.format("JSONSET did not work: %s", res.getMessage()));
        }).onSuccess(redisResponse -> {
          promise.complete();
        });

    return promise.future();
  }

  public void close() {
    redis.close();
  }



  public Future<Set<String>> getAllKeys() {
    Promise<Set<String>> promise = Promise.promise();
    LOGGER.debug("getting all keys" + redis);
    redis.keys("*", handler -> {
      if (handler.succeeded()) {
        LOGGER.debug("handler : " + handler.toString());
        List<String> list =
            Arrays.asList(handler.result().toString().replaceAll("\\[", "").replaceAll("\\]", "").split(","));

        promise.complete(list.stream().map(e -> e.trim()).collect(Collectors.toSet()));
      } else {
        LOGGER.error("faile to get Keys from Redis");
        promise.fail("failed to get keys " + handler.cause());
      }
    });
    return promise.future();
  }
}
