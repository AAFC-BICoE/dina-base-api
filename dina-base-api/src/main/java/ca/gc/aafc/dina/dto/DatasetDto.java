package ca.gc.aafc.dina.dto;

import java.util.UUID;
import java.time.LocalDate;
import java.util.List;

import ca.gc.aafc.dina.entity.AgentRoles;
import ca.gc.aafc.dina.i18n.MultilingualDescription;
import ca.gc.aafc.dina.i18n.MultilingualTitle;

/**
* Metadata describing a dataset for publication and exchange, including the
* information required to generate an Ecological Metadata Language (EML)
* document.
*/
public class DatasetDto {

  public enum DatasetType {
    DWCA
  }

  protected UUID uuid;
  protected String group;

  protected MultilingualTitle multilingualTitle;
  protected MultilingualDescription multilingualDescription;
  protected DatasetType datasetType;
  protected List<AgentRoles> agentRoles;

  protected UsageRights usageRights;

  protected List<KeywordSet> keywordSets;
  protected Coverage coverage;

  /**
   * Information describing the licence and conditions governing the use and
   * redistribution of the dataset and its contents.
   */
  public record UsageRights(
    String licenseName,
    String licenseUrl,
    String usageTerms) {
  }

  /**
   * A collection of keywords used to describe the dataset, optionally associated
   * with a controlled vocabulary or thesaurus.
   */
  public record KeywordSet(
    List<String> keywords,
    String thesaurus) {
  }

  /**
   * Describes the spatial, temporal, and taxonomic scope of the dataset.
   */
  public record Coverage(
    GeographicCoverage geographic,
    TemporalCoverage temporal,
    List<TaxonomicCoverage> taxonomic) {
  }

  public record GeographicCoverage(
    String geographicDescription,
    BoundingBox boundingBox) {
  }

  public record BoundingBox(
    double west,
    double south,
    double east,
    double north) {
  }

  public record TemporalCoverage(
    LocalDate beginDate,
    LocalDate endDate) {
  }

  public record TaxonomicCoverage(
    String rank,
    String scientificName,
    String commonName) {
  }
  
}
