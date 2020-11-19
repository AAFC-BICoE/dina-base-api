package ca.gc.aafc.dina.dto;

import java.util.UUID;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;

import ca.gc.aafc.dina.entity.ObjectUpload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@RelatedEntity(ObjectUpload.class)
@Data
@JsonApiResource(type = ObjectUploadDto.TYPENAME)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ObjectUploadDto  extends ResourceMetaInfo{ 

  public static final String TYPENAME = "object-upload";
  
  @JsonApiId
  private UUID fileIdentifier;
  private String originalFilename;
  private String sha1Hex;  
  private String bucket;  


}
