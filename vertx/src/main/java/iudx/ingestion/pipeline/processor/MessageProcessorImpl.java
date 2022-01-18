package iudx.ingestion.pipeline.processor;

import static iudx.ingestion.pipeline.common.Constants.DEFAULT_SUFFIX;
import static iudx.ingestion.pipeline.common.Constants.RMQ_PROCESSED_MSG_EX;
import static iudx.ingestion.pipeline.common.Constants.RMQ_PROCESSED_MSG_EX_ROUTING_KEY;

import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.ingestion.pipeline.rabbitmq.RabbitMQService;

public class MessageProcessorImpl implements MessageProcessService {

  private static final Logger LOGGER = LogManager.getLogger(MessageProcessorImpl.class);
  private final Vertx vertx;
  private final Map<String, Object> mappings;
  private final String defaultAttribValue = DEFAULT_SUFFIX;
  private final RabbitMQService rabbitMQService;

  public MessageProcessorImpl(Vertx vertx, Map<String, Object> mappings, RabbitMQService rabbitMQService) {
    this.vertx = vertx;
    this.mappings = mappings;
    this.rabbitMQService = rabbitMQService;
  }

  @Override
  public MessageProcessService process(JsonObject message, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.trace("message procesing starts : " + message);
    if (message == null || message.isEmpty()) {
      handler.handle(Future.failedFuture("empty/null message received"));
    } else {
      JsonObject processedJson = JsonObject.mapFrom(getProcessedMessage(message));
      JsonObject json = new JsonObject();
      json.put("body", processedJson.toString());
      rabbitMQService.publish(RMQ_PROCESSED_MSG_EX, RMQ_PROCESSED_MSG_EX_ROUTING_KEY, json, publishHandler -> {
        if (publishHandler.succeeded()) {
          LOGGER.debug("published");
          handler.handle(Future.succeededFuture(new JsonObject().put("result", "published")));
        } else {
          LOGGER.error("published failed" + publishHandler.cause().getMessage());
          handler.handle(Future.failedFuture("publish failed"));
        }
      });

    }
    return this;
  }

  private ProcessedMessage getProcessedMessage(JsonObject json) {
    StringBuilder id = new StringBuilder(json.getString("id"));
    String pathParamAttribute = (String) mappings.get(id.toString());
    StringBuilder pathParam = new StringBuilder();
    if (pathParamAttribute == null || pathParamAttribute.isBlank()) {
      id.append("/").append(defaultAttribValue);
      pathParam.append("_").append(DigestUtils.shaHex(id.toString()));
    } else {
      String value = (String) json.getString(pathParamAttribute);
      id.append("/").append(value);
      pathParam.append("_").append(DigestUtils.shaHex(id.toString()));
    }
    ProcessedMessage message =
        new ProcessedMessage(json.getString("id").replaceAll("/", "_")
            .replaceAll("-", "_")
            .replaceAll("\\.", "_"), pathParam.toString(), json);
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
