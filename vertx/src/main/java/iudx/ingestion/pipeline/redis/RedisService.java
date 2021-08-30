package iudx.ingestion.pipeline.redis;

import java.util.List;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@VertxGen
@ProxyGen
public interface RedisService {

  @Fluent
  RedisService get(final String key, final String path, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  RedisService put(final String key, final String path, final String data, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  RedisService getAllkeys(Handler<AsyncResult<List<String>>> handler);

  @GenIgnore
  static RedisService createProxy(Vertx vertx, String address) {
    return new RedisServiceVertxEBProxy(vertx, address);
  }
}
