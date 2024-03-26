package ca.gc.aafc.dina.service;

import java.util.UUID;
import lombok.Builder;

/**
 * Representing a Pair of Name/UUID
 * @param name
 * @param uuid
 */
@Builder
public record NameUUIDPair(String name, UUID uuid) {
}
