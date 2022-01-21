package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class S3JsonMessageSourceTest {
  private static final String MCS_CLAIMS_JSON =
      "{\"seq\":\"1\",\"changeType\":\"CHANGE_TYPE_UPDATE\",\"claim\":{\"idrClmHdIcn\":\"101\"}}\n"
          + "{\"seq\":\"2\",\"changeType\":\"CHANGE_TYPE_INSERT\",\"claim\":{\"idrClmHdIcn\":\"102\"}}\n";

  private S3Object s3Object;
  private S3ObjectInputStream inputStream;
  private S3JsonMessageSource<McsClaimChange> source;

  @BeforeEach
  public void setUp() throws Exception {
    inputStream = createInputStream();
    s3Object = createObject("some/path/to/key/MCS_DATA.ndjson");
    source = new S3JsonMessageSource<>(s3Object, JsonMessageSource::parseMcsClaimChange);
  }

  @Test
  public void messagesParsedAndReturnedCorrectly() throws Exception {
    List<Long> sequences = new ArrayList<>();
    while (source.hasNext()) {
      sequences.add(source.next().getSeq());
    }
    assertEquals(Arrays.asList(1L, 2L), sequences);
  }

  @Test
  public void abortCalledIfMessagesRemain() throws Exception {
    assertTrue(source.hasNext());
    source.close();
    verify(inputStream).abort();
    verify(inputStream).close();
    verify(s3Object).close();
  }

  @Test
  public void abortNotCalledIfNoMessagesRemain() throws Exception {
    while (source.hasNext()) {
      source.next();
    }
    source.close();
    verify(inputStream, times(0)).abort();
    verify(inputStream).close();
    verify(s3Object).close();
  }

  @Test
  public void allResourcesClosedEvenIfThrowing() throws Exception {
    doThrow(new IOException("stream-message")).when(inputStream).close();
    doThrow(new RuntimeException("abort-message")).when(inputStream).abort();
    doThrow(new IOException("object-message")).when(s3Object).close();
    try {
      source.close();
      fail("should have thrown");
    } catch (Exception ex) {
      List<String> messages =
          Stream.concat(Stream.of(ex), Stream.of(ex.getSuppressed()))
              .map(Throwable::getMessage)
              .sorted()
              .collect(Collectors.toList());
      assertEquals(Arrays.asList("abort-message", "object-message", "stream-message"), messages);
    }
  }

  @Test
  public void uncompressesGzipData() throws Exception {
    // replaces the standard test data with compressed data version
    var bytes = new ByteArrayOutputStream();
    try (PrintWriter out = new PrintWriter(new GZIPOutputStream(bytes))) {
      out.write(MCS_CLAIMS_JSON);
    }
    inputStream = createInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    s3Object = createObject("some/path/to/key/MCS_DATA.ndjson.gz");
    source = new S3JsonMessageSource<>(s3Object, JsonMessageSource::parseMcsClaimChange);

    // now just verify the data is loaded correctly
    messagesParsedAndReturnedCorrectly();
  }

  private S3Object createObject(String objectKey) {
    S3Object object = mock(S3Object.class);
    doAnswer(i -> inputStream).when(object).getObjectContent();
    doReturn(objectKey).when(object).getKey();
    return object;
  }

  private S3ObjectInputStream createInputStream() throws Exception {
    return createInputStream(
        new ByteArrayInputStream(MCS_CLAIMS_JSON.getBytes(StandardCharsets.UTF_8)));
  }

  private S3ObjectInputStream createInputStream(ByteArrayInputStream input) throws Exception {
    HttpRequestBase request = mock(HttpRequestBase.class);
    // using a spy here because we want the stream functionality of a real S3ObjectInputStream
    S3ObjectInputStream stream = spy(new S3ObjectInputStream(input, request));
    // make sure we don't accidentally trigger some unwanted calls within the stream during test
    doNothing().when(stream).abort();
    doNothing().when(stream).close();
    return stream;
  }
}
