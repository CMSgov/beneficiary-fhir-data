# gen-structure-def

This gives you a break down of all the nodes that can be applied to data dictionary files to have it add to the data dictionary nodes.

Here is a break down of Claim type to profile type

- NCH - all institutional + professional
- FISS - institutional
- MCS - carrier (professional)
- VMS - DME (professional)
- DDPS - part D

### Official HL7 FHIR R4 Documentation Links

Please take notes here. There are strong rules for naming of id and paths of element definitions that would be long to add here and HL7 explains them here.

* **StructureDefinition Resource Overview & Structure:** [http://hl7.org/fhir/R4/structuredefinition.html](http://hl7.org/fhir/R4/structuredefinition.html) — Covers top-level elements (`url`, `name`, `title`, `type`, `differential`) and resource-level invariants.
* **StructureDefinition Detailed Element Descriptions & Invariants:** [http://hl7.org/fhir/R4/structuredefinition-definitions.html](http://hl7.org/fhir/R4/structuredefinition-definitions.html) — Provides exact definitions, comments, and regex constraints (such as `sdf-0` for `name` and path rules for `differential.element`).
* **ElementDefinition Datatype Specification:** [https://hl7.org/fhir/R4/elementdefinition.html](https://hl7.org/fhir/R4/elementdefinition.html) — Defines `id`, `path`, `sliceName`, `label`, `alias`, and element-level invariants for elements inside `differential.element`.
* **ElementDefinition Detailed Element Descriptions:** [https://hl7.org/fhir/R4/elementdefinition-definitions.html](https://hl7.org/fhir/R4/elementdefinition-definitions.html) — Lists full field descriptions and regex validation rules (such as `eld-17` for `sliceName`) for every property on an `ElementDefinition`.

## `structureDef` Node Reference

The `structureDef` node is read from the source YAML inside each top-level node
(`node.get("structureDef")`) and processed in `process_node()`. This section
documents every field it supports, including its nested children.

### Top-Level YAML Node

The outer object read from the source YAML. `structureDef` is one key on it.

| Field | Type | Used For |
|---|---|---|
| `profiles` | `list[str]` | Filters which nodes apply to `self.profile` |
| `structureDef` | `dict` | The element definition payload (see below) |
| `appliesTo` | `list[str]` | Filters nodes against `self.applies_to` |
| `sources` | `list[str]` | Filters nodes against `self.sources` |
| `sourceColumn` | `str` | Becomes `column_name`; falls back to `inputPath` parsing if absent |
| `inputPath` | `str` | Dotted path parsed by `parse_path()`; becomes `el.short` |

### `structureDef` Fields

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `id` | `str` | `ElementDefinition.id` | |
| `path` | `str` | `ElementDefinition.path` | Also drives slicing/sorting lookups |
| `min` | `int` | `ElementDefinition.min` | Defaults to `0` |
| `max` | `str` | `ElementDefinition.max` | Defaults to `"1"` |
| `isSummary` | `bool` | `ElementDefinition.isSummary` | `None` if falsy |
| `mustSupport` | `bool` | `ElementDefinition.mustSupport` | `None` if falsy |
| `sliceName` | `str` | `ElementDefinition.sliceName` | `None` if falsy |
| `suppress` | `list[str]` | — | If any value matches `self.applies_to`, the node is skipped entirely |
| `proceedingEd` | `list[dict]` | — | Extended directly into `elements` *before* the main element is appended |
| `slicing` | `dict` (child) | → `process_slicing()` | See **Slicing** below |
| `pattern` | `dict` (child) | → `setattr(el, f"pattern{type}", detail)` | See **Pattern** below |
| `type` | `list[dict]` | `ElementDefinition.type` | Raw FHIR `ElementDefinitionType` list |
| `constraint` | `list[dict]` | `ElementDefinition.constraint` | Raw FHIR `ElementDefinitionConstraint` list |
| `binding` | `dict` | `ElementDefinition.binding` | Raw FHIR `ElementDefinitionBinding` |
| `mapping` | `list[dict]` | `ElementDefinition.mapping` | Raw FHIR `ElementDefinitionMapping` list |

#### Child: `slicing`

Consumed by `process_slicing()`, which builds a **separate** `ElementDefinition`
(the slicing "header" element) and raises if the same `path` is sliced twice.

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `path` | `str` | `ElementDefinition.path` | Falls back to the parent node's `path` |
| `id` | `str` | `ElementDefinition.id` | Falls back to the parent node's `path` |
| `rules` | `str` | `ElementDefinitionSlicing.rules` | Defaults to `"open"` (FHIR values: `open`, `closed`, `openAtEnd`) |
| `ordered` | `bool` | `ElementDefinitionSlicing.ordered` | Defaults to `False` |
| `discriminator` | `list[dict]` | `ElementDefinitionSlicing.discriminator` | Cast to `list[ElementDefinitionSlicingDiscriminator]` |
| `min` | `int` | `ElementDefinition.min` | On the slicing header element itself |
| `max` | `str` | `ElementDefinition.max` | On the slicing header element itself |

#### Child: `pattern`

Applied with a dynamic attribute name — not a fixed FHIR field, but FHIR's
`pattern[x]` polymorphic field.

| Field | Type | Effect |
|---|---|---|
| `type` | `str` | Suffix appended to `pattern`, e.g. `"CodeableConcept"` → `patternCodeableConcept` |
| `detail` | `any` | Value assigned to that dynamic attribute |

#### Child: `type`, `constraint`, `binding`, `mapping`

These are passed through largely as-is into the corresponding `ElementDefinition`
attribute — the generator doesn't reshape them, so their structure is whatever the
standard FHIR R4B `ElementDefinition` schema expects for `type`, `constraint`,
`binding`, and `mapping` respectively.
```

Paste that into your README under whichever heading fits — it's self-contained and renders the same tables and structure shown above. Let me know if you'd rather I generate this as an actual `.md` file or merge it into an existing README section.

Question: I want actual markdown not this already generated view

Response: Here it is as raw text — copy everything between the lines below exactly as-is:

```
## `structureDef` Node Reference

The `structureDef` node is read from the source YAML inside each top-level node
(`node.get("structureDef")`) and processed in `process_node()`. This section
documents every field it supports, including its nested children.

### Top-Level YAML Node

The outer object read from the source YAML. `structureDef` is one key on it.

| Field | Type | Used For |
|---|---|---|
| `profiles` | `list[str]` | Filters which nodes apply to `self.profile` |
| `structureDef` | `dict` | The element definition payload (see below) |
| `appliesTo` | `list[str]` | Filters nodes against `self.applies_to` |
| `sources` | `list[str]` | Filters nodes against `self.sources` |
| `sourceColumn` | `str` | Becomes `column_name`; falls back to `inputPath` parsing if absent |
| `inputPath` | `str` | Dotted path parsed by `parse_path()`; becomes `el.short` |

### `structureDef` Fields

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `id` | `str` | `ElementDefinition.id` | |
| `path` | `str` | `ElementDefinition.path` | Also drives slicing/sorting lookups |
| `min` | `int` | `ElementDefinition.min` | Defaults to `0` |
| `max` | `str` | `ElementDefinition.max` | Defaults to `"1"` |
| `isSummary` | `bool` | `ElementDefinition.isSummary` | `None` if falsy |
| `mustSupport` | `bool` | `ElementDefinition.mustSupport` | `None` if falsy |
| `sliceName` | `str` | `ElementDefinition.sliceName` | `None` if falsy |
| `suppress` | `list[str]` | — | If any value matches `self.applies_to`, the node is skipped entirely |
| `proceedingEd` | `list[dict]` | — | Extended directly into `elements` *before* the main element is appended |
| `slicing` | `dict` (child) | → `process_slicing()` | See **Slicing** below |
| `pattern` | `dict` (child) | → `setattr(el, f"pattern{type}", detail)` | See **Pattern** below |
| `type` | `list[dict]` | `ElementDefinition.type` | Raw FHIR `ElementDefinitionType` list |
| `constraint` | `list[dict]` | `ElementDefinition.constraint` | Raw FHIR `ElementDefinitionConstraint` list |
| `binding` | `dict` | `ElementDefinition.binding` | Raw FHIR `ElementDefinitionBinding` |
| `mapping` | `list[dict]` | `ElementDefinition.mapping` | Raw FHIR `ElementDefinitionMapping` list |

#### Child: `slicing`

Consumed by `process_slicing()`, which builds a **separate** `ElementDefinition`
(the slicing "header" element) and raises if the same `path` is sliced twice.

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `path` | `str` | `ElementDefinition.path` | Falls back to the parent node's `path` |
| `id` | `str` | `ElementDefinition.id` | Falls back to the parent node's `path` |
| `rules` | `str` | `ElementDefinitionSlicing.rules` | Defaults to `"open"` (FHIR values: `open`, `closed`, `openAtEnd`) |
| `ordered` | `bool` | `ElementDefinitionSlicing.ordered` | Defaults to `False` |
| `discriminator` | `list[dict]` | `ElementDefinitionSlicing.discriminator` | Cast to `list[ElementDefinitionSlicingDiscriminator]` |
| `min` | `int` | `ElementDefinition.min` | On the slicing header element itself |
| `max` | `str` | `ElementDefinition.max` | On the slicing header element itself |

#### Child: `pattern`

Applied with a dynamic attribute name — not a fixed FHIR field, but FHIR's
`pattern[x]` polymorphic field.

| Field | Type | Effect |
|---|---|---|
| `type` | `str` | Suffix appended to `pattern`, e.g. `"CodeableConcept"` → `patternCodeableConcept` |
| `detail` | `any` | Value assigned to that dynamic attribute |

#### Child: `type`, `constraint`, `binding`, `mapping`

These are passed through largely as-is into the corresponding `ElementDefinition`
attribute — the generator doesn't reshape them, so their structure is whatever the
standard FHIR R4B `ElementDefinition` schema expects for `type`, `constraint`,
`binding`, and `mapping` respectively.
```

That block is the literal markdown source — paste it directly into your README file and it will render into the headers and tables shown earlier. If you'd rather have an actual `.docx`/file artifact generated instead of inline text, let me know, but for a README you'd want this raw text saved into the `.md` file itself.

Question: can you make this a simple copy of markdown from a code block

Response: ```markdown
## `structureDef` Node Reference

The `structureDef` node is read from the source YAML inside each top-level node
(`node.get("structureDef")`) and processed in `process_node()`. This section
documents every field it supports, including its nested children.

### Top-Level YAML Node

The outer object read from the source YAML. `structureDef` is one key on it.

| Field | Type | Used For |
|---|---|---|
| `profiles` | `list[str]` | Filters which nodes apply to `self.profile` |
| `structureDef` | `dict` | The element definition payload (see below) |
| `appliesTo` | `list[str]` | Filters nodes against `self.applies_to` |
| `sources` | `list[str]` | Filters nodes against `self.sources` |
| `sourceColumn` | `str` | Becomes `column_name`; falls back to `inputPath` parsing if absent |
| `inputPath` | `str` | Dotted path parsed by `parse_path()`; becomes `el.short` |

### `structureDef` Fields

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `id` | `str` | `ElementDefinition.id` | |
| `path` | `str` | `ElementDefinition.path` | Also drives slicing/sorting lookups |
| `min` | `int` | `ElementDefinition.min` | Defaults to `0` |
| `max` | `str` | `ElementDefinition.max` | Defaults to `"1"` |
| `isSummary` | `bool` | `ElementDefinition.isSummary` | `None` if falsy |
| `mustSupport` | `bool` | `ElementDefinition.mustSupport` | `None` if falsy |
| `sliceName` | `str` | `ElementDefinition.sliceName` | `None` if falsy |
| `suppress` | `list[str]` | — | If any value matches `self.applies_to`, the node is skipped entirely |
| `proceedingEd` | `list[dict]` | — | Extended directly into `elements` *before* the main element is appended |
| `slicing` | `dict` (child) | → `process_slicing()` | See **Slicing** below |
| `pattern` | `dict` (child) | → `setattr(el, f"pattern{type}", detail)` | See **Pattern** below |
| `type` | `list[dict]` | `ElementDefinition.type` | Raw FHIR `ElementDefinitionType` list |
| `constraint` | `list[dict]` | `ElementDefinition.constraint` | Raw FHIR `ElementDefinitionConstraint` list |
| `binding` | `dict` | `ElementDefinition.binding` | Raw FHIR `ElementDefinitionBinding` |
| `mapping` | `list[dict]` | `ElementDefinition.mapping` | Raw FHIR `ElementDefinitionMapping` list |

#### Child: `slicing`

Consumed by `process_slicing()`, which builds a **separate** `ElementDefinition`
(the slicing "header" element) and raises if the same `path` is sliced twice.

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `path` | `str` | `ElementDefinition.path` | Falls back to the parent node's `path` |
| `id` | `str` | `ElementDefinition.id` | Falls back to the parent node's `path` |
| `rules` | `str` | `ElementDefinitionSlicing.rules` | Defaults to `"open"` (FHIR values: `open`, `closed`, `openAtEnd`) |
| `ordered` | `bool` | `ElementDefinitionSlicing.ordered` | Defaults to `False` |
| `discriminator` | `list[dict]` | `ElementDefinitionSlicing.discriminator` | Cast to `list[ElementDefinitionSlicingDiscriminator]` |
| `min` | `int` | `ElementDefinition.min` | On the slicing header element itself |
| `max` | `str` | `ElementDefinition.max` | On the slicing header element itself |

#### Child: `pattern`

Applied with a dynamic attribute name — not a fixed FHIR field, but FHIR's
`pattern[x]` polymorphic field.

| Field | Type | Effect |
|---|---|---|
| `type` | `str` | Suffix appended to `pattern`, e.g. `"CodeableConcept"` → `patternCodeableConcept` |
| `detail` | `any` | Value assigned to that dynamic attribute |

#### Child: `type`, `constraint`, `binding`, `mapping`

These are passed through largely as-is into the corresponding `ElementDefinition`
attribute — the generator doesn't reshape them, so their structure is whatever the
standard FHIR R4B `ElementDefinition` schema expects for `type`, `constraint`,
`binding`, and `mapping` respectively.
```

Question: put it in a file

Response: I don't have a tool that outputs a plain `.md` file directly — my document-creation tools only produce Word (`.docx`), PowerPoint, or HTML dashboard files, not raw markdown.

The most reliable path for a README addition is to copy the code block from my last message and paste it into your `README.md` file yourself — that's already the exact markdown source, unmodified.

If it'd help instead, I can generate this as a Word document you could download and convert, but that adds a conversion step you don't need for a README. Let me know if you'd like that anyway, or if there's another way you'd like to receive it.

Question: the why do you keep not giving me the mardown code you give me an intro then rendered

Response: ```markdown
## `structureDef` Node Reference

The `structureDef` node is read from the source YAML inside each top-level node
(`node.get("structureDef")`) and processed in `process_node()`. This section
documents every field it supports, including its nested children.

### Top-Level YAML Node

The outer object read from the source YAML. `structureDef` is one key on it.

| Field | Type | Used For |
|---|---|---|
| `profiles` | `list[str]` | Filters which nodes apply to `self.profile` |
| `structureDef` | `dict` | The element definition payload (see below) |
| `appliesTo` | `list[str]` | Filters nodes against `self.applies_to` |
| `sources` | `list[str]` | Filters nodes against `self.sources` |
| `sourceColumn` | `str` | Becomes `column_name`; falls back to `inputPath` parsing if absent |
| `inputPath` | `str` | Dotted path parsed by `parse_path()`; becomes `el.short` |

### `structureDef` Fields

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `id` | `str` | `ElementDefinition.id` | |
| `path` | `str` | `ElementDefinition.path` | Also drives slicing/sorting lookups |
| `min` | `int` | `ElementDefinition.min` | Defaults to `0` |
| `max` | `str` | `ElementDefinition.max` | Defaults to `"1"` |
| `isSummary` | `bool` | `ElementDefinition.isSummary` | `None` if falsy |
| `mustSupport` | `bool` | `ElementDefinition.mustSupport` | `None` if falsy |
| `sliceName` | `str` | `ElementDefinition.sliceName` | `None` if falsy |
| `suppress` | `list[str]` | — | If any value matches `self.applies_to`, the node is skipped entirely |
| `proceedingEd` | `list[dict]` | — | Extended directly into `elements` *before* the main element is appended |
| `slicing` | `dict` (child) | → `process_slicing()` | See **Slicing** below |
| `pattern` | `dict` (child) | → `setattr(el, f"pattern{type}", detail)` | See **Pattern** below |
| `type` | `list[dict]` | `ElementDefinition.type` | Raw FHIR `ElementDefinitionType` list |
| `constraint` | `list[dict]` | `ElementDefinition.constraint` | Raw FHIR `ElementDefinitionConstraint` list |
| `binding` | `dict` | `ElementDefinition.binding` | Raw FHIR `ElementDefinitionBinding` |
| `mapping` | `list[dict]` | `ElementDefinition.mapping` | Raw FHIR `ElementDefinitionMapping` list |

#### Child: `slicing`

Consumed by `process_slicing()`, which builds a **separate** `ElementDefinition`
(the slicing "header" element) and raises if the same `path` is sliced twice.

| Field | Type | Maps To | Notes |
|---|---|---|---|
| `path` | `str` | `ElementDefinition.path` | Falls back to the parent node's `path` |
| `id` | `str` | `ElementDefinition.id` | Falls back to the parent node's `path` |
| `rules` | `str` | `ElementDefinitionSlicing.rules` | Defaults to `"open"` (FHIR values: `open`, `closed`, `openAtEnd`) |
| `ordered` | `bool` | `ElementDefinitionSlicing.ordered` | Defaults to `False` |
| `discriminator` | `list[dict]` | `ElementDefinitionSlicing.discriminator` | Cast to `list[ElementDefinitionSlicingDiscriminator]` |
| `min` | `int` | `ElementDefinition.min` | On the slicing header element itself |
| `max` | `str` | `ElementDefinition.max` | On the slicing header element itself |

#### Child: `pattern`

Applied with a dynamic attribute name — not a fixed FHIR field, but FHIR's
`pattern[x]` polymorphic field.

| Field | Type | Effect |
|---|---|---|
| `type` | `str` | Suffix appended to `pattern`, e.g. `"CodeableConcept"` → `patternCodeableConcept` |
| `detail` | `any` | Value assigned to that dynamic attribute |

#### Child: `type`, `constraint`, `binding`, `mapping`

These are passed through largely as-is into the corresponding `ElementDefinition`
attribute — the generator doesn't reshape them, so their structure is whatever the
standard FHIR R4B `ElementDefinition` schema expects for `type`, `constraint`,
`binding`, and `mapping` respectively.
