package gov.cms.bfd.pipeline.ccw.rif;

import lombok.Builder;
import lombok.Data;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

@Data
@Builder
public class CcwRifLoadJobStatusEvent {
    private final JobStage jobStage;
    @Nullable
    private final String lastCompletedManifestKey;
    @Nullable
    private final ZonedDateTime lastCompletedTimestamp;
    @Nullable
    private final String currentManifestKey;
    @Nullable
    private final ZonedDateTime currentTimestamp;

    public enum JobStage {
    CheckingBucketForManifest,
    AwaitingManifestDataFiles,
    ProcessingManifestDataFiles,
    CompletedManifest,
    Idle
}
}
