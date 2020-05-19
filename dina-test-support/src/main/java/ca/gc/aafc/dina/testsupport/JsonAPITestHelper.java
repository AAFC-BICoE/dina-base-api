package ca.gc.aafc.dina.testsupport;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;


public final class JsonAPITestHelper {

  private static final ObjectMapper IT_OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, Object>> IT_OM_TYPE_REF = new TypeReference<Map<String, Object>>() {};
  
  private  JsonAPITestHelper() {
    
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
      Map<String, Object> attributeMap, Map<String, Object> relationshipMap, String id) {
    ImmutableMap.Builder<String, Object> bldr = new ImmutableMap.Builder<>();
    bldr.put("type", typeName);
    if (id != null) {
      bldr.put("id", id);
    }

    bldr.put("attributes", attributeMap);
    if(relationshipMap != null) {
      bldr.put("relationships", relationshipMap);
    }
    return ImmutableMap.of("data", bldr.build());
  }
  
  protected static Map<String, Object> toRelationshipMap(List<Relationship> relationship) {
    if( relationship == null) {
      return null;
    }
    
    ImmutableMap.Builder<String, Object> relationships = new ImmutableMap.Builder<>();
    for (Relationship rel : relationship) {
      relationships.putAll(toRelationshipMap(rel));
    }
    return relationships.build();
  }
    
  private static Map<String, Object> toRelationshipMap(Relationship relationship) {
    ImmutableMap.Builder<String, Object> relationships = new ImmutableMap.Builder<>();
    relationships.put("type", relationship.getType()).put("id", relationship.getId()).build();
    
    ImmutableMap.Builder<String, Object> bldr = new ImmutableMap.Builder<>();
    bldr.put("data", relationships.build());
    
    ImmutableMap.Builder<String, Object> relBuilder = new ImmutableMap.Builder<>();
    relBuilder.put(relationship.getName(), bldr.build());
    
    return relBuilder.build();
  }  
  
}
