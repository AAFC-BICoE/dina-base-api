package ca.gc.aafc.dina.locale;

import javax.inject.Inject;

import ca.gc.aafc.dina.testsupport.PostgresTestContainerInitializer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import ca.gc.aafc.dina.SupportedLanguagesConfiguration;
import ca.gc.aafc.dina.TestDinaBaseApp;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = TestDinaBaseApp.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = { PostgresTestContainerInitializer.class })
public class SupportedLanguagesConfigurationTest {

  @Inject
  private SupportedLanguagesConfiguration supportedLanguagesConfiguration;

  @Test
  public void getStringLanguages() {
    MatcherAssert.assertThat(
      supportedLanguagesConfiguration.getSupportedLanguages(),
      Matchers.contains(
        "fr",
        "en"
      )
    );
  }
}
