package iudx.ingestion.pipeline.rabbitmq;

import static iudx.ingestion.pipeline.common.Constants.EB_PROCESSED_MSG_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.RMQ_PROCESSED_MSG_EX;
import static iudx.ingestion.pipeline.common.Constants.RMQ_PROCESSED_MSG_EX_ROUTING_KEY;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.rabbitmq.RabbitMQClient;
import iudx.ingestion.pipeline.common.IProducer;

class Producer implements IProducer {

  private final RabbitMQClient client;
  private final Vertx vertx;

  public Producer(Vertx vertx, RabbitMQClient client) {
    this.vertx = vertx;
    this.client = client;
  }

  @Override
  public void start() {
    this.publish();
  }

  public void publish() {
    vertx.eventBus().consumer(EB_PROCESSED_MSG_ADDRESS, msg -> {
      JsonObject received = new JsonObject(msg.body().toString());
      JsonObject json = new JsonObject();
      json.put("body", received.toString());
      client.basicPublish(RMQ_PROCESSED_MSG_EX, RMQ_PROCESSED_MSG_EX_ROUTING_KEY, json,
          publishResultHandler -> {
            if (publishResultHandler.succeeded()) {
            } else {
            }
          });
    });
  }

}
