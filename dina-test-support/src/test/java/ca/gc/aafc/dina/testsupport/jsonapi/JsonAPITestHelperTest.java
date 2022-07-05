package ca.gc.aafc.dina.testsupport.jsonapi;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.gc.aafc.dina.testsupport.entity.ComplexObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.path.json.JsonPath;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JsonAPITestHelperTest {
  
  @Data   
  static class TestObject extends ComplexObject{
    private String email;
    private String displayName;
    private LocalDateTime createDate;
  }

  private final static JsonAPIRelationship RELATIONSHIP = JsonAPIRelationship.of("derivedFrom", "metadata", 
      "947f77ee-d144-45b5-b559-e239db0caa18");

  private final static LocalDateTime TEST_LOCAL_DT = LocalDateTime.of(2019, 1, 26, 6, 26);  
 
  private static TestObject createTestObject() {
    TestObject myTestObj = new TestObject();
    myTestObj.setDisplayName("agent");
    myTestObj.setEmail("a@a.ca");
    myTestObj.setCreateDate(TEST_LOCAL_DT);
    return myTestObj;
  }
  
  
  @Test
  public void toAttributeMap_whenGivenAnObject_thenReturnProperAttributeMap() {

    Map<String, Object> attrMap = JsonAPITestHelper.toAttributeMap(createTestObject());
    assertTrue(attrMap.keySet().contains("displayName"));
    assertTrue(attrMap.keySet().contains("email"));
    assertTrue(attrMap.keySet().contains("createDate"));
    
    assertTrue(attrMap.values().contains("agent"));
    assertTrue(attrMap.values().contains("a@a.ca"));

    assertEquals(LocalDateTime.parse((CharSequence) attrMap.get("createDate")), TEST_LOCAL_DT);
    
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void toJsonAPIMap_whenGivenAllParameters_thenReturnProperJsonMap() {

    Map<String, Object> attributeMap = new HashMap<String, Object>();
    attributeMap.put("bucket","myBucket");
    attributeMap.put("dcFormat","image");

    TestObject myTestObj = createTestObject();

    Map<String, Object> relationshipMap = new HashMap<>();
    relationshipMap.put("uploadedBy", myTestObj);

    Map<String, Object> jsonAPIMap = JsonAPITestHelper.toJsonAPIMap("metadata", attributeMap, 
        relationshipMap, "30ef7300-baf4-4ab0-b3e0-7f841c3d211e");
    assertTrue(jsonAPIMap.containsKey("data"));
    
    Map<String, Object> dataMap = (Map<String, Object>) jsonAPIMap.get("data");
    
    assertTrue(dataMap.containsKey("type"));
    assertTrue(dataMap.containsKey("id"));
    assertTrue(dataMap.containsKey("attributes"));
    assertTrue(dataMap.containsKey("relationships"));
    
    assertEquals("30ef7300-baf4-4ab0-b3e0-7f841c3d211e", dataMap.get("id"));
    assertEquals("metadata", dataMap.get("type"));
    
    Map<String, Object> attributesMap = (Map<String, Object>) dataMap.get("attributes");    
    
    
    assertTrue(attributesMap.containsKey("bucket"));
    assertTrue(attributesMap.containsKey("dcFormat"));
    assertEquals("myBucket", attributesMap.get("bucket"));
    assertEquals("image", attributesMap.get("dcFormat"));
    
    Map<String, Object> relationshipsMap = (Map<String, Object>) dataMap.get("relationships");

    assertEquals(myTestObj, relationshipsMap.get("uploadedBy"));
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void toRelationshipMap_whenGivenRelationshipObjectList_thenReturnProperRelationshipMap() {
    List<JsonAPIRelationship> relationshipList = new ArrayList<>();
    relationshipList.add(RELATIONSHIP);  

    Map<String, Object> relationshipMap = JsonAPITestHelper.toRelationshipMap(relationshipList);
    assertTrue(relationshipMap.containsKey("derivedFrom"));
    
    Map<String, Object> relationship = (Map<String, Object>) relationshipMap.get("derivedFrom");
    assertNotNull(relationship);
    
    Map<String, Object> relationshipData = (Map<String, Object>) relationship.get("data");
    assertEquals( relationshipData.get("id"), "947f77ee-d144-45b5-b559-e239db0caa18");
    assertEquals( relationshipData.get("type"), "metadata");
    
  }
  @SuppressWarnings("unchecked")
  @Test
  public void toJsonAPIMap_whenGivenAnObject_thenReturnJsonApiMap() {

    TestObject myTestObj = createTestObject();

    Map<String, Object> jsonMap = JsonAPITestHelper.toJsonAPIMap("test12", myTestObj);
    Map<String, Object> dataMap = (Map<String, Object>) jsonMap.get("data");
    Map<String, Object> attributesMap = (Map<String, Object>)dataMap.get("attributes");

    assertTrue(attributesMap.containsKey("email"));
  }

  @Test
  public void testToRelationshipByName() throws JsonProcessingException {
    List<JsonAPIRelationship> rels = List.of(
            JsonAPIRelationship.of("organisms", "organism",
            "947f77ee-d144-45b5-b559-e239db0caa18"),
            JsonAPIRelationship.of("organisms", "organism",
                    "947f77ee-d144-45b5-b559-e239db0caa18"));

    JsonPath expected = new JsonPath("{\"organisms\":{\"data\":[{\"type\":\"organism\",\"id\":\"947f77ee-d144-45b5-b559-e239db0caa18\"},{\"type\":\"organism\",\"id\":\"947f77ee-d144-45b5-b559-e239db0caa18\"}]}}");
    JsonPath result = new JsonPath(JsonAPITestHelper.toString(JsonAPITestHelper.toRelationshipMapByName(rels)));
    // compare maps from the root
    assertEquals(expected.getMap("."), result.getMap("."));
  }

}
