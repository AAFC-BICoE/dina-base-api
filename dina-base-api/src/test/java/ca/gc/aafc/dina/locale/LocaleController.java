package ca.gc.aafc.dina.locale;

import java.util.Locale;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LocaleController {

  @Inject
  private MessageSource messageSource;

  @Autowired
  public LocaleController(MessageSource messageSource) {
      this.messageSource = messageSource;
  }


  @GetMapping(value = "/locale-testing")
  public String getGreeting() {
    Locale locale = LocaleContextHolder.getLocale();
    System.out.println("locale: " + locale);
    return messageSource.getMessage("greeting.hello", null, LocaleContextHolder.getLocale());
    }
}