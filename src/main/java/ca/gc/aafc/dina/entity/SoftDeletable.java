package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;

/**
 * Interface representing an entity with soft-delete support.
 *
 */
public interface SoftDeletable {
  
  /**
   * Standard name of the field.
   */
  String DELETED_DATE_FIELD_NAME = "deletedDate";

  OffsetDateTime getDeletedDate();

  void setDeletedDate(OffsetDateTime deletedDate);

}
