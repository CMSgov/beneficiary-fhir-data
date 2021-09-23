package gov.cms.model.rda.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.RootBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;
import org.junit.Test;

public class AmountFieldTransformerTest {
  @Test
  public void requiredField() {
    ColumnBean column =
        ColumnBean.builder().name("estAmtDue").nullable(true).sqlType("decimal(11,2)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("estAmtDue").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    AmountFieldTransformer generator = new AmountFieldTransformer();
    CodeBlock block = generator.generateCodeBlock(mapping, column, transformation);
    assertEquals(
        "transformer.copyAmount(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.estAmtDue, true, from.getEstAmtDue(), to::setEstAmtDue);\n",
        block.toString());
  }

  @Test
  public void optionalField() {
    ColumnBean column =
        ColumnBean.builder().name("estAmtDue").nullable(true).sqlType("decimal(11,2)").build();
    TransformationBean field = TransformationBean.builder().from("estAmtDue").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(field)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    AmountFieldTransformer generator = new AmountFieldTransformer();
    CodeBlock block = generator.generateCodeBlock(mapping, column, field);
    assertEquals(
        "transformer.copyOptionalAmount(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.estAmtDue, from::hasEstAmtDue, from::getEstAmtDue, to::setEstAmtDue);\n",
        block.toString());
  }
}
