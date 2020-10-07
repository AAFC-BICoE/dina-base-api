package ca.gc.aafc.dina.repository;

import ca.gc.aafc.dina.dto.ExternalRelationDto;
import ca.gc.aafc.dina.repository.meta.ExternalResourceProvider;
import io.crnk.core.engine.information.InformationBuilder;
import io.crnk.core.engine.information.resource.ResourceFieldType;
import io.crnk.core.engine.registry.RegistryEntryBuilder;
import io.crnk.core.module.InitializingModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@ConditionalOnBean(ExternalResourceProvider.class)
public class ExternalTypeRepoInvoker implements InitializingModule {
  @Inject
  private ExternalResourceProvider externalResourceProvider;

  private ModuleContext context;

  @Override
  public void init() {
    externalResourceProvider.getTypes().forEach(aClass -> {
      RegistryEntryBuilder builder = context.newRegistryEntryBuilder();

      RegistryEntryBuilder.ResourceRepositoryEntryBuilder resourceRepository = builder.resourceRepository();
      resourceRepository.instance(new ExternalRepository(aClass));

      InformationBuilder.ResourceInformationBuilder resource = builder.resource();
      resource.resourceType(aClass);
      resource.implementationType(ExternalRelationDto.class);
      resource.addField("id", ResourceFieldType.ID, String.class);

      context.addRegistryEntry(builder.build());
    });
    RegistryEntryBuilder builder = context.newRegistryEntryBuilder();

    RegistryEntryBuilder.ResourceRepositoryEntryBuilder resourceRepository = builder.resourceRepository();
    resourceRepository.instance(new ExternalRepository("external-type"));

    InformationBuilder.ResourceInformationBuilder resource = builder.resource();
    resource.resourceType("external-type");
    resource.implementationType(ExternalRelationDto.class);
    resource.addField("id", ResourceFieldType.ID, String.class);

    context.addRegistryEntry(builder.build());
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
