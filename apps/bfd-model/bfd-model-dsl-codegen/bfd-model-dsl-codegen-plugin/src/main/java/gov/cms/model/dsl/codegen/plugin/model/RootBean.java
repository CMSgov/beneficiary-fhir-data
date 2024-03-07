package gov.cms.model.dsl.codegen.plugin.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

/**
 * Root object in data model that contains all of the mappings and allows them to be accessed by id
 * or entity class name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RootBean {
  /** One or more {@link MappingBean} objects contained in the data model. */
  @Singular private List<MappingBean> mappings = new ArrayList<>();

  /**
   * Adds all {@link MappingBean}s from the given {@link RootBean} into this {@link RootBean}.
   *
   * @param other another {@link RootBean} containing mappings we want to add
   */
  public void addMappingsFrom(RootBean other) {
    mappings.addAll(other.mappings);
  }

  /**
   * Search for a {@link MappingBean} with the given id value.
   *
   * @param id mapping id to search for
   * @return {@link Optional} containing the mapping if it exists or empty if it does not exist
   */
  public Optional<MappingBean> findMappingWithId(String id) {
    return mappings.stream().filter(m -> id.equals(m.getId())).findAny();
  }

  /**
   * Search for a {@link MappingBean} with the given entity class name value.
   *
   * @param entityClassName mapping entity class to search for
   * @return {@link Optional} containing the mapping if it exists or empty if it does not exist
   */
  public Optional<MappingBean> findMappingWithEntityClassName(String entityClassName) {
    return mappings.stream().filter(m -> entityClassName.equals(m.getEntityClassName())).findAny();
  }

  /**
   * Search for a {@link MappingBean} for the given {@link JoinBean}. Uses either the {@link
   * JoinBean#entityClass} or {@link JoinBean#entityMapping} (whichever is defined) to find a
   * matching {@link MappingBean}.
   *
   * @param join {@link JoinBean} to find mapping for
   * @return {@link Optional} containing the mapping if it exists or empty if it does not exist
   */
  public Optional<MappingBean> findMappingForJoinBean(JoinBean join) {
    if (join.getEntityMapping() != null) {
      return findMappingWithId(join.getEntityMapping());
    } else {
      return findMappingWithEntityClassName(join.getEntityClass());
    }
  }

  /**
   * Find and return the appropriate class name for the entity class of the {@link JoinBean}.
   *
   * @param join {@link JoinBean} to find entity for
   * @return {@link Optional} containing the class name if it exists or empty if it does not exist
   */
  public Optional<String> getEntityClassForJoin(JoinBean join) {
    Optional<String> className;
    if (join.getEntityClass() != null) {
      className = Optional.of(join.getEntityClass());
    } else {
      className = findMappingWithId(join.getEntityMapping()).map(MappingBean::getEntityClassName);
    }
    return className;
  }
}
