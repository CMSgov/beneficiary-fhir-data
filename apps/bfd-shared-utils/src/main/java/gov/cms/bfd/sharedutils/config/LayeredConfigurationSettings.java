package gov.cms.bfd.sharedutils.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import lombok.Builder;
import lombok.Getter;

@Immutable
@Getter
public class LayeredConfigurationSettings {
    private final String propertiesFile;
    private final List<String> ssmPaths;
    private final List<String> ssmHierarchies;

    @Builder
    public LayeredConfigurationSettings(@JsonProperty("propertiesFile") String propertiesFile,
                                        @JsonProperty("ssmPaths") List<String> ssmPaths,
                                        @JsonProperty("ssmHierarchies") List<String> ssmHierarchies)
    {
        this.propertiesFile = Strings.nullToEmpty(propertiesFile);
        this.ssmPaths = ssmHierarchies == null ? List.of() : List.copyOf(ssmPaths);
        this.ssmHierarchies = ssmHierarchies == null ? List.of() : List.copyOf(ssmHierarchies);
    }

    public boolean hasPropertiesFile() {
        return !propertiesFile.isEmpty();
    }

    public boolean hasSsmPaths() {
        return !ssmPaths.isEmpty();
    }

    public boolean hasSsmHierarchies() {
        return !ssmHierarchies.isEmpty();
    }
}
