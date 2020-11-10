package ca.gc.aafc.dina.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;

@Getter
public class BaseDto {
    protected Map<String, List<Warning>> meta = new HashMap<String, List<Warning>>();
    protected String WARNINGS = "warnings";

    public BaseDto() {
        meta.put(WARNINGS, new ArrayList<Warning>());
    }

    @Data
    class Warning {
        String key;
        String message;
    }
}
