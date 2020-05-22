package ca.gc.aafc.dina.testsupport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.testsupport.entity.ComplexObject;
import lombok.Data;

public class JsonAPITestHelperTest {
  
  @Data   
  static class TestObject  extends ComplexObject{
    private String email;
    private String displayName;
    private LocalDateTime createDate;
  };    

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

    Map<String, Object> relationshipMap = new HashMap<String, Object>();
    relationshipMap.put("uploadedBy", myTestObj);

    Map<String, Object> jsonAPIMap = JsonAPITestHelper.toJsonAPIMap("metadata", attributeMap, 
        relationshipMap, "30ef7300-baf4-4ab0-b3e0-7f841c3d211e");
    assertTrue(jsonAPIMap.containsKey("data"));
    
    Map<String, Object> dataMap = (Map<String, Object>) jsonAPIMap.get("data");
    
    assertTrue(dataMap.containsKey("type"));
    assertTrue(dataMap.containsKey("id"));
    assertTrue(dataMap.containsKey("attributes"));
    assertTrue(dataMap.containsKey("relationships"));
    
    assertTrue(dataMap.get("id").equals("30ef7300-baf4-4ab0-b3e0-7f841c3d211e"));
    assertTrue(dataMap.get("type").equals("metadata"));
    
    Map<String, Object> attributesMap = (Map<String, Object>) dataMap.get("attributes");    
    
    
    assertTrue(attributesMap.containsKey("bucket"));
    assertTrue(attributesMap.containsKey("dcFormat"));
    assertTrue((attributesMap.get("bucket").equals("myBucket")));
    assertTrue((attributesMap.get("dcFormat").equals("image")));
    
    Map<String, Object> relationshipsMap = (Map<String, Object>) dataMap.get("relationships");    
    
    assertTrue(relationshipsMap.get("uploadedBy").equals(myTestObj));    
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void toRelationshipMap_whenGivenRelationshipObjectList_thenReturnProperRelationshipMap() {
    List<JsonAPIRelationship> relationshipList = new ArrayList<JsonAPIRelationship>();
    relationshipList.add(RELATIONSHIP);  

    Map<String, Object> relationshipMap = JsonAPITestHelper.toRelationshipMap(relationshipList);
    assertTrue(relationshipMap.containsKey("derivedFrom"));
    
    Map<String, Object> relationship = (Map<String, Object>) relationshipMap.get("derivedFrom");
    assertNotNull(relationship);
    
    Map<String, Object> relationshipData = (Map<String, Object>) relationship.get("data");
    assertEquals( relationshipData.get("id"), "947f77ee-d144-45b5-b559-e239db0caa18");
    assertEquals( relationshipData.get("type"), "metadata");
    
  }  
  

}
