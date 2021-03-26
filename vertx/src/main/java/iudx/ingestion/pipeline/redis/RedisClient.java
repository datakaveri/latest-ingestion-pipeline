package iudx.ingestion.pipeline.redis;

import java.util.Map;
import com.redislabs.modules.rejson.JReJSON;
import com.redislabs.modules.rejson.Path;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class RedisClient {

  private final Vertx vertx;
  JReJSON client;


  public RedisClient(Vertx vertx, String ip, int port) {
    this.vertx = vertx;
    client = new JReJSON(ip, port);
  }


  public Future<JsonObject> get(String key) {
    return get(key, Path.ROOT_PATH.toString());
  }


  public Future<JsonObject> get(String key, String path) {
    Promise<JsonObject> promise = Promise.promise();
    vertx.executeBlocking(getFromRedisHandler -> {
      JsonObject json = getFromRedis(key, path);
      if (json == null) {
        getFromRedisHandler.fail("nil");
      } else {
        getFromRedisHandler.complete(json);
      }
    }, resultHandler -> {
      if (resultHandler.succeeded()) {
        promise.complete((JsonObject) resultHandler.result());
      } else {
        promise.fail(resultHandler.cause());
      }
    });
    return promise.future();
  }

  public Future<Boolean> put(String key, String path, Object data) {
    Promise<Boolean> promise = Promise.promise();
    vertx.executeBlocking(redisSetJsonHandler -> {
      if (put2Redis(key, path, data)) {
        redisSetJsonHandler.complete();
      } else {
        redisSetJsonHandler.fail("nil failed");
        System.out.println("failed");
      }
    }, resultHandler -> {
      if (resultHandler.succeeded()) {
        promise.complete();
      } else {
        promise.fail("not able to set json");
      }
    });
    return promise.future();
  }

  private JsonObject getFromRedis(String key, String path) {
    Map result = null;
    try {
      result = client.get(key, Map.class, new Path(path));
      if (result != null) {
        JsonObject res = new JsonObject(result);
        return res;
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  private boolean put2Redis(String key, String path, Object data) {
    try {
      client.set(key, data, new Path(path));
      return Boolean.TRUE;
    } catch (Exception ex) {
      System.out.println(ex);
      return Boolean.FALSE;
    }
  }

}
