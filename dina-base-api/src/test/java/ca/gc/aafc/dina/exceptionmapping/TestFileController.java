package ca.gc.aafc.dina.exceptionmapping;

import javax.inject.Inject;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TestFileController {
  
  @Inject
  private MessageSource messageSource;

  @GetMapping(value = "/file/{bucket}/{uuid}")
  public void downloadFile(@PathVariable String bucket, @PathVariable String uuid) {
    String errorMsg = messageSource.getMessage("minio.file_or_bucket_not_found",
        new Object[] { uuid, bucket }, LocaleContextHolder.getLocale());
    throw  new ResponseStatusException(HttpStatus.NOT_FOUND, errorMsg, null); 

    }
}
