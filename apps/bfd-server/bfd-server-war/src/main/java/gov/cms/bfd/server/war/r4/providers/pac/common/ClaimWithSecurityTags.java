package gov.cms.bfd.server.war.r4.providers.pac.common;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DAO of claimWithSecurityTags.
 *
 * @param <T> the claim entity type
 */
@Getter
@AllArgsConstructor
public class ClaimWithSecurityTags<T> {
  private T claimEntity;
  private Set<String> securityTags;
}
