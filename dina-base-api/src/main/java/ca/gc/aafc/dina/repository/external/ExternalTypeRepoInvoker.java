package ca.gc.aafc.dina.repository.external;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import io.crnk.core.engine.information.resource.ResourceFieldType;
import io.crnk.core.engine.registry.RegistryEntry;
import io.crnk.core.engine.registry.RegistryEntryBuilder;
import io.crnk.core.module.InitializingModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Sets up the external type repositories as provided by a {@link ExternalResourceProvider}. This
 * component is conditional on a {@link ExternalResourceProvider} being present in the application
 * context.
 */
@Component
@ConditionalOnBean(ExternalResourceProvider.class)
public class ExternalTypeRepoInvoker implements InitializingModule {

  @Inject
  private ExternalResourceProvider externalResourceProvider;

  private ModuleContext context;

  @Override
  public void init() {
    externalResourceProvider.getTypes().forEach(aClass ->
      context.addRegistryEntry(getRegistryEntryForType(aClass, context.newRegistryEntryBuilder())));
    context.addRegistryEntry(
      getRegistryEntryForType("external-type", context.newRegistryEntryBuilder()));
  }

  private static RegistryEntry getRegistryEntryForType(String type, RegistryEntryBuilder builder) {
    builder.resourceRepository().instance(new ExternalRepository(type));
    builder.resource()
      .resourceType(type)
      .implementationType(ExternalRelationDto.class)
      .addField("id", ResourceFieldType.ID, String.class);
    return builder.build();
  }

  @Override
  public String getModuleName() {
    return "external type registry entry module";
  }

  @Override
  public void setupModule(ModuleContext moduleContext) {
    this.context = moduleContext;
  }
}
