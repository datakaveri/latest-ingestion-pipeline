package iudx.ingestion.pipeline.rabbitmq.consumers;

import static iudx.ingestion.pipeline.common.Constants.RMQ_LATEST_DATA_Q;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.ingestion.pipeline.common.IConsumer;
import iudx.ingestion.pipeline.processor.MessageProcessService;

public class LatestMessageConsumer implements IConsumer {

  private static final Logger LOGGER = LogManager.getLogger(LatestMessageConsumer.class);

  private final RabbitMQClient client;
  private final MessageProcessService msgService;
  private final Vertx vertx;

  private final QueueOptions options =
      new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);

  public LatestMessageConsumer(Vertx vertx, RabbitMQOptions options,
      MessageProcessService msgService) {
    this.vertx = vertx;
    this.client = RabbitMQClient.create(vertx, options);
    this.msgService = msgService;
  }

  @Override
  public void start() {
    this.consume();
  }


  private void consume() {
    client.start().onSuccess(successHandler -> {
      client.basicConsumer(RMQ_LATEST_DATA_Q, options).onSuccess(receiveResultHandler -> {

        RabbitMQConsumer mqConsumer = receiveResultHandler;
        mqConsumer.handler(message -> {
          Buffer body = message.body();
          if (body != null) {
            LOGGER.info("latest message received");
            boolean isArrayReceived = isJsonArray(body);
            LOGGER.debug("is message array received : {}", isArrayReceived);
            if (isArrayReceived) {
              JsonArray jsonArrayBody = body.toJsonArray();
              jsonArrayBody.forEach(json -> {
                Future.future(e -> messagePush((JsonObject) json));
              });
            } else {
              Future.future(e -> messagePush(new JsonObject(body)));
            }
          }
        });
      }).onFailure(receivedMsgFailureHandler -> {
        LOGGER.error("error while consuming latest messages");
      });
    }).onFailure(failureHandler -> {
      LOGGER.fatal("Rabbit client startup failed for Latest message Q consumer.");
    });
  }

  public Future<Void> messagePush(JsonObject json) {
    Promise<Void> promise = Promise.promise();
    msgService.process(json, handler -> {
      if (handler.succeeded()) {
        LOGGER.debug("Latest message published for processing");
        promise.complete();
      } else {
        LOGGER.error("Error while publishing message for processing");
        promise.fail("Failed to send mesasge to processer service");
      }
    });
    return promise.future();
  }


  public boolean isJsonArray(Buffer jsonObjectBuffer) {
    Object value;
    try {
      value = jsonObjectBuffer.toJson();
    } catch (DecodeException e) {
      throw new RuntimeException(e);
    }
    LOGGER.debug("isArray : {}", (value instanceof JsonArray));
    return (value instanceof JsonArray) ? true : false;
  }

}
