package ca.gc.aafc.dina.cache;

import lombok.extern.log4j.Log4j2;
import org.springframework.util.StringUtils;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;

/**
 * {@link KeyGenerator} that will generate a key based on a method and its parameters.
 * Depending on the type of parameters this may or may not be suitable since toString will be called
 * in order to generate the key.
 */
@Log4j2
public class MethodBasedKeyGenerator implements KeyGenerator {

  public static final String NAME = "methodBasedKeyGenerator";

  @Override
  public Object generate(Object target, Method method, Object... params) {
    final String key = target.getClass().getSimpleName() + "_" + method.getName() + "_"
        + StringUtils.arrayToDelimitedString(params, "_");

    log.debug("Generated cache key: " + key);

    return key;
  }
}
