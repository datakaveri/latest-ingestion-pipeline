package iudx.ingestion.pipeline.processor;

import static iudx.ingestion.pipeline.common.Constants.MSG_PROCESS_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.REDIS_SERVICE_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.RMQ_SERVICE_ADDRESS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ingestion.pipeline.common.service.CatalogueService;
import iudx.ingestion.pipeline.common.service.impl.CatalogueServiceImpl;
import iudx.ingestion.pipeline.rabbitmq.RabbitMQService;
import iudx.ingestion.pipeline.redis.RedisService;

public class ProcessorVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ProcessorVerticle.class);
  private MessageProcessService processor;
  private RabbitMQService rabbitMQService;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;



  @Override
  public void start() throws Exception {

    Map<String, Object> mappings = loadMappingFile();
    LOGGER.debug("mapings file :" + mappings);


    rabbitMQService = RabbitMQService.createProxy(vertx, RMQ_SERVICE_ADDRESS);
    processor = new MessageProcessorImpl(vertx, mappings, rabbitMQService);

    binder = new ServiceBinder(vertx);

    consumer = binder
        .setAddress(MSG_PROCESS_ADDRESS)
        .register(MessageProcessService.class, processor);

  }


  private Map<String, Object> loadMappingFile() {
    try {
      String config = new String(Files.readAllBytes(Paths.get("secrets/attribute-mapping.json")),
          StandardCharsets.UTF_8);
      JsonObject json = new JsonObject(config);
      return json.getMap();
    } catch (IOException e) {
      LOGGER.error("failed to load file");
    }
    return new HashMap<String, Object>();
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
