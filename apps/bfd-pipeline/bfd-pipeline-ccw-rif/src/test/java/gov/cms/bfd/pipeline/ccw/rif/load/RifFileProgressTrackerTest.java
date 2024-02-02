package gov.cms.bfd.pipeline.ccw.rif.load;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.model.rif.RifFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link RifFileProgressTracker}. */
@ExtendWith(MockitoExtension.class)
class RifFileProgressTrackerTest {
  /** Mock for the file being tracked and updated. */
  @Mock private RifFile rifFile;

  /** Track some record number activity and verify that update is called only when necessary. */
  @Test
  void updateShouldOnlyHappenWhenThereIsAChange() {
    doReturn(1L).when(rifFile).getLastRecordNumber();
    RifFileProgressTracker tracker = new RifFileProgressTracker(rifFile);
    tracker.recordActive(2L);
    tracker.recordActive(3L);
    tracker.recordActive(4L);

    // all changes are in flight now so no update is needed
    tracker.writeProgress();
    verify(rifFile, times(0)).updateLastRecordNumber(anyLong());

    // completed 3 but 2 still in flight so still no update needed
    tracker.recordComplete(3L);
    tracker.writeProgress();
    verify(rifFile, times(0)).updateLastRecordNumber(anyLong());

    // completed 2 so now we need to update to 3
    tracker.recordComplete(2L);
    tracker.writeProgress();
    verify(rifFile, times(1)).updateLastRecordNumber(3L);
  }
}
