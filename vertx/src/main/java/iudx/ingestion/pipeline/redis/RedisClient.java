package iudx.ingestion.pipeline.redis;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
import io.vertx.redis.client.RedisReplicas;

public class RedisClient {

  private static final Logger LOGGER = LogManager.getLogger(RedisClient.class);
  private static final Cache<String, String> redisKeyCache =
      CacheBuilder.newBuilder().maximumSize(5000).build();
  private final Vertx vertx;
  private final JsonObject config;
  private Redis clusteredClient;
  private RedisAPI redis;
  private String tenantPrefix;

  public RedisClient(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
  }

  public Future<RedisClient> start() {
    Promise<RedisClient> promise = Promise.promise();
    RedisOptions options = null;

    String redisUri = "redis://"
        + URLEncoder.encode(config.getString("redisUsername"), StandardCharsets.UTF_8)
        + ":"
        + URLEncoder.encode(config.getString("redisPassword"), StandardCharsets.UTF_8)
        + "@"
        + config.getString("redisHost")
        + ":"
        + config.getInteger("redisPort").toString();
    String mode = config.getString("redisMode");
    this.tenantPrefix = config.getString("tenantPrefix");
    if (mode.equals("CLUSTER")) {
      options =
          new RedisOptions().setType(RedisClientType.CLUSTER).setUseReplicas(RedisReplicas.SHARE);
    } else if (mode.equals("STANDALONE")) {
      options = new RedisOptions().setType(RedisClientType.STANDALONE);
    } else {
      LOGGER.error("Invalid/Unsupported mode");
      promise.fail("Invalid/Unsupported mode");
    }
    options
        .setMaxWaitingHandlers(config.getInteger("redisMaxWaitingHandlers"))
        .setConnectionString(redisUri);

    clusteredClient = Redis.createClient(vertx, options);
    clusteredClient.connect(
        conn -> {
          if (conn.succeeded()) {
            redis = RedisAPI.api(conn.result());
            this.initKeysCache();
            promise.complete(this);
          } else {
            LOGGER.fatal("fail to connect to redis server :" + conn);
            promise.fail(conn.cause());
          }
        });
    return promise.future();
  }

  public Future<JsonObject> get(String idKey) {
    /*
     * example: key =
     * iudx:iisc_ac_in_89a36273d77dac4cf38114fca1bbe64392547f86_rs_iudx_io_pune_env_flood_FWR055
     * where "iudx" redis namespace and key is the other part +
     */

    if (!this.tenantPrefix.equals("none")) {
      String namespace = this.tenantPrefix.concat(":");
      idKey = namespace.concat(idKey);
    }
    final String key = idKey;
    return get(key, ".");
  }

  public Future<JsonObject> get(String idKey, String path) {

    if (!this.tenantPrefix.equals("none")) {
      String namespace = this.tenantPrefix.concat(":");
      idKey = namespace.concat(idKey);
    }
    final String key = idKey;
    Promise<JsonObject> promise = Promise.promise();
    redis
        .send(Command.JSON_GET, key, path)
        .onFailure(
            res -> {
              promise.fail(String.format("JSONGET did not work: %s", res.getMessage()));
            })
        .onSuccess(
            redisResponse -> {
              if (redisResponse == null) {
                promise.fail(String.format(" %s key not found", key));
              } else {
                promise.complete(new JsonObject(redisResponse.toString()));
              }
            });

    return promise.future();
  }

  public Future<Boolean> put(String idKey, String path, String data) {

    if (!this.tenantPrefix.equals("none")) {
      String namespace = this.tenantPrefix.concat(":");
      idKey = namespace.concat(idKey);
    }
    final String key = idKey;
    Promise<Boolean> promise = Promise.promise();
    String keyInRedis = redisKeyCache.getIfPresent(key);
    LOGGER.debug("key in redis : {}", keyInRedis);
    if (keyInRedis != null) {
      redis
          .send(Command.JSON_SET, key, path, data)
          .onFailure(
              res -> {
                LOGGER.error(String.format("JSONSET did not work: %s", res.getMessage()));
                promise.fail(String.format("JSONSET did not work: %s", res.getMessage()));
              })
          .onSuccess(
              redisResponse -> {
                LOGGER.info("Message pushed to Redis. response type {}" + redisResponse);
                promise.complete(true);
              });
    } else {
      JsonObject origin = new JsonObject();
      JsonObject pathJson = new JsonObject();
      pathJson.put("_init_d", new JsonObject());
      origin.put(key, pathJson);
      redis
          .send(Command.JSON_SET, key, ".", origin.toString())
          .compose(
              keyCreatedHandler -> {
                redisKeyCache.put(key, "TRUE");
                return redis.send(Command.JSON_SET, key, path, data);
              })
          .onSuccess(
              handler -> {
                LOGGER.info("Message pushed to Redis. response type {}" + handler);
                promise.complete(true);
              })
          .onFailure(
              handler -> {
                LOGGER.error(
                    "fail to push message to Redis [either key not present & fail to create key]");
                promise.fail("fail to push message");
              });
    }
    return promise.future();
  }

  public void close() {
    redis.close();
  }

  public Future<Set<String>> getAllKeys() {
    String keys = "*";
    if (!this.tenantPrefix.equals("none")) {
      String namespace = this.tenantPrefix.concat(":");
      keys = namespace.concat("*");
    }
    Promise<Set<String>> promise = Promise.promise();
    redis.keys(
        keys,
        handler -> {
          if (handler.succeeded()) {
            List<String> list =
                Arrays.asList(
                    handler
                        .result()
                        .toString()
                        .replaceAll("\\[", "")
                        .replaceAll("\\]", "")
                        .split(","));
            promise.complete(list.stream().map(e -> e.trim()).collect(Collectors.toSet()));
          } else {
            LOGGER.error("failed to get Keys from Redis");
            promise.fail("failed to get keys " + handler.cause());
          }
        });
    return promise.future();
  }

  private void initKeysCache() {
    getAllKeys()
        .onSuccess(
            handler -> {
              Set<String> keys = handler;
              keys.forEach(
                  key -> {
                    redisKeyCache.put(key, "TRUE");
                  });
            })
        .onFailure(
            handler -> {
              LOGGER.error("Failed to get all Keys from Redis");
            });
  }
}
