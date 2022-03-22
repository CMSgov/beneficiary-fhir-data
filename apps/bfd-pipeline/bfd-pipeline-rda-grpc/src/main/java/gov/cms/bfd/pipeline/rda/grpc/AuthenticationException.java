package gov.cms.bfd.pipeline.rda.grpc;

/** Indicates that there was an issue with authentication */
public class AuthenticationException extends RuntimeException {
  public AuthenticationException(String message) {
    super(message);
  }
}
