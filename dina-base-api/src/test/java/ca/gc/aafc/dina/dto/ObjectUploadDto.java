package ca.gc.aafc.dina.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import ca.gc.aafc.dina.entity.ObjectUpload;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.crnk.core.resource.annotations.JsonApiId;
import io.crnk.core.resource.annotations.JsonApiResource;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuppressFBWarnings({ "EI_EXPOSE_REP", "EI_EXPOSE_REP2" })
@RelatedEntity(ObjectUpload.class)
@Getter
@JsonApiResource(type = ObjectUploadDto.TYPENAME)
@SuperBuilder
public class ObjectUploadDto extends ResourceMetaInfo{

  public static final String TYPENAME = "object-upload";
  
  @JsonApiId
  private UUID fileIdentifier;

  private String createdBy;
  private OffsetDateTime createdOn;

  private String originalFilename;
  private String sha1Hex;
  private String receivedMediaType;
  private String detectedMediaType;
  private String detectedFileExtension;
  private String evaluatedMediaType;
  private String evaluatedFileExtension;
  private long sizeInBytes;
  private UUID thumbnailIdentifier;
  private String bucket;

}
