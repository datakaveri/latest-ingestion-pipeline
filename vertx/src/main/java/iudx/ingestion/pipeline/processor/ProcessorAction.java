package iudx.ingestion.pipeline.processor;

import io.vertx.core.json.JsonObject;

interface ProcessorAction {
  void process(JsonObject json);
}
