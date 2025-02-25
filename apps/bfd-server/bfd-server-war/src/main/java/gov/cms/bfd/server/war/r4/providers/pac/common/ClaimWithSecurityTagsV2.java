package gov.cms.bfd.server.war.r4.providers.pac.common;

import java.util.Set;
import lombok.Getter;

/** DAO of ClaimWithSecurityTagsV2. */
@Getter
public class ClaimWithSecurityTagsV2<T> {
  private T claimEntity;
  private Set<String> securityTags;

  /**
   * The tag ClaimWithSecurityTagsV2.
   *
   * @param claimEntity claimEntity
   * @param securityTags securityTags
   */
  public ClaimWithSecurityTagsV2(T claimEntity, Set<String> securityTags) {
    this.claimEntity = claimEntity;
    this.securityTags = securityTags;
  }
}
