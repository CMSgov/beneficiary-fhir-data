package gov.cms.bfd.sharedutils.config;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersByPathRequest;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.AllArgsConstructor;

/** Client for retrieving parameters from AWS SSM. Defined as an object to facilitate mocking. */
@AllArgsConstructor
public class AwsParameterStoreClient {
  /** Suggested default value to use for batch size. */
  public static final int DEFAULT_BATCH_SIZE = 100;

  /** Used to fetch parameters from AWS parameter store. */
  private final AWSSimpleSystemsManagement ssmClient;

  /** Number of parameters to download per API call. */
  private final int batchSize;

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
      request.setMaxResults(batchSize);
      do {
        final var prefix = path.endsWith("/") ? path : path + "/";
        final var response = ssmClient.getParametersByPath(request);
        for (Parameter parameter : response.getParameters()) {
          final var paramPath = parameter.getName();
          final var paramName = paramPath.substring(prefix.length());
          mapBuilder.put(paramName, parameter.getValue());
        }
        request.setNextToken(response.getNextToken());
      } while (!Strings.isNullOrEmpty(request.getNextToken()));
      return mapBuilder.build();
    } catch (RuntimeException ex) {
      throw new ConfigException("--init--", "AWS call failure", ex);
    }
  }
}
