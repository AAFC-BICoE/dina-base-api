package ca.gc.aafc.dina.property;

import java.io.IOException;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class can be used with {@link PropertySource} factory to load the properties from a yaml file.
 *
 */
public class YamlPropertyLoaderFactory extends DefaultPropertySourceFactory {

  @Override
  @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "resource.getResource() can't return null")
  public PropertySource<?> createPropertySource(String name, EncodedResource resource)
      throws IOException {

    CompositePropertySource propertySource = new CompositePropertySource(
        resource.getResource().getFilename());
    new YamlPropertySourceLoader()
        .load(resource.getResource().getFilename(), resource.getResource())
        .forEach(propertySource::addPropertySource);
    return propertySource;
  }
}
