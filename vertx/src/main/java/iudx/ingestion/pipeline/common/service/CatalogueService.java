package iudx.ingestion.pipeline.common.service;

import java.util.List;

import io.vertx.core.Future;

public interface CatalogueService {

  public Future<List<String>> getAllAvailableRG();
}
