package ca.gc.aafc.dina.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;

public class BaseDtoTest {

    private static String WARNINGS = "warnings";

    BaseDto inheritedDtoObject = new BaseDto (){};    

    @Test
    public void when_inherited_childrenDtos_hasAll_expected_sections(){
        assertNotNull(inheritedDtoObject.getMeta());
        assertEquals(WARNINGS, BaseDto.WARNINGS);
        assertNotNull(inheritedDtoObject.getMeta().get(WARNINGS));
    }
}
