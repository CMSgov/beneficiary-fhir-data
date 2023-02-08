package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests the functionality of the {@link OffsetLinkBuilder}. */
public class OffsetLinkBuilderTest {

  /**
   * Tests that when the page size is set to a non number, an {@link InvalidRequestException} is
   * thrown. This ensures the caller gets a 400 error with an explanation for the error.
   */
  @Test
  public void testNonNumberPageSizeExpectException() {

    ServletRequestDetails requestDetails = mock(ServletRequestDetails.class);
    Map<String, String[]> params = new HashMap<>();
    String[] value = {"abc"};
    params.put(Constants.PARAM_COUNT, value);
    when(requestDetails.getParameters()).thenReturn(params);

    assertThrows(
        InvalidRequestException.class,
        () -> {
          new OffsetLinkBuilder(requestDetails, "");
        },
        "Invalid argument in request URL: " + Constants.PARAM_COUNT + " must be a number.");
  }

  /**
   * Tests that when the start index is set to a non number, an {@link InvalidRequestException} is
   * thrown. This ensures the caller gets a 400 error with an explanation for the error.
   */
  @Test
  public void testNonNumberStartIndexExpectException() {

    ServletRequestDetails requestDetails = mock(ServletRequestDetails.class);
    Map<String, String[]> params = new HashMap<>();
    String[] value = {"abc"};
    params.put("startIndex", value);
    when(requestDetails.getParameters()).thenReturn(params);

    assertThrows(
        InvalidRequestException.class,
        () -> {
          new OffsetLinkBuilder(requestDetails, "");
        },
        "Invalid argument in request URL: startIndex must be a number.");
  }

  /**
   * Tests that when the start index is set to a negative number, an {@link InvalidRequestException}
   * is thrown when it is accessed. This ensures the caller gets a 400 error with an explanation for
   * the error.
   */
  @Test
  public void testNegativeStartIndexExpectException() {

    ServletRequestDetails requestDetails = mock(ServletRequestDetails.class);
    Map<String, String[]> params = new HashMap<>();
    String[] value = {"-1"};
    params.put("startIndex", value);
    when(requestDetails.getParameters()).thenReturn(params);
    OffsetLinkBuilder linkBuilder = new OffsetLinkBuilder(requestDetails, "");

    assertThrows(
        InvalidRequestException.class,
        () -> {
          linkBuilder.getStartIndex();
        },
        "Value for startIndex cannot be negative: -1");
  }

  /**
   * Tests that when the page size is set to a negative number, an {@link InvalidRequestException}
   * is thrown when it is accessed. This ensures the caller gets a 400 error with an explanation for
   * the error.
   */
  @Test
  public void testNegativePageSizeExpectException() {

    ServletRequestDetails requestDetails = mock(ServletRequestDetails.class);
    Map<String, String[]> params = new HashMap<>();
    String[] value = {"-1"};
    params.put(Constants.PARAM_COUNT, value);
    when(requestDetails.getParameters()).thenReturn(params);
    OffsetLinkBuilder linkBuilder = new OffsetLinkBuilder(requestDetails, "");

    assertThrows(
        InvalidRequestException.class,
        () -> {
          linkBuilder.getPageSize();
        },
        "Value for pageSize cannot be negative: -1");
  }

  /**
   * Ensures that when valid details are passed to a {@link OffsetLinkBuilder} no errors are raised.
   */
  @Test
  public void testValidOffsetLinkBuilderExpectNoException() {

    ServletRequestDetails requestDetails = mock(ServletRequestDetails.class);
    when(requestDetails.getServerBaseForRequest()).thenReturn("baseValue");
    Map<String, String[]> params = new HashMap<>();
    String[] pageValue = {"2"};
    String[] startIndexValue = {"2"};
    params.put("startIndex", startIndexValue);
    params.put(Constants.PARAM_COUNT, pageValue);
    when(requestDetails.getParameters()).thenReturn(params);
    String resource = "baseUri";

    OffsetLinkBuilder offsetLinkBuilder = new OffsetLinkBuilder(requestDetails, resource);

    // In addition to ensuring no exceptions, check the getters dont throw exceptions
    assertEquals(Integer.parseInt(pageValue[0]), offsetLinkBuilder.getPageSize());
    assertEquals(Integer.parseInt(pageValue[0]), offsetLinkBuilder.getStartIndex());
  }
}
