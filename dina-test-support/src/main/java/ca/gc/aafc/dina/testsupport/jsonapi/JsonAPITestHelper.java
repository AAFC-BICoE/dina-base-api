package ca.gc.aafc.dina.testsupport.jsonapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.restassured.response.ValidatableResponse;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The class provides some helper methods to build JSON API compliant Map
 * that can be serialized by Jackson to send to a running api for testing.
 *
 */
public final class JsonAPITestHelper {

  private static final ObjectMapper IT_OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> IT_OM_TYPE_REF = new TypeReference<>() { };

  static {
    IT_OBJECT_MAPPER.registerModule(new JavaTimeModule());
    IT_OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    IT_OBJECT_MAPPER.setSerializationInclusion(Include.NON_NULL);
  }

  private  JsonAPITestHelper() {

  }

  /**
   * Package protected utility method to write the provided object as json string using the defined ObjectMapper.
   *
   * @param obj
   * @return
   */
  static String toString(Object obj) throws JsonProcessingException {
    return IT_OBJECT_MAPPER.writeValueAsString(obj);
  }

  /**
   * Create an attribute map for the provided object. Attributes with nulls will be skipped.
   *
   * @param obj
   * @return attribute map for the provided object
   */

  public static Map<String, Object> toAttributeMap(Object obj) {
    return IT_OBJECT_MAPPER.convertValue(obj, IT_OM_TYPE_REF);
  }

  /**
   * Creates a JSON API Map from the provided type name and object.
   * No id will be set.
   * @param typeName
   * @param obj
   * @return
   */
  public static Map<String, Object> toJsonAPIMap(String typeName, Object obj) {
    return toJsonAPIMap(typeName, toAttributeMap(obj), null, null);
  }

  /**
   * Creates a JSON API Map from the provided type name, object and id.
   * @param typeName
   * @param id
   * @param obj
   * @return
   */
  public static Map<String, Object> toJsonAPIMap(String typeName,
      String id, Object obj) {
    return toJsonAPIMap(typeName, toAttributeMap(obj), null, id);
  }

  /**
   * Creates a JSON:API Map from the provided type name, attributes.
   *
   * @param typeName
   *          "type" in JSON API
   * @param attributeMap
   *          key/value representing the "attributes" in JSON API
   * @return
   */
  public static Map<String, Object> toJsonAPIMap(String typeName,
      Map<String, Object> attributeMap) {
    return toJsonAPIMap(typeName, attributeMap, null, null);
  }

  /**
   * Creates a JSON API Map from the provided type name, attributes and id.
   *
   * @param typeName
   *          "type" in JSON API
   * @param attributeMap
   *          key/value representing the "attributes" in JSON API
   * @param id
   *          id of the resource or null if there is none
   * @return
   */
  public static Map<String, Object> toJsonAPIMap(String typeName,
      Map<String, Object> attributeMap, String id) {
    return toJsonAPIMap(typeName, attributeMap, null, id);
  }

  /**
   * Creates a JSON API Map from the provided type name, attributes and id.
   *
   * @param typeName
   *          "type" in JSON API
   * @param attributeMap
   *          key/value representing the "attributes" in JSON:API
   * @param relationshipMap
   *          key/value representing the "relationships" in JSON:API
   * @param id
   *          id of the resource or null if there is none
   * @return
   */
  public static Map<String, Object> toJsonAPIMap(String typeName,
      Map<String, Object> attributeMap, Map<String, ?> relationshipMap, String id) {
    Map<String, Object> jsonApiMap = new HashMap<>();
    jsonApiMap.put("type", typeName);
    if (id != null) {
      jsonApiMap.put("id", id);
    }

    jsonApiMap.put("attributes", attributeMap);
    if (relationshipMap != null) {
      jsonApiMap.put("relationships", relationshipMap);
    }
    return Map.of("data", jsonApiMap);
  }

  /**
   * Used to update multiple To-One relationships.
   * @param relationship
   * @return
   */
  public static Map<String, Object> toRelationshipMap(List<JsonAPIRelationship> relationship) {
    if (relationship == null) {
      return null;
    }

    Map<String, Object> relationships = new HashMap<>();
    for (JsonAPIRelationship rel : relationship) {
      relationships.putAll(toRelationshipMap(rel));
    }
    return relationships;
  }

  /**
   * Used to update To-One relationships.
   * @param relationship
   * @return
   */
  public static Map<String, Object> toRelationshipMap(JsonAPIRelationship relationship) {
    return Map.of(
      relationship.getName(),
      Map.of(
        "data", Map.of(
          "type", relationship.getType(),
          "id", relationship.getId()
        )
      )
    );
  }

  /**
   * Used to update To-Many relationships.
   * Will produce one "data" block per relationship name.
   * @param relationships
   * @return
   */
  public static Map<String, ?> toRelationshipMapByName(List<JsonAPIRelationship> relationships) {
    Map<String, Pair<String, List<Map<String, Object>>>> relationshipByName = new HashMap<>();
    for(JsonAPIRelationship currRel : relationships) {
      Pair<String, List<Map<String, Object>>> p = relationshipByName.computeIfAbsent(currRel.getName(), k -> Pair.of("data", new ArrayList<>()));
      p.getRight().add(Map.of(
              "type", currRel.getType(),
              "id", currRel.getId()
      ));
    }
    return relationshipByName;
  }

  /**
   * Extract the id field as per JSON API standard
   * @param response
   * @return
   */
  public static String extractId(ValidatableResponse response) {
    return response.extract()
        .body()
        .jsonPath()
        .get("data.id");
  }

  /**
   * Convenience method to generate a Map representation of a External Relation as a List.
   * External relationships here means that the identifier (UUID) will be randomly generated.
   *
   * @param type         type of the external relation
   * @param elementCount number of relations to generate
   * @return a Map representation of a External Relation as a List
   */
  public static Map<String, Object> generateExternalRelationList(String type, int elementCount) {
    List<Map<String, String>> list = new ArrayList<>();
    for (int i = 0; i < elementCount; i++) {
      list.add(newExternalType(type));
    }
    return Map.of("data", list);
  }

  /**
   * Convenience method to generate a Map representation of a single External Relation.
   * External relationship here means that the identifier (UUID) will be randomly generated.
   *
   * @param type type of the external relation
   * @return a Map representation of a External Relation
   */
  public static Map<String, Object> generateExternalRelation(String type) {
    return Map.of("data", newExternalType(type));
  }

  private static Map<String, String> newExternalType(String type) {
    return Map.of("id", UUID.randomUUID().toString(), "type", type);
  }

}
