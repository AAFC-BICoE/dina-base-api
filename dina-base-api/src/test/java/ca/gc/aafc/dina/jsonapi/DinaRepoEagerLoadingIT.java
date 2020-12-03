package ca.gc.aafc.dina.jsonapi;

import ca.gc.aafc.dina.dto.ChainDto;
import ca.gc.aafc.dina.dto.ChainTemplateDto;
import ca.gc.aafc.dina.entity.Chain;
import ca.gc.aafc.dina.entity.ChainTemplate;
import ca.gc.aafc.dina.filter.DinaFilterResolver;
import ca.gc.aafc.dina.jpa.BaseDAO;
import ca.gc.aafc.dina.mapper.DinaMapper;
import ca.gc.aafc.dina.repository.DinaRepository;
import ca.gc.aafc.dina.service.DefaultDinaService;
import ca.gc.aafc.dina.testsupport.BaseRestAssuredTest;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPIRelationship;
import ca.gc.aafc.dina.testsupport.jsonapi.JsonAPITestHelper;
import io.crnk.core.queryspec.PathSpec;
import io.crnk.core.queryspec.QuerySpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

@SpringBootTest(
  properties = {"dev-user.enabled: true", "keycloak.enabled: false"},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class DinaRepoEagerLoadingIT extends BaseRestAssuredTest {

  public static final String CHAIN_TEMPLATE_PATH = "chainTemplate";
  public static final String CHAIN_PATH = "chain";

  @Inject
  DinaRepository<ChainTemplateDto, ChainTemplate> templateRepoUnderTest;
  @Inject
  DinaRepository<ChainDto, Chain> chainRepoUnderTest;

  protected DinaRepoEagerLoadingIT() {
    super("");
  }

  @Test
  void findOne_mapsLazyLoadedRelations() {
    ChainTemplateDto persistedTemplate = persistTemplate();
    ChainDto chainDto = newChain();

    String chainID = sendPost(CHAIN_PATH, chainToMap(persistedTemplate, chainDto)).extract()
      .body()
      .jsonPath()
      .getString("data.id");

    QuerySpec querySpec = new QuerySpec(ChainDto.class);
    querySpec.includeRelation(PathSpec.of("chainTemplate"));
    ChainDto resultChain = chainRepoUnderTest.findOne(UUID.fromString(chainID), querySpec);

    assertChain(chainDto, resultChain);
    assertTemplate(persistedTemplate, resultChain.getChainTemplate());
  }

  @Test
  void findAll_mapsLazyLoadedRelations() {
    ChainTemplateDto persistedTemplate = persistTemplate();
    ChainDto chainDto = newChain();

    String chainID = sendPost(CHAIN_PATH, chainToMap(persistedTemplate, chainDto)).extract()
      .body()
      .jsonPath()
      .getString("data.id");

    QuerySpec querySpec = new QuerySpec(ChainDto.class);
    querySpec.includeRelation(PathSpec.of("chainTemplate"));
    ChainDto resultChain = chainRepoUnderTest.findAll(
      Collections.singletonList(UUID.fromString(chainID)),
      querySpec).get(0);

    assertChain(chainDto, resultChain);
    assertTemplate(persistedTemplate, resultChain.getChainTemplate());
  }

  private void assertChain(ChainDto expected, ChainDto result) {
    Assertions.assertEquals(expected.getName(), result.getName());
    Assertions.assertEquals(expected.getCreatedBy(), result.getCreatedBy());
    Assertions.assertEquals(expected.getGroup(), result.getGroup());
  }

  private void assertTemplate(ChainTemplateDto expected, ChainTemplateDto result) {
    Assertions.assertEquals(expected.getName(), result.getName());
    Assertions.assertEquals(expected.getCreatedBy(), result.getCreatedBy());
    Assertions.assertEquals(expected.getGroup(), result.getGroup());
  }

  private ChainTemplateDto persistTemplate() {
    ChainTemplateDto template = newTemplate();
    String templateId = sendPost(CHAIN_TEMPLATE_PATH, templateToMap(template)).extract()
      .body()
      .jsonPath()
      .getString("data.id");
    ChainTemplateDto persistedTemplate = templateRepoUnderTest.findOne(
      UUID.fromString(templateId),
      new QuerySpec(ChainTemplateDto.class));
    assertTemplate(template, persistedTemplate);
    return persistedTemplate;
  }

  private static Map<String, Object> chainToMap(ChainTemplateDto relation, ChainDto chainDto) {
    JsonAPIRelationship relationship = JsonAPIRelationship.of(
      CHAIN_TEMPLATE_PATH,
      CHAIN_TEMPLATE_PATH,
      relation.getUuid().toString());
    return JsonAPITestHelper.toJsonAPIMap(
      CHAIN_PATH,
      JsonAPITestHelper.toAttributeMap(chainDto),
      JsonAPITestHelper.toRelationshipMap(relationship),
      null);
  }

  private Map<String, Object> templateToMap(ChainTemplateDto template) {
    return JsonAPITestHelper.toJsonAPIMap(
      CHAIN_TEMPLATE_PATH,
      JsonAPITestHelper.toAttributeMap(template),
      null,
      null);
  }

  private static ChainDto newChain() {
    ChainDto chainDto = new ChainDto();
    chainDto.setName(RandomStringUtils.randomAlphabetic(4));
    chainDto.setGroup(RandomStringUtils.randomAlphabetic(5));
    chainDto.setCreatedBy(RandomStringUtils.randomAlphabetic(8));
    return chainDto;
  }

  private static ChainTemplateDto newTemplate() {
    ChainTemplateDto template = new ChainTemplateDto();
    template.setName(RandomStringUtils.randomAlphabetic(4));
    template.setGroup(RandomStringUtils.randomAlphabetic(5));
    template.setCreatedBy(RandomStringUtils.randomAlphabetic(8));
    return template;
  }

  @TestConfiguration
  static class DinaRepoBulkOperationITConfig {
    @Bean
    public DinaRepository<ChainDto, Chain> chainRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new DinaRepository<>(
        new DefaultDinaService<>(baseDAO) {
          @Override
          protected void preCreate(Chain entity) {
            entity.setUuid(UUID.randomUUID());
          }
        },
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ChainDto.class),
        ChainDto.class,
        Chain.class,
        filterResolver,
        null,
        new BuildProperties(new Properties())
      );
    }

    @Bean
    public DinaRepository<ChainTemplateDto, ChainTemplate> TemplateRepo(
      BaseDAO baseDAO,
      DinaFilterResolver filterResolver
    ) {
      return new DinaRepository<>(
        new DefaultDinaService<>(baseDAO) {
          @Override
          protected void preCreate(ChainTemplate entity) {
            entity.setUuid(UUID.randomUUID());
            super.preUpdate(entity);
          }
        },
        Optional.empty(),
        Optional.empty(),
        new DinaMapper<>(ChainTemplateDto.class),
        ChainTemplateDto.class,
        ChainTemplate.class,
        filterResolver,
        null,
        new BuildProperties(new Properties())
      );
    }
  }

}
