package iudx.ingestion.pipeline.common;

public class Constants {

  public static final String REDIS_SERVICE_ADDRESS = "iudx.ingestion.redis.service";
  public static final String RMQ_SERVICE_ADDRESS = "iudx.ingestion.rabbit.service";
  public static final String MSG_PROCESS_ADDRESS = "iudx.ingestion.msg.service";
  public static final String CACHE_SERVICE_ADDRESS = "iudx.ingestion.cache.service";
  public static final String PG_SERVICE_ADDRESS = "iudx.ingestion.postgres.service";

  // RMQ
  public final static String RMQ_LATEST_DATA_Q = "redis-latest";
  public final static String RMQ_PROCESSED_DATA_Q = "vertx-rmq-redis-reader";

  public final static String RMQ_PROCESSED_MSG_EX = "vertx-rmq-redis-writer";
  public final static String RMQ_PROCESSED_MSG_EX_ROUTING_KEY = "processed";
  
  public final static String UNIQUE_ATTR_Q = "unique-attribute";

  // EventBus
  public final static String EB_RECEIVED_MSG_ADDRESS = "received.message";
  public final static String EB_PROCESSED_MSG_ADDRESS = "processed.message";
  public final static String EB_PUBLISH_2_REDIS = "publish2redis.message";


  //
  public final static String DEFAULT_SUFFIX = "_d";
  
  
  //pg queries
  public final static String SELECT_UNIQUE_ATTRIBUTE = "SELECT * from unique_attributes";

}
