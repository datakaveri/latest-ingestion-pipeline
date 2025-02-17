package iudx.ingestion.pipeline.common;

public class Constants {

  public static final String REDIS_SERVICE_ADDRESS = "iudx.ingestion.redis.service";
  public static final String RMQ_SERVICE_ADDRESS = "iudx.ingestion.rabbit.service";
  public static final String MSG_PROCESS_ADDRESS = "iudx.ingestion.msg.service";
  public static final String CACHE_SERVICE_ADDRESS = "iudx.ingestion.cache.service";
  public static final String PG_SERVICE_ADDRESS = "iudx.ingestion.postgres.service";

  // RMQ
  public static final String RMQ_LATEST_DATA_Q = "redis-latest";
  public static final String SEQ_NUM = "seq_num";

  public static final String RMQ_PROCESSED_MSG_EX = "processed-messages";
  public static final String RMQ_PROCESSED_MSG_Q = "lip-processed-messages";
  public static final String RMQ_PROCESSED_MSG_EX_ROUTING_KEY = "processed";

  public static final String UNIQUE_ATTR_Q = "lip-unique-attributes";

  // EventBus
  public static final String EB_RECEIVED_MSG_ADDRESS = "received.message";
  public static final String EB_PROCESSED_MSG_ADDRESS = "processed.message";
  public static final String EB_PUBLISH_2_REDIS = "publish2redis.message";

  //
  public static final String DEFAULT_SUFFIX = "_d";

  // pg queries
  public static final String SELECT_UNIQUE_ATTRIBUTE = "SELECT * from unique_attributes";
}
