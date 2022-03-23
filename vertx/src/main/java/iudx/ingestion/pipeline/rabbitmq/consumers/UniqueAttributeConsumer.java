package iudx.ingestion.pipeline.rabbitmq.consumers;


import static iudx.ingestion.pipeline.common.Constants.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.QueueOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQConsumer;
import io.vertx.rabbitmq.RabbitMQOptions;
import iudx.ingestion.pipeline.cache.CacheService;
import iudx.ingestion.pipeline.cache.cacheImpl.CacheType;
import iudx.ingestion.pipeline.common.IConsumer;

public class UniqueAttributeConsumer implements IConsumer {

  private static final Logger LOGGER = LogManager.getLogger(UniqueAttributeConsumer.class);

  private final RabbitMQClient client;
  private final CacheService cache;
  private final Vertx vertx;

  private final QueueOptions options =
      new QueueOptions()
          .setMaxInternalQueueSize(5000)
          .setKeepMostRecent(true);


  public UniqueAttributeConsumer(Vertx vertx, RabbitMQOptions options, CacheService cacheService) {
    this.vertx = vertx;
    this.client = RabbitMQClient.create(vertx, options);
    this.cache = cacheService;
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
              .basicConsumer(UNIQUE_ATTR_Q, options)
              .onSuccess(receivedMessageHandler -> {
                RabbitMQConsumer mqConsumer = receivedMessageHandler;
                mqConsumer.handler(message -> {
                  Buffer body = message.body();
                  if (body != null) {
                    JsonObject uniqueAttribJson = new JsonObject(body);
                    LOGGER.debug("received message from unique-attrib Q :" + uniqueAttribJson);
                    String key = uniqueAttribJson.getString("id");
                    String value = uniqueAttribJson.getString("unique-attribute");
                    String eventType = uniqueAttribJson.getString("eventType");
                    LOGGER.debug(eventType);
                    JsonObject cacheJson = new JsonObject();
                    cacheJson.put("type", CacheType.UNIQUE_ATTRIBUTES);

                    if (eventType == null) {
                      LOGGER.error("Invalid BroadcastEventType [ null ] ");
                      return;
                    }

                    if (eventType.equalsIgnoreCase("CREATE")) {
                      cacheJson.put("key", key);
                      cacheJson.put("value", value);
                    }

                    cache.refresh(cacheJson, cacheHandler -> {
                      if (cacheHandler.succeeded()) {
                        LOGGER.debug("unique attrib message published to Cache Verticle");
                      } else {
                        LOGGER.debug("unique attrib message published to Cache Verticle fail");
                      }
                    });
                  } else {
                    LOGGER.error("Empty json received from unique_attribute queue");
                  }
                });
              })
              .onFailure(receivedMsgFailureHandler -> {
                LOGGER.error("error while consuming processed messages");
              });
        })
        .onFailure(failureHandler -> {
          LOGGER.fatal("Rabbit client startup failed for unique attributes Q consumer.");
        });
  }
}
