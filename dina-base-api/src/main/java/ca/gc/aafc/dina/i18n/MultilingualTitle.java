package ca.gc.aafc.dina.i18n;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.javers.core.metamodel.annotation.Value;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Value
public class MultilingualTitle {

  private List<@Valid MultilingualTitlePair> titles;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Value
  public static class MultilingualTitlePair {

    @NotEmpty
    // 2 or 3 letters ISO 639 code
    @Pattern(regexp = "^[a-zA-Z]{2,3}$")
    private String lang;

    @Size(min = 2, max = 1000)
    private String title;

    public static MultilingualTitlePair of(String lang, String title) {
      return new MultilingualTitlePair(lang, title);
    }
  }

  /**
   * Will probably be removed since Bean Validation can handle it now.
   *
   * Checks if titles contains any entry with a blank title.
   * @return true if at least 1 element contains a blank title. false otherwise or if
   * title is empty.
   */
  public boolean hasBlankTitle() {
    if (CollectionUtils.isNotEmpty(titles)) {
      return titles.stream().map(MultilingualTitlePair::getTitle).anyMatch(StringUtils::isBlank);
    }
    return false;
  }

}
