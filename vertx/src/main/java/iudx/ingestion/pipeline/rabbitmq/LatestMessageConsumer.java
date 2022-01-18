package iudx.ingestion.pipeline.rabbitmq;

import static iudx.ingestion.pipeline.common.Constants.RMQ_LATEST_DATA_Q;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import iudx.ingestion.pipeline.common.IConsumer;
import iudx.ingestion.pipeline.processor.MessageProcessService;

class LatestMessageConsumer implements IConsumer {

  private static final Logger LOGGER = LogManager.getLogger(LatestMessageConsumer.class);

  private final RabbitMQClient client;
  private final MessageProcessService msgService;
  private final Vertx vertx;

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  public LatestMessageConsumer(Vertx vertx, RabbitMQClient client, MessageProcessService msgService) {
    this.vertx = vertx;
    this.client = client;
    this.msgService = msgService;
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
            msgService.process(new JsonObject(body), handler -> {
              if (handler.succeeded()) {
                LOGGER.debug("Messaged processed and published");
              } else {
                LOGGER.error("Error while processing message and publishing");
              }
            });
          }
        });
      } else {
        LOGGER.error("error while consuming latest messages");
      }
    });
  }

}
