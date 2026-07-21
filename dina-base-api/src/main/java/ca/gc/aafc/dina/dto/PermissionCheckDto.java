package ca.gc.aafc.dina.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

@JsonApiTypeForClass(PermissionCheckDto.TYPE_NAME)
@Data
@Builder
public class PermissionCheckDto {

  public static final String TYPE_NAME = "permission-check";

  // random id generated for every response
  @JsonApiId
  private String id;

  private String targetType;
  private Set<String> permissions;

  private String permissionsProvider;
  private Set<String> evaluatedAttributes;

}
