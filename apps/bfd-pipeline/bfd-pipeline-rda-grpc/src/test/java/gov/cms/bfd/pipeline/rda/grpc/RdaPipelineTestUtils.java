package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.Meter;

public class RdaPipelineTestUtils {
  public static void assertMeterReading(long expected, String meterName, Meter meter) {
    assertEquals("Meter " + meterName, expected, meter.getCount());
  }
}
