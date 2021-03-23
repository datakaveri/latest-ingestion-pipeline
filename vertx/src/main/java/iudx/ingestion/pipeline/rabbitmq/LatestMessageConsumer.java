package iudx.ingestion.pipeline.rabbitmq;

import static iudx.ingestion.pipeline.common.Constants.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import iudx.ingestion.pipeline.common.IConsumer;

class LatestMessageConsumer implements IConsumer {

  private final RabbitMQClient client;
  private final Vertx vertx;
  
  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  public LatestMessageConsumer(Vertx vertx,RabbitMQClient client) {
    this.vertx=vertx;
    this.client = client;
  }

  @Override
  public void start() {
    this.consume();
  }
  
  
  private void consume() {
    client.basicConsumer(RMQ_LATEST_DATA_Q, options, receivedResultHandler -> {
      if (receivedResultHandler.succeeded()) {
        RabbitMQConsumer mqConsumer = receivedResultHandler.result();
        mqConsumer.handler(message -> {
          Buffer body = message.body();
          if (body != null) {
            vertx.eventBus().publish(EB_RECEIVED_MSG_ADDRESS, body.toString());
          }
        });
      }
    });
  }

}
