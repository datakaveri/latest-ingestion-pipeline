package iudx.ingestion.pipeline.processor;

import static iudx.ingestion.pipeline.common.Constants.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ingestion.pipeline.cache.CacheService;
import iudx.ingestion.pipeline.rabbitmq.RabbitMQService;

public class ProcessorVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ProcessorVerticle.class);
  private MessageProcessService processor;
  private RabbitMQService rabbitMQService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  private CacheService cache;


  @Override
  public void start() throws Exception {


    rabbitMQService = RabbitMQService.createProxy(vertx, RMQ_SERVICE_ADDRESS);
    cache=CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);
    
    processor = new MessageProcessorImpl(vertx, cache, rabbitMQService);
    
    binder = new ServiceBinder(vertx);

    consumer = binder
        .setAddress(MSG_PROCESS_ADDRESS)
        .register(MessageProcessService.class, processor);

  }


//  private Map<String, Object> loadMappingFile() {
//    try {
//      String config = new String(Files.readAllBytes(Paths.get("secrets/attribute-mapping.json")),
//          StandardCharsets.UTF_8);
//      JsonObject json = new JsonObject(config);
//      return json.getMap();
//    } catch (IOException e) {
//      LOGGER.error("failed to load file");
//    }
//    return new HashMap<String, Object>();
//  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
