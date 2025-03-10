package ca.gc.aafc.dina.dto;

import java.util.UUID;

/**
 * Interface of a resource (usually DTO) that can be exposed in JSON:API.
 *
 * Required for resources being used in relationships to retrieve the type and uuid.
 * 
 * For example, when a resource has a relationship to another resource:
 * 
 * <pre>
 * <code>
 * &#064;JsonApiRelation
 * private List&lt;OrganizationDto&gt; organizations;
 * </code>
 * </pre>
 * 
 * The related resource (e.g. OrganizationDto) will need to implement JsonApiResource.
 */
public interface JsonApiResource {
  String getJsonApiType();
  UUID getJsonApiId();
}
