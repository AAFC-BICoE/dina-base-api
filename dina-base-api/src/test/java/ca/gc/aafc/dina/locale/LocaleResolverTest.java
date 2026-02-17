package ca.gc.aafc.dina.locale;

import jakarta.inject.Named;

import org.javers.spring.boot.sql.JaversSqlAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import ca.gc.aafc.dina.DinaBaseApiAutoConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {DinaBaseApiAutoConfiguration.class})
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, JaversSqlAutoConfiguration.class})
@Import(LocaleResolverTest.LocaleTestBundleConfig.class)
public class LocaleResolverTest {

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
  }

  @Test
  public void defaultLocale_ReturnEnglishGreeting() throws Exception {
    this.mockMvc.perform(get("/locale-testing")).andExpect(status().isOk()).andExpect(content().string("Hello"));
  }

  @Test
  public void localeSetToFrench_ReturnFrenchGreeting() throws Exception {
    this.mockMvc.perform(get("/locale-testing").param("lang", "fr")).andExpect(status().isOk())
        .andExpect(content().string("Bonjour"));
  }

  @Test
  public void localeSetToEnglish_ReturnEnglishGreeting() throws Exception {
    this.mockMvc.perform(get("/locale-testing?lang=en"))
      .andExpect(status().isOk())
      .andExpect(content().string("Hello"));
  }

  @TestConfiguration
  static class LocaleTestBundleConfig {
    @Bean
    @Named("testBundle")
    public MessageSource messageSourceTestBundle() {
      ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
      messageSource.setAlwaysUseMessageFormat(true);

      messageSource.setBasename("classpath:messages");
      messageSource.setDefaultEncoding("UTF-8");
      return messageSource;
    }

    @RestController
    static class LocaleController {

      private final MessageSource messageSource;

      public LocaleController(@Named("testBundle") MessageSource messageSource) {
        this.messageSource = messageSource;
      }

      @GetMapping(value = "/locale-testing")
      public String getGreeting() {
        return messageSource.getMessage("greeting.hello", null, LocaleContextHolder.getLocale());
      }
    }
  }
}
