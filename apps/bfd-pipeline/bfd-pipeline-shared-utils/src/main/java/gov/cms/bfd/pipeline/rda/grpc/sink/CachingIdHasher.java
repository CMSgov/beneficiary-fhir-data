package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.util.concurrent.ExecutionException;

/** IdHasher implementation that caches recently computed hash values in memory. */
public class CachingIdHasher extends IdHasher {
  private final Cache<String, String> cache;

  public CachingIdHasher(Config config, int cacheSize) {
    super(config);
    cache = CacheBuilder.newBuilder().maximumSize(cacheSize).build();
  }

  @Override
  public String computeIdentifierHash(String identifier) {
    try {
      return cache.get(identifier, () -> super.computeIdentifierHash(identifier));
    } catch (ExecutionException e) {
      return super.computeIdentifierHash(identifier);
    }
  }
}
