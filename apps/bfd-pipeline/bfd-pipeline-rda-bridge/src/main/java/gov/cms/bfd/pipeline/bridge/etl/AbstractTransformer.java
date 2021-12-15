package gov.cms.bfd.pipeline.bridge.etl;

import com.google.protobuf.MessageOrBuilder;

public interface AbstractTransformer {

  MessageOrBuilder transform(Parser.Data<String> data);
}
