package iudx.ingestion.pipeline.common;

public enum VHosts {


  IUDX_PROD("prodVhost"), IUDX_INTERNAL("internalVhost"), IUDX_EXTERNAL("externalVhost");

  public String value;

  VHosts(String value) {
    this.value = value;
  }

}
