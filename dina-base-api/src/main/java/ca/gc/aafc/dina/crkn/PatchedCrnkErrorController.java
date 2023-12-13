package ca.gc.aafc.dina.crkn;

import io.crnk.core.engine.document.Document;
import io.crnk.core.engine.document.ErrorData;
import io.crnk.core.engine.document.ErrorDataBuilder;
import io.crnk.core.engine.http.HttpHeaders;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * CrnkErrorController but patched for SpringBoot >= 2.5
 * Source: https://github.com/crnk-project/crnk-framework/commit/0ed1721159943f6ffc5260ac502252efbbcc39c8
 *
 * We can't upgrade Crnk at this point since it creates other (more complex) issues and the project is not maintained anymore.
 *
 */
public class PatchedCrnkErrorController extends BasicErrorController {

  public PatchedCrnkErrorController(ErrorAttributes errorAttributes,
                             ErrorProperties errorProperties) {
    super(errorAttributes, errorProperties);
  }

  public PatchedCrnkErrorController(ErrorAttributes errorAttributes,
                             ErrorProperties errorProperties,
                             List<ErrorViewResolver> errorViewResolvers) {
    super(errorAttributes, errorProperties, errorViewResolvers);
  }

  // TODO for whatever reason this is not called directly
  @RequestMapping(produces = HttpHeaders.JSONAPI_CONTENT_TYPE)
  @ResponseBody
  public ResponseEntity<Document> errorToJsonApi(HttpServletRequest request) {
    Map<String, Object> body = getErrorAttributes(request,
      getErrorAttributeOptions(request, MediaType.ALL));
    HttpStatus status = getStatus(request);

    ErrorDataBuilder errorDataBuilder = ErrorData.builder();
    for (Map.Entry<String, Object> attribute : body.entrySet()) {
      if (attribute.getKey().equals("status")) {
        errorDataBuilder.setStatus(attribute.getValue().toString());
      } else if (attribute.getKey().equals("error")) {
        errorDataBuilder.setTitle(attribute.getValue().toString());
      } else if (attribute.getKey().equals("message")) {
        errorDataBuilder.setDetail(attribute.getValue().toString());
      } else {
        errorDataBuilder.addMetaField(attribute.getKey(), attribute.getValue());
      }
    }
    Document document = new Document();
    document.setErrors(Arrays.asList(errorDataBuilder.build()));
    return new ResponseEntity<>(document, status);
  }


  @RequestMapping
  @ResponseBody
  public ResponseEntity error(HttpServletRequest request) {
    return errorToJsonApi(request);
  }
}
