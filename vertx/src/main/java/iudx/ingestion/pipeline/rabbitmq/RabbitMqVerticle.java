package iudx.ingestion.pipeline.rabbitmq;

import static iudx.ingestion.pipeline.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ingestion.pipeline.cache.CacheService;
import iudx.ingestion.pipeline.common.ConsumerAction;
import iudx.ingestion.pipeline.common.VirtualHosts;
import iudx.ingestion.pipeline.processor.MessageProcessService;
import iudx.ingestion.pipeline.rabbitmq.consumers.LatestMessageConsumerAction;
import iudx.ingestion.pipeline.rabbitmq.consumers.ProcessedMessageConsumerAction;
import iudx.ingestion.pipeline.rabbitmq.consumers.UniqueAttributeConsumerAction;
import iudx.ingestion.pipeline.redis.RedisService;

public class RabbitMqVerticle extends AbstractVerticle {
  private RabbitMqService rabbitMqService;

  private MessageProcessService messageProcessService;
  private RedisService redisService;
  private CacheService cacheService;

  private ConsumerAction processedMsgConsumer;
  private ConsumerAction latestMessageConsumer;
  private ConsumerAction uniqueAttribConsumer;

  private RabbitMQOptions config;
  private String dataBrokerIp;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
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

    dataBrokerIp = config().getString("dataBrokerIP");
    dataBrokerPort = config().getInteger("dataBrokerPort");
    dataBrokerManagementPort = config().getInteger("dataBrokerManagementPort");
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
    config.setHost(dataBrokerIp);
    config.setPort(dataBrokerPort);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(true);

    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIp);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);

    RabbitMQOptions internalVhostOptions = new RabbitMQOptions(config);
    String internalCommVhost = config().getString(VirtualHosts.IUDX_INTERNAL.value);
    internalVhostOptions.setVirtualHost(internalCommVhost);
    rabbitMqService = new RabbitMqServiceImpl(vertx, internalVhostOptions);

    messageProcessService = MessageProcessService.createProxy(vertx, MSG_PROCESS_ADDRESS);
    redisService = RedisService.createProxy(vertx, REDIS_SERVICE_ADDRESS);
    cacheService = CacheService.createProxy(vertx, CACHE_SERVICE_ADDRESS);

    // redis-latest Q will be in PROD Vhost
    RabbitMQOptions prodOptions = new RabbitMQOptions(config);
    String prodVhost = config().getString(VirtualHosts.IUDX_PROD.value);
    prodOptions.setVirtualHost(prodVhost);
    latestMessageConsumer =
        new LatestMessageConsumerAction(vertx, prodOptions, messageProcessService);

    processedMsgConsumer =
        new ProcessedMessageConsumerAction(vertx, internalVhostOptions, redisService);
    uniqueAttribConsumer =
        new UniqueAttributeConsumerAction(vertx, internalVhostOptions, cacheService);

    processedMsgConsumer.start();
    latestMessageConsumer.start();
    uniqueAttribConsumer.start();

    binder = new ServiceBinder(vertx);

    consumer =
        binder.setAddress(RMQ_SERVICE_ADDRESS).register(RabbitMqService.class, rabbitMqService);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}
