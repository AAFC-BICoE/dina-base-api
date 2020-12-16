package ca.gc.aafc.dina.repository.meta;

public interface WarningInfoHandler<T extends WarningInfoProvider> {

  void loadWarnings(T resource);

}
