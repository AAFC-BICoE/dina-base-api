package ca.gc.aafc.dina.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.Getter;

@Getter
public class BaseDto {
    protected Map<String, List<Warning>> meta = new HashMap<String, List<Warning>>();
    protected static final String WARNINGS = "warnings";

    public BaseDto() {
        meta.put(WARNINGS, new ArrayList<Warning>());
    }

    @Data
    @SuppressFBWarnings("UUF_UNUSED_FIELD")
    static class Warning {
        private String key;
        private String message;
    }
}
