package ca.gc.aafc.dina.repository.validation;

import java.util.List;

public interface ValidationResourceConfiguration {

  List<ValidationResourceHandler<?>> getValidationHandlers();

}
