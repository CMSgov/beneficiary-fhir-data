package gov.cms.bfd.pipeline.ccw.rif.extract;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import org.apache.parquet.io.SeekableInputStream;

public class MySeekableInputStream extends SeekableInputStream {
  private final RandomAccessFile source;

  public MySeekableInputStream(File file) throws IOException {
    source = new RandomAccessFile(file, "r");
  }

  @Override
  public long getPos() throws IOException {
    return source.getFilePointer();
  }

  @Override
  public void seek(long newPos) throws IOException {
    source.seek(newPos);
  }

  @Override
  public void readFully(byte[] bytes) throws IOException {
    source.readFully(bytes);
  }

  @Override
  public void readFully(byte[] bytes, int start, int len) throws IOException {
    source.readFully(bytes, start, len);
  }

  @Override
  public int read(ByteBuffer buf) throws IOException {
    return source.read();
  }

  @Override
  public void readFully(ByteBuffer buf) throws IOException {
    byte[] buffer = new byte[8192];
    int remaining = buf.remaining();
    while (remaining > 0) {
      int read = source.read(buffer, 0, Math.min(remaining, buffer.length));
      if (read <= 0) {
        throw new EOFException(
            "Reached the end of stream. Still have: " + buf.remaining() + " bytes left");
      }
      remaining -= read;
    }
  }

  @Override
  public int read() throws IOException {
    return source.read();
  }

  @Override
  public void close() throws IOException {
    super.close();
    source.close();
  }
}
