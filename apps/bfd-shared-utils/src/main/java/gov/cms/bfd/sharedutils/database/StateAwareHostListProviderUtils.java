package gov.cms.bfd.sharedutils.database;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.Filter;
import software.amazon.jdbc.HostListProvider;
import software.amazon.jdbc.HostSpec;

/** Utilities for custom "state aware" {@link HostListProvider}s. */
public final class StateAwareHostListProviderUtils {
  /**
   * Returns a filtered {@link List<HostSpec>} of hosts where hosts still in their {@code creating}
   * phase, as reported by the RDS API, are filtered out of the host list.
   *
   * @param hostList {@link List<HostSpec>} of hosts to filter
   * @param rdsClient {@link RdsClient} used to check instance status
   * @return a filtered {@link List<HostSpec>} with all unready hosts removed if there is no AWS API
   *     error. Otherwise, the full host list is returned
   */
  public static List<HostSpec> filterUnreadyHostsByApiState(
      List<HostSpec> hostList, RdsClient rdsClient) {
    try {
      final var dbInstanceIdFilter =
          Filter.builder()
              .name("db-instance-id")
              .values(hostList.stream().map(HostSpec::getHostId).collect(Collectors.toSet()))
              .build();
      final var dbInstancesDetails =
          rdsClient.describeDBInstances(request -> request.filters(dbInstanceIdFilter));

      final var dbInstancesStateMap =
          dbInstancesDetails.dbInstances().stream()
              .collect(
                  Collectors.toMap(DBInstance::dbInstanceIdentifier, DBInstance::dbInstanceStatus));
      return hostList.stream()
          .filter(
              hostSpec ->
                  dbInstancesStateMap.containsKey(hostSpec.getHostId())
                      && !dbInstancesStateMap
                          .get(hostSpec.getHostId())
                          .equalsIgnoreCase("creating"))
          .toList();
    } catch (AwsServiceException e) {
      // We return the full host list on error as there is still DNS round-robin load balancing
      // assuming the Server is connected via the cluster URL. If the nodes returned by the cluster
      // DNS are not in the host list, the Wrapper will fail to connect to the "unknown" node
      return hostList;
    }
  }
}
