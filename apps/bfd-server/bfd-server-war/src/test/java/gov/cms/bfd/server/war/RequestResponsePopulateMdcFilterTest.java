package gov.cms.bfd.server.war;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.spi.MDCAdapter;

/** Unit tests for {@link BfdMDC}. */
@ExtendWith(MockitoExtension.class)
public final class RequestResponsePopulateMdcFilterTest {
  /** A mocked MDC adapter for use in the tests. */
  @Mock MDCAdapter mdcMock;

  /** A mocked servlet request for use in the tests. */
  @Mock HttpServletRequest servletRequest;

  /** A mocked servlet response for use in the tests. */
  @Mock HttpServletResponse servletResponse;

  /** A mocked filter chain for use in the tests. */
  @Mock FilterChain filterChain;

  /** The previous MDC adapter. */
  private static MDCAdapter previousAdapter;

  /** Set up tests with a mocked MDC adapter and make sure the MDC context is clear. */
  @BeforeEach
  public void beforeEach() {
    mdcMock.clear();
    BfdMDC.setMDCAdapter(mdcMock);
    Mockito.reset(mdcMock);
  }

  /** Gets the previous MDC adapter. */
  @BeforeAll
  public static void beforeAll() {
    previousAdapter = BfdMDC.getMDCAdapter();
  }

  /** Restores the MDC adapter. */
  @AfterAll
  public static void afterAll() {
    BfdMDC.setMDCAdapter(previousAdapter);
  }

  /***
   * Test arguments.
   * @return arguments
   */
  public static Stream<Arguments> loggingTest() {
    return Stream.of(
        arguments("nonsensitive", "nonsensitive"),
        arguments("1S00S00SS00", "*********"),
        arguments("1S00-S00-SS00", "*********"),
        arguments("1S00-S00SS00", "*********"),
        arguments("1S00S00-SS00", "*********"),
        arguments("1s00S00sS00", "*********"),
        arguments("1S00S00SS001S00S00SS00", "******************"),
        arguments("1S00S00SS00dd1S00S00SS00", "*********dd*********"),
        arguments("path/1S00S00SS00", "path/*********"),
        arguments("path/1s00S00sS00", "path/*********"),
        arguments("1S00S00SS00/path/1S00S00SS00", "*********/path/*********"),
        arguments(
            "_format=application%2Ffhir%2Bjson&identifier=http%3A%2F%2Fhl7.org%2Ffhir%2Fsid%2Fus-mbi%1SS0SS0SS00",
            "_format=application%2Ffhir%2Bjson&identifier=http%3A%2F%2Fhl7.org%2Ffhir%2Fsid%2Fus-mbi%*********"),
        arguments(
            "_format=application%2Ffhir%2Bjson1SS0SS0SS00&identifier=http%3A%2F%2Fhl7.org%2Ffhir%2Fsid%2Fus-mbi%1SS0SS0SS00",
            "_format=application%2Ffhir%2Bjson*********&identifier=http%3A%2F%2Fhl7.org%2Ffhir%2Fsid%2Fus-mbi%*********"));
  }

  /***
   * Verify the query string is logged correctly.
   * @param queryString query string
   * @param loggedQueryString logged query string
   * @throws ServletException Servlet exception
   */
  @ParameterizedTest
  @MethodSource("loggingTest")
  public void logQueryString(String queryString, String loggedQueryString) throws ServletException {
    Mockito.when(servletRequest.getQueryString()).thenReturn(queryString);
    Mockito.when(servletRequest.getRequestURI()).thenReturn("");
    Mockito.when(servletRequest.getRequestURL()).thenReturn(new StringBuffer());
    Mockito.when(servletRequest.getHeaderNames())
        .thenReturn(Collections.enumeration(new ArrayList<>()));
    Mockito.when(servletResponse.getHeader("Content-Length")).thenReturn("0");
    RequestResponsePopulateMdcFilter filter = new RequestResponsePopulateMdcFilter();
    filter.doFilterInternal(servletRequest, servletResponse, filterChain);
    Mockito.verify(mdcMock).put("http_access_request_query_string", loggedQueryString);
  }

