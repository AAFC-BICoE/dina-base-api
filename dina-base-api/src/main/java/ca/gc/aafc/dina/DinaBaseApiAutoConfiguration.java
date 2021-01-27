package ca.gc.aafc.dina;

import io.crnk.core.engine.transaction.TransactionRunner;
import io.crnk.core.queryspec.mapper.DefaultQuerySpecUrlMapper;
import io.crnk.operations.server.OperationsModule;
import io.crnk.operations.server.TransactionOperationFilter;
import io.crnk.spring.jpa.SpringTransactionRunner;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.LocaleContextMessageInterpolator;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.inject.Inject;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Locale;

@Configuration
// Must explicitly depend on "querySpecUrlMapper" so Spring can inject it into this class'
// initQuerySpecUrlMapper method.
@DependsOn("querySpecUrlMapper")
public class DinaBaseApiAutoConfiguration implements WebMvcConfigurer {

  @Inject
  public void initQuerySpecUrlMapper(DefaultQuerySpecUrlMapper mapper) {
    // Disables Crnk's behavior of splitting up query params that contain commas into HashSets.
    // This will allow RSQL 'OR' filters like "name==primer2,name==primer4".
    mapper.setAllowCommaSeparatedValue(false);
  }

  /**
   * Registers the transaction filter that executes a transaction around bulk jsonpatch operations.
   *
   * @param module the Crnk operations module.
   */
  @Inject
  public void initTransactionOperationFilter(OperationsModule module) {
    module.addFilter(new TransactionOperationFilter());
    module.setIncludeChangedRelationships(false);
    module.setResumeOnError(true);
  }

  /**
   * Provides Crnk's SpringTransactionRunner that implements transactions around bulk jsonpatch operations
   * using Spring's transaction management.
   *
   * @return the transaction runner.
   */
  @Bean
  public TransactionRunner crnkSpringTransactionRunner() {
    return new SpringTransactionRunner();
  }

  @Bean
  public LocaleResolver localeResolver() {
    SessionLocaleResolver slr = new SessionLocaleResolver();
    slr.setDefaultLocale(Locale.ENGLISH);
    return slr;
  }

  @Bean
  public LocaleChangeInterceptor localeChangeInterceptor() {
    LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
    lci.setParamName("lang");
    return lci;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(localeChangeInterceptor());
  }

  @Bean
  public Validator validator(MessageSource source) {
    return Validation.byDefaultProvider()
      .configure()
      .messageInterpolator(
        new LocaleContextMessageInterpolator(
          new ResourceBundleMessageInterpolator(
            new MessageSourceResourceBundleLocator(
              source))))
      .buildValidatorFactory()
      .getValidator();
  }

}
