package gov.cms.bfd.server.ng.testUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

public class SnapshotHelper {
  public static File getPatchfile(Class<?> currentClass, String filename) throws IOException {
    Properties properties = new Properties();
    properties.load(currentClass.getClassLoader().getResourceAsStream("snapshot.properties"));
    var snapshotDir = properties.getProperty("snapshot-dir");

    return new File(
        Paths.get("src/test/java/gov/cms/bfd/server/ng", snapshotDir, filename + ".patch")
            .toString());
  }
}
