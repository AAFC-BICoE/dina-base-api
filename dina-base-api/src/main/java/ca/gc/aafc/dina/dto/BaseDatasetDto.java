package ca.gc.aafc.dina.dto;

import java.util.UUID;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

import ca.gc.aafc.dina.entity.AgentRoles;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;
import lombok.Builder;
import lombok.Data;

/**
* Metadata describing a dataset for publication and exchange, including the
* information required to generate an Ecological Metadata Language (EML)
* document.
*/
@Data
public class BaseDatasetDto implements Serializable {

  public enum DatasetType {
    DWCA
  }

  public static final String AGENT_ROLE_CREATOR = "creator";
  public static final String AGENT_ROLE_METADATA_PROVIDER = "metadataProvider";

  protected UUID uuid;
  protected String group;

  protected MultilingualTitle multilingualTitle;
  protected MultilingualDescription multilingualDescription;
  protected DatasetType datasetType;
  protected List<AgentRoles> agentRoles = List.of();

  protected UsageRights usageRights;

  protected List<KeywordSet> keywordSets = List.of();
  protected Coverage coverage;

  /**
   * Information describing the licence and conditions governing the use and
   * redistribution of the dataset and its contents.
   */
  @Builder
  public record UsageRights(
    String licenseName,
    String licenseUrl,
    String usageTerms) implements Serializable {
  }

  /**
   * A collection of keywords used to describe the dataset, optionally associated
   * with a controlled vocabulary or thesaurus.
   */
  @Builder
  public record KeywordSet(
    List<String> keywords,
    String thesaurus) implements Serializable {
  }

  /**
   * Describes the spatial, temporal, and taxonomic scope of the dataset.
   */
  @Builder
  public record Coverage(
    GeographicCoverage geographic,
    TemporalCoverage temporal,
    List<TaxonomicCoverage> taxonomic) implements Serializable {
  }

  @Builder
  public record GeographicCoverage(
    String geographicDescription,
    BoundingBox boundingBox) implements Serializable {
  }

  @Builder
  public record BoundingBox(
    double west,
    double south,
    double east,
    double north) implements Serializable {
  }

  @Builder
  public record TemporalCoverage(
    LocalDate beginDate,
    LocalDate endDate) implements Serializable {
  }

  @Builder
  public record TaxonomicCoverage(
    String rank,
    String scientificName,
    String commonName) implements Serializable {
  }
  
}
