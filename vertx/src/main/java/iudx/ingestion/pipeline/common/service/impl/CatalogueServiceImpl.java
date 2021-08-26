package iudx.ingestion.pipeline.common.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import iudx.ingestion.pipeline.common.service.CatalogueService;

public class CatalogueServiceImpl implements CatalogueService {


  private static final Logger LOGGER = LogManager.getLogger(CatalogueServiceImpl.class);
  private WebClient webClient;
  private String host;
  private int port;

  public CatalogueServiceImpl(WebClient webClient, JsonObject config) {
    this.webClient = webClient;
    this.host = config.getString("catalogueHost");
    this.port = config.getInteger("cataloguePort");
  }

  @Override
  public Future<List<String>> getAllAvailableRG() {
    Promise<List<String>> promise = Promise.promise();
    webClient.get(port, host, "/iudx/cat/v1/list/resourceGroup").send(handler -> {
      if (handler.succeeded()) {
        JsonObject response = handler.result().bodyAsJsonObject();
        List<String> availableRG = response.getJsonArray("results")
            .stream()
            .map(e -> (String) e)
            .collect(Collectors.toList());
        promise.complete(availableRG);
      } else {
        promise.fail("failed to retreive resource groups");
      }
    });
    return promise.future();
  }

}
