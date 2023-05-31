package ca.gc.aafc.dina.filter;

import com.querydsl.core.types.Ops;

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
public record FilterExpression(String attribute, Ops operator, String value)
        implements FilterComponent { }
