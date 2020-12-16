package ca.gc.aafc.dina.repository.meta;

public interface DinaJsonMetaInfoHandler<T extends DinaJsonMetaInfoProvider> {

  void loadMeta(T resource);

}
