package iudx.ingestion.pipeline.cache.cacheImpl;

public enum CacheType {
  UNIQUE_ATTRIBUTES("unique_attributes");

  String cacheName;

  CacheType(String name) {
    this.cacheName = name;
  }


}