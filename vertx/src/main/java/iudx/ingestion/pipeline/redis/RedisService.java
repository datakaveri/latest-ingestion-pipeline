package iudx.ingestion.pipeline.redis;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Set;

@VertxGen
@ProxyGen
public interface RedisService {

  @GenIgnore
  static RedisService createProxy(Vertx vertx, String address) {
    return new RedisServiceVertxEBProxy(vertx, address);
  }

  @Fluent
  RedisService get(final String key, final String path, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  RedisService put(
      final String key,
      final String path,
      final String data,
      Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  RedisService getAllkeys(Handler<AsyncResult<Set<String>>> handler);
}
