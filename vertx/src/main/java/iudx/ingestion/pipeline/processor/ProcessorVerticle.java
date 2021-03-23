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
import io.vertx.core.json.JsonObject;

public class ProcessorVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(ProcessorVerticle.class);
  private IProcessor processor;

  @Override
  public void start() throws Exception {

    Map<String,Object> mappings=loadMappingFile();
    LOGGER.debug("mapings file :"+mappings);
    processor = new MessageProcessor(vertx,mappings);
    
    vertx.eventBus().consumer(EB_RECEIVED_MSG_ADDRESS, handler -> {
      JsonObject received = new JsonObject(handler.body().toString());
      processor.process(received);
    });
  }
  
  
  
  private Map<String,Object> loadMappingFile(){
   try {
    String config = new String(Files.readAllBytes(Paths.get("configs/attribute-mapping.json")), StandardCharsets.UTF_8);
    JsonObject json=new JsonObject(config);
    return json.getMap();
  } catch (IOException e) {
    LOGGER.error("failed to load file");
  }
   return new HashMap<String,Object>();
  }
}
