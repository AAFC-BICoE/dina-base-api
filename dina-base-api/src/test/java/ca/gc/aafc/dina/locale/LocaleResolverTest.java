package ca.gc.aafc.dina.locale;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.LocaleResolver;


import ca.gc.aafc.dina.DinaBaseApiAutoConfiguration;
//import ca.gc.aafc.dina.TestConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.inject.Inject;

@SpringBootTest(classes={DinaBaseApiAutoConfiguration.class})
public class LocaleResolverTest {

    private MockMvc mockMvc;

    MessageSource messageSource;

    @Inject
    LocaleResolver localeResolver;

    @Inject
    LocaleChangeInterceptor localeChangeInterceptor;

    @Inject
    InterceptorRegistry registry;

    @Before
    public void setup(){
        this.messageSource = getMessageSource();
        this.mockMvc = MockMvcBuilders.standaloneSetup(
            new LocaleController(messageSource))
            .setLocaleResolver(localeResolver)
            .build();
    }

    @Test
    public void default_ReturnEnglishGreeting() throws Exception {
        this.mockMvc.perform(get("/locale-testing")).andExpect(status().isOk())
        .andExpect(content().string("Hello"));
    }

    @Test
    public void french_ReturnFrenchGreeting() throws Exception {
        this.mockMvc.perform(get("/locale-testing").param("lang", "fr")).andExpect(status().isOk())
        .andExpect(content().string("Bonjour"));
    }

   @Test
    public void testThree() throws Exception {
        this.mockMvc.perform(get("/locale-testing?lang=fr")).andExpect(status().isOk())
        .andExpect(content().string("Bonjour"));
    }


    @Bean
    public MessageSource getMessageSource() {
      ReloadableResourceBundleMessageSource messageSource
          = new ReloadableResourceBundleMessageSource();   
      messageSource.setBasename("classpath:messages");
      messageSource.setDefaultEncoding("UTF-8");
      messageSource.setUseCodeAsDefaultMessage(true);
      return messageSource;
    }
    
}