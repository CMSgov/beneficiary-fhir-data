import subprocess
from pathlib import Path


def test_gen_dd_runs_successfully() -> None:
    result = subprocess.run(
        ["uv", "run", "gen_dd"],
        cwd=Path(__file__).parent.parent.parent,
        capture_output=True,
        check=False,
    )

    assert result.returncode == 0, result.stderr.decode()
