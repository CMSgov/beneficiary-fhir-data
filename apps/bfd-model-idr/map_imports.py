# Get referenced maps from the source map file
import json
import sys
from pathlib import Path


def get_referenced_maps(compiled_map_path):
    try:
        with Path(compiled_map_path).open() as f:
            map_data = json.load(f)

        referenced_maps = set()
        if "import" in map_data:
            for imp in map_data["import"]:
                map_name = "maps/" + imp.split("/")[-1] + ".map"
                referenced_maps.add(map_name)

        return referenced_maps
    except Exception as e:
        print("Error reading map:", e)
        return set()


referenced_maps = get_referenced_maps(sys.argv[1])
map_imports = " ".join([f"-ig {map_file}" for map_file in referenced_maps])
print(map_imports)
