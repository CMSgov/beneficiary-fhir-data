# Upgrading Dependencies

All dependency versions should be declared in the `pom.xml` file contained in this folder.

Run `mvn versions:update-properties` to update dependencies to their latest versions.
Incompatible dependency versions should be tracked in `rules.xml`.

Note: running `mvn versions:use-latest-versions` should yield no changes as long as all versions are correctly tracked as properties.

# Running Tests

When running unit and integration tests with `mvn clean install`, you may get a test failure for `bfd-server-war` which looks like this (Note the RYUK reference):

```
 [INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running gov.cms.bfd.server.war.ServerCapabilityStatementIT
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 0.648 s <<< FAILURE! -- in gov.cms.bfd.server.war.ServerCapabilityStatementIT
[ERROR] gov.cms.bfd.server.war.ServerCapabilityStatementIT -- Time elapsed: 0.648 s <<< ERROR!
org.testcontainers.containers.ContainerLaunchException: Container startup failed for image testcontainers/ryuk:0.5.1
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:362)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:333)
        at org.testcontainers.utility.RyukResourceReaper.maybeStart(RyukResourceReaper.java:78)
        at org.testcontainers.utility.RyukResourceReaper.init(RyukResourceReaper.java:42)
        at org.testcontainers.DockerClientFactory.client(DockerClientFactory.java:232)
        at org.testcontainers.DockerClientFactory$1.getDockerClient(DockerClientFactory.java:106)
        at com.github.dockerjava.api.DockerClientDelegate.authConfig(DockerClientDelegate.java:109)
        at org.testcontainers.containers.GenericContainer.start(GenericContainer.java:332)
        at gov.cms.bfd.DatabaseTestUtils.initUnpooledDataSourceForTestContainerWithPostgres(DatabaseTestUtils.java:298)
        at gov.cms.bfd.DatabaseTestUtils.initUnpooledDataSource(DatabaseTestUtils.java:193)
        at gov.cms.bfd.DatabaseTestUtils.initUnpooledDataSource(DatabaseTestUtils.java:140)
        at gov.cms.bfd.DatabaseTestUtils.<init>(DatabaseTestUtils.java:82)
        at gov.cms.bfd.DatabaseTestUtils.get(DatabaseTestUtils.java:97)
        at gov.cms.bfd.server.war.ServerRequiredTest.setup(ServerRequiredTest.java:61)
        at java.base/java.lang.reflect.Method.invoke(Method.java:565)
        at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
Caused by: org.rnorth.ducttape.RetryCountExceededException: Retry limit hit with exception
        at org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess(Unreliables.java:88)
        at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:347)
        ... 15 more
Caused by: org.testcontainers.containers.ContainerLaunchException: Could not create/start container
        at org.testcontainers.containers.GenericContainer.tryStart(GenericContainer.java:566)
        at org.testcontainers.containers.GenericContainer.lambda$doStart$0(GenericContainer.java:357)
        at org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess(Unreliables.java:81)
        ... 16 more
Caused by: com.github.dockerjava.api.exception.InternalServerErrorException: Status 500: {"cause":"operation not supported","message":"make cli opts(): making volume mountpoint for volume /var/folders/zl/zc2dfjm56zb6rcd80fv9lnh00000gp/T/podman/podman-machine-default-api.sock: mkdir /var/folders/zl/zc2dfjm56zb6rcd80fv9lnh00000gp/T/podman/podman-machine-default-api.sock: operation not supported","response":500}
```

If you get this failure, follow these steps:
1. Using Podman without Docker

Insure that you do not have docker installed.  You can check by running `which docker` and `docker --version` which may yield something like what follows:
```
❯ docker --version
Docker version 29.1.3, build f52814d454
❯ which docker
/opt/homebrew/bin/docker
```

You should not have Docker installed because it would likely cause conflicts with Podman which is what you should be using.

	- If you used Homebrew to install Docker, run `brew remove docker`.

	- Regardless of how you installed it, also follow these additional steps to insure it's removed (Note the section marked IMPORTANT):
    [Uninstall Docker](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Local-Environment-Setup-for-BFD-Development#podman)

	- Verify you are actually using Podman when issuing a Docker command:
    ```
    docker --version
    podman version 5.7.1
    ```
2. Enable Docker-compatible Socket and CLI Shim (Persistently)
```
podman machine ssh sudo systemctl enable --now podman.socket
```

3. Export the Docker socket for Podman

You should have a symbolic (soft) link to this in your `.local` folder such that you can export the socket for Podman like so:
```
export DOCKER_HOST=unix:///Users/$USER/.local/share/containers/podman/machine/podman.sock
```

If not, then the following should work:
```
export DOCKER_HOST=unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')
```

For persistence in the future, you should add this export to your exports section of your local bash profile or `.zshrc`.

Test that `docker ps` works.

4. Disable RYUK (IMPORTANT)

Run this export in your shell and, for persistence, also add it to your bash profile or `~/.zshrc`:

```
export TESTCONTAINERS_RYUK_DISABLED=true
```

Alternately, add this switch to your Maven command:
```
-Dtestcontainers.ryuk.disabled=true
```

5. Verify Everything Works Properly
    1. Run `podman run hello-world`
    2. Run `docker run hello-world`

Both commands should work.

The unit and integration tests should now succeed.
