package gov.cms.bfd.pipeline.bridge.etl;

import com.google.protobuf.MessageOrBuilder;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractTransformer {

  protected static final AtomicInteger mpnCounter = new AtomicInteger();

  public abstract MessageOrBuilder transform(Parser.Data<String> data);
}
