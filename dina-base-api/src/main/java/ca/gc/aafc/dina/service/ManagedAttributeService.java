package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import lombok.NonNull;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base Managed Attribute Service that takes care of the generation of the key.
 * @param <T>
 */
public abstract class ManagedAttributeService<T extends ManagedAttribute>
    extends DefaultDinaService<T> {

  private static final Pattern NON_ALPHANUMERICAL = Pattern.compile("[^a-z0-9]");

  public ManagedAttributeService(BaseDAO baseDAO) {
    super(baseDAO);
  }

  @Override
  protected void preCreate(T entity) {
    if (StringUtils.isNotBlank(entity.getName())) {
      entity.setKey(generateKeyFromName(entity.getName()));
    }
  }

  /**
   * Transforms a name into a key. camelCase is supported.
   * "Aa bb !! mySuperAttribute # 11" will become aa_bb_my_super_attribute_11
   * @param name
   * @return
   */
  private static String generateKeyFromName(String name) {
    Objects.requireNonNull(name);

    return Arrays.stream(StringUtils.
        splitByCharacterTypeCamelCase(StringUtils.normalizeSpace(name)))
        .filter(StringUtils::isNotBlank)
        .map(ManagedAttributeService::processName)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.joining("_"));
  }

  private static String processName(String name) {
    return RegExUtils.removeAll(name.toLowerCase(), NON_ALPHANUMERICAL);
  }

}
