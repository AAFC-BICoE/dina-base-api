package ca.gc.aafc.dina.dto;

import java.util.Map;
import lombok.Data;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;

/**
 * Used to carry information about the api/module itself
 */
@JsonApiTypeForClass(ApiInfoDto.TYPE_NAME)
@Data
public class ApiInfoDto {

  public static final String TYPE_NAME = "api-info";

  @JsonApiId
  private String moduleVersion;

  private Boolean messageProducer;
  private Boolean messageConsumer;
  private boolean attentionRequired;

  private Map<String, Object> moduleInfo;

}
