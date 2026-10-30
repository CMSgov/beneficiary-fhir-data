import json
from pathlib import Path


class FHIRDefinitions:
    """Load StructureDefinitions from official FHIR bundles."""

    def __init__(self):
        self.by_id: dict[str, dict] = {}
        self.__load()

    def __load(self) -> None:
        script_dir = Path(__file__).resolve().parent
        for path in ["R4-def/profiles-resources.json", "R4-def/profiles-types.json"]:
            full_path = script_dir / path
            with full_path.open(encoding="utf-8") as f:
                bundle = json.load(f)
            for entry in bundle.get("entry", []):
                sd = entry.get("resource", {})
                if sd.get("resourceType") == "StructureDefinition":
                    self.by_id.setdefault(sd["id"], sd)

    def __is_primitive_type(self, type_code: str) -> bool:
        """Determine if a FHIR primative type."""
        return type_code[:1].islower()

    def __elements_for(self, type_name: str) -> list[dict[str, str]] | None:
        sd = self.by_id.get(type_name)
        if not sd:
            return None
        snap = sd.get("snapshot") or sd.get("differential")
        return snap["element"] if snap else []

    def __build_children_index(self, elements: list[dict]) -> dict[str, list[dict[str, str]]]:
        """Map a definition path to its direct children, preserving the snapshot's base order."""
        index: dict[str, list[dict]] = {}
        for el in elements:
            path = el["path"]
            if "." in path:
                parent = path.rsplit(".", 1)[0]
                index.setdefault(parent, []).append(el)
        return index

    def __expand_children(
        self,
        children_index: dict[str, list[dict]],
        def_path: str,
        cur_path: str,
        type_chain: set,
        depth: int,
        max_depth: int,
        out: list[str],
    ) -> None:
        if depth > max_depth:
            return
        for child in children_index.get(def_path, []):
            segment = child["path"].rsplit(".", 1)[-1]
            child_cur_path = f"{cur_path}.{segment}"

            # Recursive backbone elements (e.g. Questionnaire.item) reuse an
            # earlier element's subtree instead of declaring a "type".
            if "contentReference" in child:
                ref = child["contentReference"].lstrip("#")
                out.append(child_cur_path)
                if ref not in type_chain:
                    self.__expand_children(
                        children_index,
                        ref,
                        child_cur_path,
                        type_chain | {ref},
                        depth + 1,
                        max_depth,
                        out,
                    )
                continue

            types = child.get("type", [])
            if not types:
                # Inline backbone element - children live in the same snapshot.
                out.append(child_cur_path)
                self.__expand_children(
                    children_index,
                    child["path"],
                    child_cur_path,
                    type_chain,
                    depth + 1,
                    max_depth,
                    out,
                )
                continue

            for t in types:
                code = t.get("code", "")
                if child_cur_path.endswith("[x]"):
                    # polymorphic element, e.g. value[x] -> valueQuantity
                    emitted_path = child_cur_path[:-3] + code[0].upper() + code[1:]
                else:
                    emitted_path = child_cur_path

                out.append(emitted_path)

                if self.__is_primitive_type(code) or code in type_chain:
                    continue  # leaf, or already expanding this type (avoids
                    # infinite loops e.g. Extension.extension)

                sub_elements = self.__elements_for(code)
                if not sub_elements:
                    continue  # unresolved type (external profile, etc.)

                sub_children = self.__build_children_index(sub_elements)
                sub_root = sub_elements[0]["path"]
                self.__expand_children(
                    sub_children,
                    sub_root,
                    emitted_path,
                    type_chain | {code},
                    depth + 1,
                    max_depth,
                    out,
                )

    def expand_type_paths(self, resource_type: str, max_depth: int = 8) -> list[str]:
        """List resource_type paths in definition order, expanding referenced types in place."""
        elements = self.__elements_for(resource_type)
        if elements is None:
            raise ValueError(f"No StructureDefinition found for {resource_type!r}")

        children_index = self.__build_children_index(elements)
        root_path = elements[0]["path"]
        out: list[str] = [root_path]
        self.__expand_children(
            children_index,
            root_path,
            root_path,
            set({resource_type}),
            0,
            max_depth,
            out,
        )
        return out
