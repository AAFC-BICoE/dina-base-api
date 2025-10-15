package ca.gc.aafc.dina.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ValueHolderTest {

  @Test
  public void testValueHolder() {
    ValueHolder<Integer> myVal = ValueHolder.undefined();
    assertInstanceOf(ValueHolder.Undefined.class, myVal);

    ValueHolder<Integer> myVal2 = ValueHolder.of(18);
    assertInstanceOf(ValueHolder.Defined.class, myVal2);

    ValueHolder.Defined<Integer> theVal = myVal2.asDefined().orElseThrow(() -> new IllegalStateException("Expected Defined"));
    assertEquals(18, theVal.value());

    ValueHolder<Integer> myVal3 = ValueHolder.ofNull();
    assertInstanceOf(ValueHolder.Null.class, myVal3);

    // special case
    ValueHolder<Integer> myVal4 = ValueHolder.of(null);
    assertInstanceOf(ValueHolder.Null.class, myVal4);
  }
}
