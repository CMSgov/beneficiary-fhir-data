package gov.cms.bfd.model.metadata;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link FieldDefinitionImpl}. */
public final class FieldDefinitionImplTest {
  /** Tests {@link FieldDefinitionImpl#parseFromYaml(String)}. */
  @Test
  public void yamlParsing() {
    FieldDefinitionImpl ccwBeneIdFieldDefinition =
        FieldDefinitionImpl.parseFromYaml("field_definitions/sample_a.yml");
    Assert.assertEquals("sample_a", ccwBeneIdFieldDefinition.getId());
    Assert.assertEquals(Optional.of("SAMPLE_A"), ccwBeneIdFieldDefinition.getCommonName());
    Assert.assertEquals(
        Optional.of("First line.\n\n_Second line._\n"),
        ccwBeneIdFieldDefinition.getDescriptionAsMarkdown());
    Assert.assertEquals(
        Optional.of("<p>First line.</p>\n<p><em>Second line.</em></p>\n"),
        ccwBeneIdFieldDefinition.getDescriptionAsHtml());
  }
}
