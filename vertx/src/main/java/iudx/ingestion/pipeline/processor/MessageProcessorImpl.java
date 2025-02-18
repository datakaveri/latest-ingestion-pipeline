package iudx.ingestion.pipeline.processor;

import static iudx.ingestion.pipeline.common.Constants.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.ingestion.pipeline.cache.CacheService;
import iudx.ingestion.pipeline.cache.cacheimpl.CacheType;
import iudx.ingestion.pipeline.rabbitmq.RabbitMqService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageProcessorImpl implements MessageProcessService {

  private static final Logger LOGGER = LogManager.getLogger(MessageProcessorImpl.class);
  private final CacheService cache;
  private final String defaultAttribValue = DEFAULT_SUFFIX;
  private final RabbitMqService rabbitMqService;

  public MessageProcessorImpl(CacheService cache, RabbitMqService rabbitMqService) {
    this.cache = cache;
    this.rabbitMqService = rabbitMqService;
  }

  @Override
  public MessageProcessService process(
      JsonObject message, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info("message procesing starts : ");
    message.remove(SEQ_NUM);
    if (message == null || message.isEmpty()) {
      handler.handle(Future.failedFuture("empty/null message received"));
    } else {

      Future<ProcessedMessage> processedMsgFuture = getMessage(message);

      processedMsgFuture.onSuccess(
          msgHandler -> {
            JsonObject processedJson = JsonObject.mapFrom(msgHandler);
            LOGGER.debug("message publishing to processed Q : ");
            rabbitMqService.publish(
                RMQ_PROCESSED_MSG_EX,
                RMQ_PROCESSED_MSG_EX_ROUTING_KEY,
                processedJson,
                publishHandler -> {
                  if (publishHandler.succeeded()) {
                    LOGGER.debug("published");
                    handler.handle(
                        Future.succeededFuture(new JsonObject().put("result", "published")));
                  } else {
                    LOGGER.error("published failed" + publishHandler.cause().getMessage());
                    handler.handle(Future.failedFuture("publish failed"));
                  }
                });
          });
    }
    return this;
  }

  private Future<ProcessedMessage> getMessage(JsonObject json) {
    Promise<ProcessedMessage> promise = Promise.promise();

    StringBuilder id = new StringBuilder(json.getString("id"));

    JsonObject cacheJson = new JsonObject();
    cacheJson.put("type", CacheType.UNIQUE_ATTRIBUTES);
    cacheJson.put("key", id);

    cache.get(
        cacheJson,
        cacheHandler -> {
          if (cacheHandler.succeeded()) {
            JsonObject uaJson = cacheHandler.result();
            String uniqueAttrib = uaJson.getString("value");
            ProcessedMessage message = getProcessedMessage(json, uniqueAttrib);
            promise.complete(message);
          } else {
            ProcessedMessage message = getProcessedMessage(json, null);
            promise.complete(message);
          }
        });

    return promise.future();
  }

  private ProcessedMessage getProcessedMessage(JsonObject json, String pathParamAttribute) {
    StringBuilder id = new StringBuilder(json.getString("id"));
    StringBuilder pathParam = new StringBuilder();
    if (pathParamAttribute == null || pathParamAttribute.isBlank()) {
      id.append("/").append(defaultAttribValue);
      pathParam.append("_").append(DigestUtils.shaHex(id.toString()));
    } else {
      String value = json.getString(pathParamAttribute);
      id.append("/").append(value);
      pathParam.append("_").append(DigestUtils.shaHex(id.toString()));
    }
    ProcessedMessage message =
        new ProcessedMessage(json.getString("id"), pathParam.toString(), json);
    return message;
  }

  private class ProcessedMessage {
    @JsonProperty("key")
    private final String key;

    @JsonProperty("path_param")
    private final String pathParam;

    @JsonProperty("data")
    private final JsonObject data;

    ProcessedMessage(String key, String pathParam, JsonObject data) {
      this.key = key;
      this.pathParam = pathParam;
      this.data = data;
    }

    public String getKey() {
      return key;
    }

    public String getPathParam() {
      return pathParam;
    }

    public JsonObject getData() {
      return data;
    }

    @Override
    public String toString() {
      return "Key : " + this.key + " pathParam : " + pathParam + " data : " + data;
    }
  }
}
