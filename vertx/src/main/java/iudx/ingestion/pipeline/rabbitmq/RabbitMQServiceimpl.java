package iudx.ingestion.pipeline.rabbitmq;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;

public class RabbitMQServiceimpl implements RabbitMQService {

  private static final Logger LOGGER = LogManager.getLogger(RabbitMQServiceimpl.class);
  private final RabbitMQClient client;

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  public RabbitMQServiceimpl(RabbitMQClient client) {
    this.client = client;
  }

  @Override
  public RabbitMQService publish(String exchange, String routingkey, JsonObject data,
      Handler<AsyncResult<JsonObject>> handler) {
    client.basicPublish(exchange, routingkey, data,
        publishResultHandler -> {
          if (publishResultHandler.succeeded()) {
            handler.handle(Future.succeededFuture());
          } else {
            LOGGER.error("published failed");
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
