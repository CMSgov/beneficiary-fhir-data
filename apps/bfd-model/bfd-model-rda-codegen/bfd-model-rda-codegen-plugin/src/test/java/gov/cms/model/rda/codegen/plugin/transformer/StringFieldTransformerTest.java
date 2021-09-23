package gov.cms.model.rda.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.RootBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;
import org.junit.Test;

public class StringFieldTransformerTest {
  @Test
  public void requiredField() {
    ColumnBean column =
        ColumnBean.builder().name("hicNo").nullable(true).sqlType("varchar(12)").build();
    TransformationBean transformation =
        TransformationBean.builder().optional(false).from("hicNo").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    StringFieldTransformer generator = new StringFieldTransformer();
    CodeBlock block = generator.generateCodeBlock(mapping, column, transformation);
    assertEquals(
        "transformer.copyString(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.hicNo, true, 1, 12, from.getHicNo(), to::setHicNo);\n",
        block.toString());
  }

  @Test
  public void optionalField() {
    ColumnBean column =
        ColumnBean.builder().name("hicNo").nullable(true).sqlType("varchar(12)").build();
    TransformationBean transformation = TransformationBean.builder().from("hicNo").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    StringFieldTransformer generator = new StringFieldTransformer();
    CodeBlock block = generator.generateCodeBlock(mapping, column, transformation);
    assertEquals(
        "transformer.copyOptionalString(namePrefix + gov.cms.bfd.model.rda.PreAdjFissClaim.Fields.hicNo, 1, 12, from::hasHicNo, from::getHicNo, to::setHicNo);\n",
        block.toString());
  }
}
