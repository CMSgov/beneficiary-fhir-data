package gov.cms.bfd.server.war.r4.providers.pac.common;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** DAO of ClaimWithSecurityTagsV2. */
@Getter
@AllArgsConstructor
public class ClaimWithSecurityTags<T> {
  private T claimEntity;
  private Set<String> securityTags;
}
