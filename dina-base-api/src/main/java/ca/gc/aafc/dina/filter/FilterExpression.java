package ca.gc.aafc.dina.filter;

import com.querydsl.core.types.Ops;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a filter expression used for filtering data.
 * 
 * A filter expression consists of an attribute, an operator, and a value. It is used to define a 
 * condition for filtering data in various filtering systems. The attribute represents the field or 
 * property to be filtered, the operator specifies the comparison or logical operation to be 
 * performed, and the value represents the value against which the attribute is compared.
 * 
 * This class implements the {@link FilterComponent} interface, indicating that it can be used as a 
 * filter component within a filtering system.
 */
@Getter
@Setter
public class FilterExpression implements FilterComponent {

  /**
   * The attribute to be filtered.
   */
  private String attribute;

  /**
   * The operation to be performed. 
   * Example: "EQ" enum for equals.
   */
  private Ops operator;

  /**
   * The value to be compared with the attribute.
   */
  private String value;

}
