#!python3
# renames a given migration script to a timestamped filename

import datetime
import math
import os
import sys

if len(sys.argv) < 2:
    print("Usage:")
    print("rename-migration-script.py [filename]")
    sys.exit()
fn = sys.argv[1]
basename_array = os.path.basename(fn).split("__")
if len(basename_array) < 2:
    print("missing __ from filename.")
    sys.exit()
description = basename_array[1]
path = os.path.dirname(fn)
ms = math.floor(int(datetime.datetime.now().strftime("%f")) / 1000)
new_fn = datetime.datetime.now().strftime(f"V%Y%m%d%H%M%S{str(ms).zfill(3)}__{description}")
new_fn = f"{path}/{new_fn}"
os.rename(fn, new_fn)
print(f"Renamed {fn} to {new_fn}")
