package gov.cms.bfd.pipeline.ccw.rif.extract;

import com.google.common.base.Strings;
import gov.cms.model.dsl.codegen.library.RifObject;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.apache.parquet.example.data.simple.SimpleGroup;

@AllArgsConstructor
public class RifParquetObject implements RifObject {
  private final long recordNumber;
  private final SimpleGroup group;
  private final List<String> fieldNames;

  @Nullable
  @Override
  public String get(String key) {
    return Strings.nullToEmpty(group.getString(key, 0));
  }

  @Override
  public long getRecordNumber() {
    return recordNumber;
  }

  @Override
  public Stream<String> stream() {
    return fieldNames.stream().map(this::get);
  }

  @Nonnull
  @Override
  public Iterator<String> iterator() {
    return stream().iterator();
  }
}
