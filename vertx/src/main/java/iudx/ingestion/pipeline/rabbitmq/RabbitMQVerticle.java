package iudx.ingestion.pipeline.rabbitmq;

import static iudx.ingestion.pipeline.common.Constants.CACHE_SERVICE_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.MSG_PROCESS_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.REDIS_SERVICE_ADDRESS;
import static iudx.ingestion.pipeline.common.Constants.RMQ_SERVICE_ADDRESS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ingestion.pipeline.cache.CacheService;
import iudx.ingestion.pipeline.common.IConsumer;
import iudx.ingestion.pipeline.common.IProducer;
import iudx.ingestion.pipeline.common.VHosts;
import iudx.ingestion.pipeline.processor.MessageProcessService;
import iudx.ingestion.pipeline.rabbitmq.consumers.ProcessedMessageConsumer;
import iudx.ingestion.pipeline.rabbitmq.consumers.UniqueAttributeConsumer;
import iudx.ingestion.pipeline.rabbitmq.consumers.LatestMessageConsumer;
import iudx.ingestion.pipeline.redis.RedisService;

public class RabbitMQVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(RabbitMQVerticle.class);


  private IProducer rabbitProducer;
  private RabbitMQService rabbitMQService;

  private MessageProcessService messageProcessService;
  private RedisService redisService;
  private CacheService cacheService;

  private IConsumer processedMsgConsumer;
  private IConsumer latestMessageConsumer;
  private IConsumer uniqueAttribConsumer;

  private RabbitMQOptions config;
  private RabbitMQClient client;
  private String dataBrokerIP;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
  private String dataBrokerVhost;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private WebClientOptions webConfig;

  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  @Override
  public void start() throws Exception {

    dataBrokerIP = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort = config().getInteger("dataBrokerManagementPort");
    dataBrokerVhost = config().getString("dataBrokerVhost");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = config().getInteger("connectionTimeout");
    requestedHeartbeat = config().getInteger("requestedHeartbeat");
    handshakeTimeout = config().getInteger("handshakeTimeout");
    requestedChannelMax = config().getInteger("requestedChannelMax");
    networkRecoveryInterval = config().getInteger("networkRecoveryInterval");

    /* Configure the RabbitMQ Data Broker client with input from config files. */

    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIP);
    config.setPort(dataBrokerPort);
    config.setVirtualHost(dataBrokerVhost);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(true);

    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIP);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);


    RabbitMQOptions internalVhostOptions = new RabbitMQOptions(config);
    String internalCommVhost = config().getString(VHosts.IUDX_INTERNAL.value);
    internalVhostOptions.setVirtualHost(internalCommVhost);
    rabbitMQService = new RabbitMQServiceimpl(vertx, internalVhostOptions);

    messageProcessService = MessageProcessService.createProxy(vertx, MSG_PROCESS_ADDRESS);
    redisService = RedisService.createProxy(vertx, REDIS_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);

    // redis-latest Q will be in PROD Vhost
    RabbitMQOptions prodOptions = new RabbitMQOptions(config);
    String prodVhost = config().getString(VHosts.IUDX_PROD.value);
    prodOptions.setVirtualHost(prodVhost);
    latestMessageConsumer = new LatestMessageConsumer(vertx, prodOptions, messageProcessService);


    processedMsgConsumer = new ProcessedMessageConsumer(vertx, internalVhostOptions, redisService);
    uniqueAttribConsumer = new UniqueAttributeConsumer(vertx, internalVhostOptions, cacheService);

    processedMsgConsumer.start();
    latestMessageConsumer.start();
    uniqueAttribConsumer.start();

    binder = new ServiceBinder(vertx);

    consumer = binder
        .setAddress(RMQ_SERVICE_ADDRESS)
        .register(RabbitMQService.class, rabbitMQService);



  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
