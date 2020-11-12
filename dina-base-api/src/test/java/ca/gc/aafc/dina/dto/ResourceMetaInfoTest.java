package ca.gc.aafc.dina.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

public class ResourceMetaInfoTest {

    private static String WARNINGS = "warnings";

    ResourceMetaInfo inheritedDtoObject = new ResourceMetaInfo (){};    

    @Test
    public void when_inherited_childrenDtos_hasAll_expected_sections(){
        assertNotNull(inheritedDtoObject.getMeta());
        assertEquals(WARNINGS, ResourceMetaInfo.WARNINGS);
        assertNotNull(inheritedDtoObject.getMeta().get(WARNINGS));
    }
}
