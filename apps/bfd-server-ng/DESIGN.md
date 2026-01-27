# API and Data Mapping Design

The main purpose of the BFD server is mapping source data to FHIR resources.
This process lends itself to a somewhat unique set of challenges due to the nature of the data and the large amount of fields we surface with each API request, notably:

- **Wide data**: some of our tables have over 100 fields and we join several tables together while surfacing every field on every request.
- **Deep data**: many beneficiaries have many thousands of claims, claim line items can multiply the volume of rows by an order of magnitude past that.
- **Optional fields**: The vast majority of columns we store are optional, meaning there is a large possibility of null reference exceptions if adequate care is not taken.

## Handling Wide Data

We use a modular approach to segment large tables. Many JPA libraries support `@Embedded` classes to split large entities into smaller classes.
This allows us to keep the FHIR mapping logic directly within the entity classes without having classes that are unmanageably large.
The vast number of column we handle means we need to stick with an approach that can automatically map database result sets to java fields without any hand-written mapping logic.

## Handling Deep Data

Considering the volume of data, we need to be careful to minimize the number of queries we issue for a single request and limit the number of rows returned.
We've designed the database to minimize the number of one-to-many and many-to-many relationships so that we don't have the cartesian product problem when issuing queries.
Multiple one-to-many relationships should only be used when the volume of data returned for each relationship is guaranteed to be small and strictly bounded.

## Handling Optional Fields

In our entity classes, we use `Optional<T>` types to represent fields which could be null/empty in the database.
This is somewhat unconventional, as doing so contradicts the generally recommended practice of only using `Optional` for return values.
However, we need a way to protect against null reference exceptions with the vast amount of potentially nullable fields we have.
Without the type system to protect us, we would need to be very diligent in always checking for null before attempting to reference
any of the hundreds of optional fields we store. 
It isn't feasible to check every combination of missing/present values in our test data due to the volume of fields.

### Left Joins

Left joins are the one place that we can't easily protect against null reference exceptions.
This is because the child entity is stored as a field on the parent entity, but the child will always be null in the case of a left join that returns no match.
As opposed to columns, there's no middleware mechanism to wrap these in `Optional`.
As a workaround, we can store all left joined entities in a separate embedded class, protecting the main entity from accessing the private fields that could be null.
The result looks like this:

```java
class MyEntity {
    @Embedded private MyEntityOptional myEntityOptional.
}

@Embeddable
class MyEntityOptional {
    @Column(name = "cntrct_pbp_sk", insertable = false, updatable = false)
    private long alwaysPresentField;

    @Nullable
    @OneToOne
    @JoinColumn(name = "my_column")
    private ChildEntity childEntity;

    public Optional<ChildEntity> getChildEntity() {
        return Optional.ofNullable(childEntity);
    }
}
```

and then in our main classes, we're prevented from accessing the nullable `childEntity` field.

```java
class MyEntity {
    private void doStuff() {
        // Null reference prevented since we've hidden away the nullable fields
        myOptionalEntity.getChildEntity().isPresent();
    }
}
```

Unfortunately, there is one hack that we have to do to make this work.
If you have an entity with no fields present, Hibernate will automatically set the entity to null instead of returning an object with no columns present.
To get around this, we have to include one field that will always be present, forcing the relationship to be non-nullable.

```java
@Embeddable
class MyEntityOptional {
    // Including a field that's always present ensures this entity will not be null
    @Column(name = "always_present", insertable = false, updatable = false)
    private long alwaysPresentField;

    @Nullable
    @OneToOne
    @JoinColumn(name = "my_column")
    private ChildEntity childEntity;

    public Optional<ChildEntity> getChildEntity() {
        return Optional.ofNullable(childEntity);
    }
}
```

This is a pretty unfortunate hack, but it's currently the only way to ensure type safety.
There's a property in Hibernate to force this (`hibernate.create_empty_composites.enabled`), but it's currently deprecated and marked for removal.
