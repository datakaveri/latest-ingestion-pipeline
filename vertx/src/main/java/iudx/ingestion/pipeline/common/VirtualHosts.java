package iudx.ingestion.pipeline.common;

public enum VirtualHosts {


  IUDX_PROD("prodVhost"), IUDX_INTERNAL("internalVhost"), IUDX_EXTERNAL("externalVhost");

  public String value;

  VirtualHosts(String value) {
    this.value = value;
  }

}
