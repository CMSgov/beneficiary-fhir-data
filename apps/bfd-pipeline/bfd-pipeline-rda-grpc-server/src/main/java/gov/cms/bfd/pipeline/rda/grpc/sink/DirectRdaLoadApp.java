package gov.cms.bfd.pipeline.rda.grpc.sink;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadJob;
import gov.cms.bfd.pipeline.rda.grpc.RdaLoadOptions;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcRdaSource;
import gov.cms.bfd.pipeline.sharedutils.DatabaseOptions;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.time.Duration;
import java.util.Properties;

/**
 * Simple application that invokes an RDA API server and writes FISS claims to a database using
 * RdaLoadJob. This program was written for testing purposes and is not planned to be executed in a
 * production environment.
 */
public class DirectRdaLoadApp {
  private static final String RDA_PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.printf("usage: %s configfile%n", DirectRdaLoadApp.class.getSimpleName());
      System.exit(1);
    }
    Properties props = new Properties();
    try (Reader in = new BufferedReader(new FileReader(args[0]))) {
      props.load(in);
    }
    final RdaLoadOptions jobConfig = readRdaLoadOptionsFromProperties(props);
    final DatabaseOptions databaseConfig = readDatabaseOptions(props);
    final PipelineApplicationState appState =
        new PipelineApplicationState(
            new MetricRegistry(), databaseConfig, RDA_PERSISTENCE_UNIT_NAME);
    final PipelineJob job = jobConfig.createFissClaimsLoadJob(appState);
    job.call();
  }

  private static DatabaseOptions readDatabaseOptions(Properties props) {
    return new DatabaseOptions(
        props.getProperty("database.url"),
        props.getProperty("database.user"),
        props.getProperty("database.password"),
        10);
  }

  private static RdaLoadOptions readRdaLoadOptionsFromProperties(Properties props) {
    final IdHasher.Config idHasherConfig =
        new IdHasher.Config(
            getIntOrDefault(props, "hash.iterations", 100),
            props.getProperty("hash.pepper", "notarealpepper"));
    final RdaLoadJob.Config jobConfig =
        new RdaLoadJob.Config(Duration.ofDays(1), getIntOrDefault(props, "job.batchSize", 1));
    final GrpcRdaSource.Config grpcConfig =
        new GrpcRdaSource.Config(
            props.getProperty("api.host", "localhost"),
            getIntOrDefault(props, "api.port", 5003),
            Duration.ofSeconds(getIntOrDefault(props, "job.idleSeconds", Integer.MAX_VALUE)));
    return new RdaLoadOptions(jobConfig, grpcConfig, idHasherConfig);
  }

  private static int getIntOrDefault(Properties props, String key, int defaultValue) {
    String strValue = props.getProperty(key);
    return strValue != null ? Integer.parseInt(strValue) : defaultValue;
  }
}
