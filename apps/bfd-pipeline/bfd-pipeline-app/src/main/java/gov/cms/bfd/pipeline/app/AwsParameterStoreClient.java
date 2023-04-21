package gov.cms.bfd.pipeline.app;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClient;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.amazonaws.services.simplesystemsmanagement.model.ParameterNotFoundException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.sharedutils.config.ConfigException;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.util.Map;

/** Client for retrieving parameters from AWS SSM. Defined as an object to facilitate mocking. */
public class AwsParameterStoreClient {
  /** Used to fetch parameters from AWS parameter store. */
  private final AWSSimpleSystemsManagement ssmClient;

  /**
   * Creates an instance.
   *
   * @param ssmClient {@link AWSSimpleSystemsManagementClient} to use
   */
  public AwsParameterStoreClient(AWSSimpleSystemsManagement ssmClient) {
    this.ssmClient = ssmClient;
  }

  /**
   * Load a single parameter and return its value. Can be used as a source lambda for {@link
   * ConfigLoader}.
   *
   * @param parameterName name of parameter to look up
   * @return value of the parameter or null if parameter is not found
   * @throws ConfigException if AWS call fails
   */
  public String lookupParameter(String parameterName) {
    try {
      final var request = new GetParameterRequest();
      request.setName(parameterName);
      request.setWithDecryption(true);
      final var response = ssmClient.getParameter(request);
      if (response.getParameter().getValue() != null) {
        return response.getParameter().getValue();
      } else {
        return null;
      }
    } catch (ParameterNotFoundException ex) {
      return null;
    } catch (RuntimeException ex) {
      throw new ConfigException(parameterName, "AWS call failure", ex);
    }
  }

  /**
   * Load all parameters at the specified path into an immutable {@link Map} and return it. Can be
   * used with {@link ConfigLoader.Builder#addMap}.
   *
   * @param path path containing the parameters
   * @return {@link Map} of values
   * @throws ConfigException if AWS call fails
   */
  public Map<String, String> loadParametersAtPath(String path) {
    try {
      final var mapBuilder = ImmutableMap.<String, String>builder();
      final var request = new GetParametersByPathRequest();
      request.setPath(path);
      request.setWithDecryption(true);
      request.setRecursive(false);
      do {
        final var response = ssmClient.getParametersByPath(request);
        for (Parameter parameter : response.getParameters()) {
          mapBuilder.put(parameter.getName(), parameter.getValue());
        }
        request.setNextToken(response.getNextToken());
      } while (!Strings.isNullOrEmpty(request.getNextToken()));
      return mapBuilder.build();
    } catch (RuntimeException ex) {
      throw new ConfigException("--init--", "AWS call failure", ex);
    }
  }
}
