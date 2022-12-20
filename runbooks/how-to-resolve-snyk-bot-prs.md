Follow this runbook to resolve a Java Maven dependency upgrade PR that is created by SNYK-bot.

1. Determine whether the PR may be merged as-is by answering these questions (all must be YES to merge as-is)
   - Github actions are all passing (enforced by github)
   - Code reviews have been completed (enforced by github)
   - All other github preconditions have been met (enforced by github)
   - The PR does not involve an upgrade to any of these artifacts which require additional scrutiny:
     - ca.uhn.hapi.fhir:* (HAPI)
     - org.springframework.\*:\* (Spring)
     - org.hibernate.\*:\* (Hibernate)
     - org.eclipse.jetty.\*:\* (Jetty)
     - javax.\*:\* (Java EE)
     - jakarta.\*:\* (Jakarta EE)
     - io.grpc.\*:\* (GRPC)
     - JDK/JRE

2. If the conditions above are all met, the PR may be merged and no further action is required.

3. If any of the conditions above are NOT met these steps should be followed:
   - Create a JIRA ticket or modify the AC of an existing ticket to capture the task of performing the upgrade manually.
     - For upgrades that resolve critical severity vulnerabilities, the ticket is considered a sprint buster and should
       be scheduled in the current sprint.
     - For non-critical upgrades, the ticket should be scheduled no later than the next PI as part of the regularly
       occurring dependency upgrade sweep.
     - The ticket should reference the SNYK PR and explain why it could not be merged.
     - The ticket AC should include performance regression testing.
   - Close the PR with a comment that references the JIRA ticket and a brief explanation of why it was not merged.

Additional information (not exhaustive) for creating JIRA upgrade tickets:
 - BFD relies on the Java/Jakarta EE ecosystem directly and indirectly via several dependencies: Jetty, HAPI,
   Hibernate(JPA), JAXB, Jakarta Annotations, and potentially others now or in the future. Care must be taken when
   upgrading these components to ensure cross compatibility even when newer versions are available. The versions of
   these components must be harmonized around a single EE version.
 - Keeping HAPI on the latest version is highly desirable and this determines the Java Servlet API version that must
   be used by BFD and consequently the EE version and the Jetty version.
 - The BFD Spring version should match the Spring version that HAPI depends on, so the HAPI version effectively
   determines the Spring version that BFD will use.
 - The Jetty version should be the highest version available that supports the EE version that is compatible with HAPI.
 - Java EE and Jakarta EE libraries should be the latest available for the EE version that is compatible with HAPI/Jetty.
 - Hibernate and JPA libraries must be mutually compatible with each other and with the EE version.
 - Hibernate artifacts must be mutually compatible and use the same version numbers whenever possible.
 - Spring artifacts must be mutually compatible.
 - As of this writing (Aug 2022), the latest available version of HAPI is compatible with Servlet API 4.0 which is
   supported by Jetty 10 (even though a later version of Jetty that is compatible with Servlet API 5.0 is available)
   which is licensed as Jakarta EE 8 but still uses the javax namespace as part of the transition from Java EE to
   Jakarta EE.

References:
 - [Jetty versions and corresponding EE and Servlet API versions](https://eclipse.org/jetty/)
 - [HAPI versions](https://hapifhir.io/hapi-fhir/docs/getting_started/versions.html)
 - [Spring version compatibility](https://docs.spring.io/spring-data/jpa/docs/2.7.0/reference/html/#dependencies.spring-framework) 
 - [Background on Java EE to Jakarta EE transition](https://blogs.oracle.com/javamagazine/post/transition-from-java-ee-to-jakarta-ee)