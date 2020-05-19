package ca.gc.aafc.dina.testsupport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ca.gc.aafc.dina.testsupport.entity.ComplexObject;
import lombok.Data;
import lombok.Getter;

public class JsonAPITestHelperTest {
  
  @Data   
  class TestObject  extends ComplexObject{
    private String email;
    private String displayName;
  };    
  
  private TestObject myTestObj = new TestObject();
  private Map<String, Object> attributeMap = new HashMap<String, Object>();
  private Map<String, Object> relationshipMap = new HashMap<String, Object>();
  private List<Relationship> relationshipList = new ArrayList<Relationship>();
  private Relationship relationship = Relationship.of("derivedFrom", "metadata", 
      "947f77ee-d144-45b5-b559-e239db0caa18");
  
    
  @BeforeEach
  void setup() {
    
    myTestObj.setDisplayName("agent");
    myTestObj.setEmail("xuemei.gan@canada.ca");
    
    attributeMap.put("bucket","myBucket");
    attributeMap.put("dc_format","image");
    
    relationshipMap.put("uploadedBy", myTestObj);
    
    relationshipList.add(relationship);    
    
  }
  
  
  @Test
  public void toAttributeMap_whenGivenAnObject_thenReturnProperAttributeMap() {
    
    Map<String, Object> attrMap = JsonAPITestHelper.toAttributeMap(myTestObj);
    assertTrue(attrMap.keySet().contains("displayName"));
    assertTrue(attrMap.keySet().contains("email"));
    
    assertTrue(attrMap.values().contains("agent"));
    assertTrue(attrMap.values().contains("xuemei.gan@canada.ca"));    
    
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void toJsonAPIMap_whenGivenAllParameters_thenReturnProperJsonMap() {

    Map<String, Object> jsonAPIMap = JsonAPITestHelper.toJsonAPIMap("metadata", attributeMap, 
        relationshipMap, "30ef7300-baf4-4ab0-b3e0-7f841c3d211e");
    System.out.println("jsonAPIMap " + jsonAPIMap.toString());
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
    assertTrue(attributesMap.containsKey("dc_format"));
    assertTrue((attributesMap.get("bucket").equals("myBucket")));
    assertTrue((attributesMap.get("dc_format").equals("image")));
    
    Map<String, Object> relationshipsMap = (Map<String, Object>) dataMap.get("relationships");    
    
    assertTrue(relationshipsMap.get("uploadedBy").equals(myTestObj));    
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void toRelationshipMap_whenGivenRelationshipObjectList_thenReturnProperRelationshipMap() {
   
    Map<String, Object> relationshipMap = JsonAPITestHelper.toRelationshipMap(relationshipList);
    assertTrue(relationshipMap.containsKey("derivedFrom"));
    
    Map<String, Object> relationship = (Map<String, Object>) relationshipMap.get("derivedFrom");
    assertNotNull(relationship);
    
    Map<String, Object> relationshipData = (Map<String, Object>) relationship.get("data");
    assertEquals( relationshipData.get("id"), "947f77ee-d144-45b5-b559-e239db0caa18");
    assertEquals( relationshipData.get("type"), "metadata");
    
  }  
  

}
