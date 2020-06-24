package ca.gc.aafc.dina.entity;

import java.time.OffsetDateTime;

/**
 * Represents a DINA entity with an id field.
 * The id is usually the Primary Key and auto-generated.
 */
public interface DinaEntity {

    Integer getId();

    String getCreatedBy();

    OffsetDateTime getCreatedOn();

}
