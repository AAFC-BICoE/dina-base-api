package ca.gc.aafc.dina.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ResourceMetaInfo {
    public static final String WARNINGS = "warnings";
    protected Map<String, List<Warning>> meta = new HashMap<String, List<Warning>>();

    public ResourceMetaInfo() {
        meta.put(WARNINGS, new ArrayList<Warning>());
    }

    @Getter
    @SuppressFBWarnings("UUF_UNUSED_FIELD")
    @SuperBuilder
	public static
    class Warning {
        private String key;
        private String message;
    }
}
