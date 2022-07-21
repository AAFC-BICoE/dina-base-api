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

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@RequiredArgsConstructor
@Value
public class MultilingualTitle {

  private List<MultilingualTitlePair> titles;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Value
  public static class MultilingualTitlePair {
    private String lang;
    private String title;

    public static MultilingualTitlePair of(String lang, String title) {
      return new MultilingualTitlePair(lang, title);
    }
  }

  /**
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
