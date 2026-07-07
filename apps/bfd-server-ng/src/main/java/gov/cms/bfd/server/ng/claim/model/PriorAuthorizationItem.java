package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.*;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.jetbrains.annotations.NotNull;

@Getter
@Entity
@EqualsAndHashCode
@Table(name = "prior_auth_item", schema = "idr")
class PriorAuthorizationItem implements Comparable<PriorAuthorizationItem> {

  @EmbeddedId private PriorAuthorizationItemId priorAuthorizationItemId;
  @Embedded private HcpcsOrCptOrHippsCode hcpcsOrCptOrHipps;
  @Embedded private PriceModifierCode priceModifierCode;
  @Embedded private PriorAuthorizationItemExtensions extensions;

  @Column(name = "current_segment")
  private int currentSegment;

  @Column(name = "place_of_serv")
  private Optional<ClaimPlaceOfServiceCode> placeOfServiceCode;

  @Column(name = "rev_code_1")
  private Optional<ClaimLineRevenueCenterCode> revenueCode1;

  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent() {
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(currentSegment);
    populateProductAndQuantity(line);
    line.addModifier(priceModifierCode.toFhir());
    placeOfServiceCode.ifPresent(c -> line.setLocation(c.toFhir()));
    revenueCode1.ifPresent(
        c -> {
          var revenueCoding = c.toFhir(Optional.empty());
          line.setRevenue(revenueCoding);
        });
    extensions.toFhir().forEach(line::addExtension);

    return Optional.of(line);
  }

  void populateProductAndQuantity(ExplanationOfBenefit.ItemComponent line) {
    var productOrService = hcpcsOrCptOrHipps.toFhir();
    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
  }

  @Override
  public int compareTo(@NotNull PriorAuthorizationItem o) {
    return this.priorAuthorizationItemId.compareTo(o.priorAuthorizationItemId);
  }
}
