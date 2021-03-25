package iudx.ingestion.pipeline.redis;

import static iudx.ingestion.pipeline.common.Constants.EB_PUBLISH_2_REDIS;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import iudx.ingestion.pipeline.common.IProducer;


class RedisProducer implements IProducer {

  private final Vertx vertx;
  private final RedisClient redisClient;


  public RedisProducer(Vertx vertx, RedisClient client) {
    this.vertx = vertx;
    this.redisClient = client;
  }

  @Override
  public void start() {
    publish();
  }



  private void publish() {
    vertx.eventBus().consumer(EB_PUBLISH_2_REDIS, msg -> {
      if (msg.body() != null) {
        JsonObject fromEventBus = new JsonObject(msg.body().toString());
        String messageKey = fromEventBus.getString("key");
        
        redisClient.get(messageKey, ".").onComplete(handler -> {
          if (handler.succeeded()) {
            // key found
            System.out.println("key found : "+messageKey);
            JsonObject fromRedis = handler.result();
            // if (isValidMessage2Push(fromRedis, fromEventBus)) {
            StringBuilder pathParam = new StringBuilder();
            pathParam.append(".")
                .append(messageKey)
                .append(".")
                .append(fromEventBus.getString("path_param"));
            String message=fromEventBus.getJsonObject("data").toString();
            com.google.gson.JsonObject json=new Gson().fromJson(message, com.google.gson.JsonObject.class);
            redisClient.put(messageKey, pathParam.toString(), (Object) json);
            // }
          } else {
            // key not found
            System.out.println("key not found : "+messageKey);
            Map<String,Object> origin =new HashMap<String, Object>();
            Map<String,Object> origin2=new HashMap<String,Object>();
            origin2.put(fromEventBus.getString("path_param"), new JsonObject());
            origin.put(messageKey, origin2);
            
            //put first entry for RG
            redisClient.put(messageKey, ".",(Object) origin).onComplete(firstEntryHandler -> {
              if (firstEntryHandler.succeeded()) {
                StringBuilder pathParam = new StringBuilder();
                pathParam.append(".")
                    .append(messageKey)
                    .append(".")
                    .append(fromEventBus.getString("path_param"));
                //push message to redis server
                String message=fromEventBus.getJsonObject("data").toString();
                com.google.gson.JsonObject json=new Gson().fromJson(message, com.google.gson.JsonObject.class);
                redisClient.put(messageKey, pathParam.toString(), (Object) json);
              }
            });
          }
        });
      }
    });
  }



  /**
   * check if message is valid through 'ObservationDateTime' field's in both messages.
   * 
   * @param fromRedis Json from Redis Cache.
   * @param latestJson Json from EB.
   * @return
   */
  private boolean isValidMessage2Push(JsonObject fromRedis, JsonObject latestJson) {
    String dateFromRedisData = fromRedis.getString("observationDateTime");
    String dateFromLatestData = latestJson.getString("observationDateTime");
    boolean result = Boolean.FALSE;
    System.out
        .println("from Redis : " + dateFromRedisData + " from Latest : " + dateFromLatestData);
    try {
      ZonedDateTime fromRedisData = ZonedDateTime.parse(dateFromRedisData);
      ZonedDateTime fromLatestData = ZonedDateTime.parse(dateFromLatestData);

      if (fromLatestData.isAfter(fromRedisData)) {
        result = Boolean.TRUE;
      }
    } catch (DateTimeParseException e) {
      result = Boolean.FALSE;
    }
    return result;
  }
  

}
