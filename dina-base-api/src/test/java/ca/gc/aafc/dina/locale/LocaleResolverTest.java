package ca.gc.aafc.dina.locale;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ca.gc.aafc.dina.DinaBaseApiAutoConfiguration;
import ca.gc.aafc.dina.mapper.JpaDtoMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootApplication
@SpringBootTest(classes = { LocaleResolverTest.LocaleResolverTestConfig.class, DinaBaseApiAutoConfiguration.class })
public class LocaleResolverTest {

  @TestConfiguration
  static class LocaleResolverTestConfig {
    // this bean will be injected into the OrderServiceTest class
    @Bean
    public JpaDtoMapper jpaDtoMapper() {
      return new JpaDtoMapper(new HashMap<>(), new HashMap<>());
    }
  }

  @Autowired
  private WebApplicationContext wac;

  private MockMvc mockMvc;

  @Before
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

}
