package gov.cms.model.rda.codegen.plugin.model;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MappingBean {
  private String id;
  private String messageClassName;
  private String entityClassName;
  private String transformerClassName;
  private TableBean table;
  @Singular private List<EnumTypeBean> enumTypes = new ArrayList<>();
  @Singular private List<TransformationBean> transformations = new ArrayList<>();
  @Singular private List<ArrayElement> arrays = new ArrayList<>();
  @Singular private List<ExternalTransformationBean> externalTransformations = new ArrayList<>();

  public EnumTypeBean findEnum(String enumName) {
    return enumTypes.stream()
        .filter(e -> enumName.equals(e.getName()))
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(
                        "reference to non-existent enum %s in mapping %s", enumName, id)));
  }

  public boolean hasTransformer() {
    return !Strings.isNullOrEmpty(transformerClassName);
  }

  public boolean hasArrayElements() {
    return arrays.size() > 0;
  }

  public boolean hasExternalTransformations() {
    return externalTransformations.size() > 0;
  }

  public String entityPackageName() {
    return ModelUtil.packageName(entityClassName);
  }

  public String entityClassName() {
    return ModelUtil.className(entityClassName);
  }

  public String transformerPackage() {
    return ModelUtil.packageName(transformerClassName);
  }

  public String transformerSimpleName() {
    return ModelUtil.className(transformerClassName);
  }

  public Optional<TransformationBean> firstPrimaryKeyField() {
    return transformations.stream().findFirst();
  }
}
