package gov.cms.bfd.sharedutils.config;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;

/** Client for retrieving parameters from AWS SSM. Defined as an object to facilitate mocking. */
@AllArgsConstructor
public class AwsParameterStoreClient {
  /** Suggested default value to use for batch size. */
  public static final int DEFAULT_BATCH_SIZE = 10;

  /** Used to fetch parameters from AWS parameter store. */
  private final SsmClient ssmClient;

  /** Number of parameters to download per API call. */
  private final int batchSize;

  /**
   * Load all parameters at the specified path into an immutable {@link Map} and return it. Can be
   * used with {@link ConfigLoader.Builder#addMap}. The base path is not included in any parameter
   * names. When loading parameters recursively any '/' characters from sub-hierarchies are retained
   * in the final parameter name.
   *
   * @param path path containing the parameters
   * @param recursive true if parameters from sub-folders should also be downloaded
   * @return {@link Map} of values
   * @throws ConfigException if AWS call fails
   */
  public Map<String, String> loadParametersAtPath(String path, boolean recursive) {
    try {
      final var mapBuilder = ImmutableMap.<String, String>builder();
      var request =
          GetParametersByPathRequest.builder()
              .path(path)
              .withDecryption(true)
              .recursive(recursive)
              .maxResults(batchSize)
              .build();
      do {
        final var prefix = path.endsWith("/") ? path : path + "/";
        final var response = ssmClient.getParametersByPath(request);
        for (Parameter parameter : response.parameters()) {
          final var paramPath = parameter.name();
          final var paramName = paramPath.substring(prefix.length());
          mapBuilder.put(paramName, parameter.value());
        }
        request = request.toBuilder().nextToken(response.nextToken()).build();
      } while (!Strings.isNullOrEmpty(request.nextToken()));
      return mapBuilder.build();
    } catch (RuntimeException ex) {
      throw new ConfigException("--init--", "AWS call failure", ex);
    }
  }
}
