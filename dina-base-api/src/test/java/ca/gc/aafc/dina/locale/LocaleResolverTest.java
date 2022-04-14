package ca.gc.aafc.dina.locale;

import ca.gc.aafc.dina.DinaBaseApiAutoConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {DinaBaseApiAutoConfiguration.class})
public class LocaleResolverTest {

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
