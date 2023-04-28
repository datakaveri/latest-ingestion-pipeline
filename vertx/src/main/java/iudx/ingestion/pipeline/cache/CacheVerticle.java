package iudx.ingestion.pipeline.cache;

import static iudx.ingestion.pipeline.common.Constants.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.ingestion.pipeline.postgres.PostgresService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CacheVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LogManager.getLogger(CacheVerticle.class);

  private ServiceBinder binder;

  private CacheService cacheService;
  private PostgresService pgService;

  @Override
  public void start() throws Exception {

    pgService = PostgresService.createProxy(vertx, PG_SERVICE_ADDRESS);

    cacheService = new CacheServiceImpl(vertx, pgService);

    binder = new ServiceBinder(vertx);
    binder.setAddress(CACHE_SERVICE_ADDRESS).register(CacheService.class, cacheService);

    LOGGER.info("Cache Verticle deployed.");
  }
}
