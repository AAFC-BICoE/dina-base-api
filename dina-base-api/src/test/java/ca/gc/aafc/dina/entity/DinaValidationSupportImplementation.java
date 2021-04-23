package ca.gc.aafc.dina.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import ca.gc.aafc.dina.entity.DinaValidationSupport;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, 
      include = JsonTypeInfo.As.PROPERTY, property = "type") 
@JsonSubTypes({ 
      @Type(value = Department.class, name = "department"),
      @Type(value = Employee.class, name = "employee")
   })
public interface DinaValidationSupportImplementation extends DinaValidationSupport {
  
}
