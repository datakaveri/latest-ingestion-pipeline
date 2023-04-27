package iudx.ingestion.pipeline.postgres;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PostgresServiceImpl implements PostgresService {

  private static final Logger LOGGER = LogManager.getLogger(PostgresServiceImpl.class);

  private final PgPool client;

  public PostgresServiceImpl(final PgPool pgclient) {
    this.client = pgclient;
  }

  @Override
  public PostgresService executeQuery(
      final String query, Handler<AsyncResult<JsonObject>> handler) {

    Collector<Row, ?, List<JsonObject>> rowCollector =
        Collectors.mapping(row -> row.toJson(), Collectors.toList());

    client
        .withConnection(
            connection ->
                connection.query(query).collecting(rowCollector).execute().map(row -> row.value()))
        .onSuccess(
            successHandler -> {
              JsonArray response = new JsonArray(successHandler);
              handler.handle(Future.succeededFuture(new JsonObject().put("result", response)));
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(failureHandler);
              JsonObject response = new JsonObject();
              response.put("error", "DB Error.");
              handler.handle(Future.failedFuture(response.toString()));
            });
    return this;
  }
}
