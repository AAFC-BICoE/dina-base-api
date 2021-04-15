package ca.gc.aafc.dina.locale;

import java.util.Locale;

import javax.inject.Inject;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.SupportedLanguagesConfiguration;
import ca.gc.aafc.dina.TestDinaBaseApp;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SupportedLanguagesConfigurationTest {

  @Inject
  private SupportedLanguagesConfiguration supportedLanguagesConfiguration;

  @Test
  public void getStringLanguages() {
    MatcherAssert.assertThat(
      supportedLanguagesConfiguration.getSupportedStringLanguages(),
      Matchers.contains(
        "fr",
        "en"
      )
    );
  }

  @Test
  public void getLocaleLanguages() {
    MatcherAssert.assertThat(
      supportedLanguagesConfiguration.getSupportedLocaleLanguages(),
      Matchers.contains(
      Locale.FRENCH,  
      Locale.ENGLISH
      )
    );
  }
}
