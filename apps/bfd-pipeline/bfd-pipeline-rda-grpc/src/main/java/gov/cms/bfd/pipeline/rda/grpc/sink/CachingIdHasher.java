package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.util.concurrent.ExecutionException;

public class CachingIdHasher extends IdHasher {
  private final Cache<String, String> cache;

  public CachingIdHasher(Config config) {
    super(config);
    cache = CacheBuilder.newBuilder().maximumSize(config.getCacheSize()).build();
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
