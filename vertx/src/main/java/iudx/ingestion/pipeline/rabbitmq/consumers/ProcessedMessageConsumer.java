package iudx.ingestion.pipeline.rabbitmq.consumers;

import static iudx.ingestion.pipeline.common.Constants.RMQ_PROCESSED_MSG_Q;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.ingestion.pipeline.common.IConsumer;
import iudx.ingestion.pipeline.redis.RedisService;

public class ProcessedMessageConsumer implements IConsumer {

  private static final Logger LOGGER = LogManager.getLogger(ProcessedMessageConsumer.class);

  private final RabbitMQClient client;
  private final RedisService redisService;
  private final Vertx vertx;

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(1000)
          .setKeepMostRecent(true);

  public ProcessedMessageConsumer(Vertx vertx, RabbitMQOptions options, RedisService redisService) {
    this.vertx = vertx;
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
        .onSuccess(successHandler -> {
          client
              .basicConsumer(RMQ_PROCESSED_MSG_Q, options)
              .onSuccess(receivedResultHandler -> {
                RabbitMQConsumer mqConsumer = receivedResultHandler;
                mqConsumer.handler(message -> {
                  Buffer body = message.body();
                  if (body != null) {
                    JsonObject fromRMQ = new JsonObject(body);
                    String key = fromRMQ.getString("key");
                    String path = fromRMQ.getString("path_param");
                    String data = fromRMQ.getJsonObject("data").toString();
                    redisService.put(key, path, data, handler -> {
                      if (handler.succeeded()) {
                        LOGGER.debug("Processed message pushed to redis");
                      } else {
                        LOGGER.error("redis push failed, " + handler.cause().getMessage());
                      }
                    });
                  }
                });
              })
              .onFailure(receivedMsgFailureHandler -> {
                LOGGER.error("error while consuming processed messages");
              });
        })
        .onFailure(failureHandler -> {
          LOGGER.fatal("Rabbit client startup failed for Processed message Q consumer.");
        });
  }

}
