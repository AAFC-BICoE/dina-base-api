package ca.gc.aafc.dina.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MethodBasedKeyGeneratorTest {

  @Test
  public void methodBasedKeyGenerator_onGenerate_ExpectedKeyReturned() throws NoSuchMethodException {
    MethodBasedKeyGenerator keyGenerator = new MethodBasedKeyGenerator();

    // mimic what Spring Cache will do
    ClassWithCache instance = new ClassWithCache();
    String param = "qwerty";
    String key = keyGenerator
        .generate(instance, ClassWithCache.class.getMethod("callWithCache", String.class), param)
        .toString();
    assertEquals("ClassWithCache_callWithCache_qwerty", key);

    // make sure null can be handled
    key = keyGenerator
        .generate(instance, ClassWithCache.class.getMethod("callWithCache", String.class),
            (Object) null)
        .toString();
    assertEquals("ClassWithCache_callWithCache_null", key);
  }

  /**
   * Simulates a class that has a method where the result could be cached.
   */
  static final class ClassWithCache {
    public int callWithCache(String s) {
      return s == null ? 0 : s.length();
    }
  }
}
