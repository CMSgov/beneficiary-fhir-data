package gov.cms.bfd.migrator.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.cms.bfd.sharedutils.database.DatabaseMigrationStage;
import gov.cms.bfd.sharedutils.exceptions.UncheckedIOException;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SqsProgessReporter {
  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .addModule(new Jdk8Module())
          .addModule(new JavaTimeModule())
          .serializationInclusion(JsonInclude.Include.NON_NULL)
          .build();
  private final SqsDao sqsDao;
  private final String queueUrl;

  public void reportMigratorProgress(MigratorProgress progress) {
    final var message =
        new SqsProgressMessage(
            ProcessHandle.current().pid(), progress.getStage(), progress.getMigrationProgress());
    final var messageText = convertMessageToJson(message);
    sqsDao.sendMessage(queueUrl, messageText);
  }

  private String convertMessageToJson(SqsProgressMessage message) {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (JsonProcessingException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  @Data
  public static class SqsProgressMessage {
    private final long pid;
    private final MigratorProgress.Stage appStage;
    @Nullable private final DatabaseMigrationStage migrationStage;
  }
}
