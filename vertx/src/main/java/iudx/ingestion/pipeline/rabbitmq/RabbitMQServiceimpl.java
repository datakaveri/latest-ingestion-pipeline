package iudx.ingestion.pipeline.rabbitmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.ingestion.pipeline.common.VHosts;

public class RabbitMQServiceimpl implements RabbitMQService {

  private static final Logger LOGGER = LogManager.getLogger(RabbitMQServiceimpl.class);
  private final RabbitMQClient client;

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  public RabbitMQServiceimpl(Vertx vertx, RabbitMQOptions options) {
    this.client = RabbitMQClient.create(vertx, options);
    this.client
        .start()
        .onSuccess(handler -> {
          LOGGER.info("RMQ client started.");
        }).onFailure(handler -> {
          LOGGER.error("RMQ client startup failed");
        });

  }

  @Override
  public RabbitMQService publish(String exchange, String routingkey, JsonObject data,
      Handler<AsyncResult<JsonObject>> handler) {
    Buffer buffer = Buffer.buffer(data.toString());
    client.basicPublish(exchange, routingkey, buffer,
        publishResultHandler -> {
          if (publishResultHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
          } else {
            LOGGER.error("published failed" + publishResultHandler);
            handler.handle(Future.failedFuture("publish failed"));
          }
        });
    return this;
  }

  @Override
  public RabbitMQService consume(String queue, Handler<AsyncResult<JsonObject>> handler) {
    client.basicConsumer(queue, options, receivedResultHandler -> {
      if (receivedResultHandler.succeeded()) {
        RabbitMQConsumer mqConsumer = receivedResultHandler.result();
        mqConsumer.handler(message -> {
          Buffer body = message.body();
          if (body != null) {
            handler.handle(Future.succeededFuture(new JsonObject(body)));
          } else {
            handler.handle(Future.failedFuture("null/empty message"));
          }
        });
      }
    });

    return this;
  }

}
