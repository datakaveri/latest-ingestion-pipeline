package iudx.ingestion.pipeline.processor;

import io.vertx.core.json.JsonObject;

interface IProcessor {
  public void process(JsonObject json);
}
