package iudx.ingestion.pipeline.processor;

import static iudx.ingestion.pipeline.common.Constants.DEFAULT_SUFFIX;
import static iudx.ingestion.pipeline.common.Constants.EB_PUBLISH_2_REDIS;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.annotate.JsonProperty;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

@Deprecated
class MessageProcessor implements IProcessor {

  private static final Logger LOGGER = LogManager.getLogger(MessageProcessor.class);
  private final Vertx vertx;
  private final Map<String, Object> mappings;
  private final String defaultAttribValue = DEFAULT_SUFFIX;

  public MessageProcessor(Vertx vertx, Map<String, Object> mappings) {
    this.vertx = vertx;
    this.mappings = mappings;
  }

  @Override
  public void process(JsonObject json) {
    this.processMessage(json);
  }


  private void processMessage(JsonObject json) {
    if (json == null || json.isEmpty()) {
      LOGGER.debug("empty/null message received");
      return;
    }
    JsonObject processedJson = JsonObject.mapFrom(getProcessedMessage(json));
    //vertx.eventBus().publish(EB_PROCESSED_MSG_ADDRESS, processedJson.toString());
    vertx.eventBus().publish(EB_PUBLISH_2_REDIS,processedJson.toString());
  }


  private ProcessedMessage getProcessedMessage(JsonObject json) {
    String id = json.getString("id");
    String idSHA = DigestUtils.shaHex(id);
    String[] idComponents = id.split("/");
    String groupId = idComponents[3];
    String pathParamAttribute = (String) mappings.get(groupId);
    StringBuilder pathParam = new StringBuilder();
    if (pathParamAttribute == null || pathParamAttribute.isBlank()) {
      pathParam.append("_").append(idSHA).append(defaultAttribValue);
    } else {
      String value = (String) json.getString(pathParamAttribute);
      pathParam.append("_").append(idSHA).append("_").append(value);
    }
    ProcessedMessage message = new ProcessedMessage(groupId.replaceAll("-","_"), pathParam.toString(), json);
    return message;
  }


  private class ProcessedMessage {
    @JsonProperty("key")
    private String key;
    @JsonProperty("path_param")
    private String pathParam;
    @JsonProperty("data")
    private JsonObject data;

    ProcessedMessage(String key, String pathParam, JsonObject data) {
      this.key = key;
      this.pathParam = pathParam;
      this.data = data;
    }
  }
}


