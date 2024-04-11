package ca.gc.aafc.auto;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ca.gc.aafc.dina.config.ResourceNameIdentifierConfig;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.service.ResourceNameIdentifierService;

@Configuration
public class ResourceNameIdentifierAutoConfiguration {

  @Bean
  @ConditionalOnBean(ResourceNameIdentifierConfig.class)
  public ResourceNameIdentifierService resourceNameIdentifierService(BaseDAO baseDAO, ResourceNameIdentifierConfig config) {
    return new ResourceNameIdentifierService(baseDAO, config);
  }

}
