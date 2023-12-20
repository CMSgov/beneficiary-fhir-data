package gov.cms.bfd.pipeline.ccw.rif.extract;

import java.io.File;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

@AllArgsConstructor
public class MyInputFile implements InputFile {
  private final File file;

  @Override
  public long getLength() throws IOException {
    return file.length();
  }

  @Override
  public SeekableInputStream newStream() throws IOException {
    return new MySeekableInputStream(file);
  }
}
