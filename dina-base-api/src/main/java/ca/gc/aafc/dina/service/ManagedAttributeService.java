package ca.gc.aafc.dina.service;

import ca.gc.aafc.dina.entity.ManagedAttribute;
import ca.gc.aafc.dina.jpa.BaseDAO;
import lombok.NonNull;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base Managed Attribute Service that takes care of the generation of the key.
 *
 * @param <T>
 */
public abstract class ManagedAttributeService<T extends ManagedAttribute>
  extends DefaultDinaService<T> {
  private static final String KEY = "key";
  private static final Pattern NON_ALPHANUMERICAL = Pattern.compile("[^a-z0-9]");
  private final Class<T> maClass;

  public ManagedAttributeService(BaseDAO baseDAO, @NonNull Class<T> managedAttributeClass) {
    super(baseDAO);
    this.maClass = managedAttributeClass;
  }

  @Override
  protected void preCreate(T entity) {
    if (StringUtils.isNotBlank(entity.getName())) {
      entity.setKey(generateKeyFromName(entity.getName()));
    }
  }

  @Override
  public void delete(T entity){
    throw new UnsupportedOperationException("DELETE");
  }

  public Map<String, T> findAttributesForKeys(Set<String> keySet) {
    if (CollectionUtils.isEmpty(keySet)) {
      return Map.of();
    }
    return this.findAll(
      maClass, (criteriaBuilder, eRoot) -> {
        CriteriaBuilder.In<String> in = criteriaBuilder.in(eRoot.get(KEY));
        keySet.forEach(in::value);
        return new Predicate[]{in};
      },
      null, 0, Integer.MAX_VALUE
    ).stream().collect(Collectors.toMap(ManagedAttribute::getKey, Function.identity()));
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
