package gov.cms.model.dsl.codegen.library;

import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface RifObject extends Iterable<String> {
  @Nullable
  String get(String key);

  long getRecordNumber();

  Stream<String> stream();
}
