package ca.gc.aafc.dina.i18n;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.metamodel.annotation.Value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Builder
@Setter
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Value
public class MultilingualDescription {

  private List<@Valid MultilingualPair> descriptions;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Value
  public static class MultilingualPair {

    @NotEmpty
    // 2 or 3 letters ISO 639 code
    @Pattern(regexp = "^[a-zA-Z]{2,3}$")
    private String lang;

    @Size(min = 2, max = 5000)
    private String desc;

    public static MultilingualPair of(String lang, String desc) {
      return new MultilingualPair(lang, desc);
    }
  }

  /**
   * Will probably be removed since Bean Validation can handle it now.
   *
   * Checks if descriptions contains any entry with a blank description.
   * @return true if at least 1 element contains a blank description. false otherwise or if
   * descriptions is empty.
   */
  public boolean hasBlankDescription() {
    if (CollectionUtils.isNotEmpty(descriptions)) {
      return descriptions.stream().map(MultilingualPair::getDesc).anyMatch(StringUtils::isBlank);
    }
    return false;
  }


}
