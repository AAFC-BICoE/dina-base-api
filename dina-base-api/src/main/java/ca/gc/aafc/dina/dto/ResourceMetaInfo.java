package ca.gc.aafc.dina.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.gc.aafc.dina.mapper.DerivedDtoField;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class ResourceMetaInfo {
    public static final String WARNINGS = "warnings";

    @DerivedDtoField
    protected Map<String, List<Warning>> metaInfo = new HashMap<String, List<Warning>>();

    public ResourceMetaInfo() {
        metaInfo.put(WARNINGS, new ArrayList<Warning>());
    }

    @Getter
    @Setter
    @SuppressFBWarnings("UUF_UNUSED_FIELD")
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
	public static
    class Warning {
        private String key;
        private String message;
    }
}
