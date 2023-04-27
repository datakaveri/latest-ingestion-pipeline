package iudx.ingestion.pipeline.rabbitmq.consumers;

import static iudx.ingestion.pipeline.common.Constants.*;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.ingestion.pipeline.common.ConsumerAction;
import iudx.ingestion.pipeline.redis.RedisService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessedMessageConsumerAction implements ConsumerAction {

  private static final Logger LOGGER = LogManager.getLogger(ProcessedMessageConsumerAction.class);

  private final RabbitMQClient client;
  private final RedisService redisService;
  private final QueueOptions options =
      new QueueOptions().setMaxInternalQueueSize(1000).setKeepMostRecent(true);

  public ProcessedMessageConsumerAction(
      Vertx vertx, RabbitMQOptions options, RedisService redisService) {
    this.client = RabbitMQClient.create(vertx, options);
    this.redisService = redisService;
  }

  @Override
  public void start() {
    this.consume();
  }

  private void consume() {
    client
        .start()
        .onSuccess(
            successHandler -> {
              client
                  .basicConsumer(RMQ_PROCESSED_MSG_Q, options)
                  .onSuccess(
                      receivedResultHandler -> {
                        RabbitMQConsumer mqConsumer = receivedResultHandler;
                        mqConsumer.handler(
                            message -> {
                              Buffer body = message.body();
                              if (body != null) {
                                JsonObject fromRabbitMq = new JsonObject(body);
                                String key = fromRabbitMq.getString("key");
                                String path = fromRabbitMq.getString("path_param");
                                String data = fromRabbitMq.getJsonObject("data").toString();
                                LOGGER.info("processed message received");
                                redisService.put(
                                    key,
                                    path,
                                    data,
                                    handler -> {
                                      if (handler.succeeded()) {
                                        LOGGER.debug("Processed message pushed to redis");
                                      } else {
                                        LOGGER.error(
                                            "redis push failed, " + handler.cause().getMessage());
                                      }
                                    });
                              }
                            });
                      })
                  .onFailure(
                      receivedMsgFailureHandler -> {
                        LOGGER.error("error while consuming processed messages");
                      });
            })
        .onFailure(
            failureHandler -> {
              LOGGER.fatal("Rabbit client startup failed for Processed message Q consumer.");
            });
  }
}
