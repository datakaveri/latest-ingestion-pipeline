package iudx.ingestion.pipeline.processor;

import static iudx.ingestion.pipeline.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ingestion.pipeline.cache.CacheService;
import iudx.ingestion.pipeline.rabbitmq.RabbitMqService;

public class ProcessorVerticle extends AbstractVerticle {

  private MessageProcessService processor;
  private RabbitMqService rabbitMqService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  private CacheService cache;

  @Override
  public void start() throws Exception {

    rabbitMqService = RabbitMqService.createProxy(vertx, RMQ_SERVICE_ADDRESS);
    cache = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);

    processor = new MessageProcessorImpl(cache, rabbitMqService);

    binder = new ServiceBinder(vertx);

    consumer =
        binder.setAddress(MSG_PROCESS_ADDRESS).register(MessageProcessService.class, processor);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