  /**
   * Verify the URL is logged correctly.
   *
   * @param requestUrl request URL
   * @param loggedRequestUrl logged request URL
   * @throws ServletException Servlet exception
   */
  @ParameterizedTest
  @MethodSource("loggingTest")
  public void logRequestUrl(String requestUrl, String loggedRequestUrl) throws ServletException {
    Mockito.when(servletRequest.getQueryString()).thenReturn("");
    Mockito.when(servletRequest.getRequestURI()).thenReturn("");
    Mockito.when(servletRequest.getRequestURL()).thenReturn(new StringBuffer().append(requestUrl));
    Mockito.when(servletRequest.getHeaderNames())
        .thenReturn(Collections.enumeration(new ArrayList<>()));
    Mockito.when(servletResponse.getHeader("Content-Length")).thenReturn("0");
    RequestResponsePopulateMdcFilter filter = new RequestResponsePopulateMdcFilter();
    filter.doFilterInternal(servletRequest, servletResponse, filterChain);
    Mockito.verify(mdcMock).put("http_access_request_url", loggedRequestUrl);
  }

  /**
   * Verify the URI is logged correctly.
   *
   * @param requestUri request URI
   * @param loggedRequestUri logged request URI
   * @throws ServletException Servlet exception
   */
  @ParameterizedTest
  @MethodSource("loggingTest")
  public void logRequestUri(String requestUri, String loggedRequestUri) throws ServletException {
    Mockito.when(servletRequest.getQueryString()).thenReturn("");
    Mockito.when(servletRequest.getRequestURI()).thenReturn(requestUri);
    Mockito.when(servletRequest.getRequestURL()).thenReturn(new StringBuffer());
    Mockito.when(servletRequest.getHeaderNames())
        .thenReturn(Collections.enumeration(new ArrayList<>()));
    Mockito.when(servletResponse.getHeader("Content-Length")).thenReturn("0");
    RequestResponsePopulateMdcFilter filter = new RequestResponsePopulateMdcFilter();
    filter.doFilterInternal(servletRequest, servletResponse, filterChain);
    Mockito.verify(mdcMock).put("http_access_request_uri", loggedRequestUri);
  }

  /**
   * Verify the header is logged correctly.
   *
   * @param requestHeader request header
   * @param loggedRequestHeader logged request header
   * @throws ServletException Servlet exception
   */
  @ParameterizedTest
  @MethodSource("loggingTest")
  public void logHeader(String requestHeader, String loggedRequestHeader) throws ServletException {
    Mockito.when(servletRequest.getQueryString()).thenReturn("");
    Mockito.when(servletRequest.getRequestURI()).thenReturn("");
    Mockito.when(servletRequest.getRequestURL()).thenReturn(new StringBuffer());
    Mockito.when(servletRequest.getHeaderNames())
        .thenReturn(Collections.enumeration(List.of("test-header1", "test-header2")));
    Mockito.when(servletRequest.getHeaders("test-header1"))
        .thenReturn(Collections.enumeration(List.of(requestHeader)));
    Mockito.when(servletRequest.getHeaders("test-header2"))
        .thenReturn(Collections.enumeration(Arrays.asList(requestHeader, requestHeader)));
    Mockito.when(servletResponse.getHeader("Content-Length")).thenReturn("0");
    RequestResponsePopulateMdcFilter filter = new RequestResponsePopulateMdcFilter();
    filter.doFilterInternal(servletRequest, servletResponse, filterChain);
    Mockito.verify(mdcMock).put("http_access_request_header_test-header1", loggedRequestHeader);
    Mockito.verify(mdcMock)
        .put(
            "http_access_request_header_test-header2",
            String.format("[%1$s, %1$s]", loggedRequestHeader));
  }
}
