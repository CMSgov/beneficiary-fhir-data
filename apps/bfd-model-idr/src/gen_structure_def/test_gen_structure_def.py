import subprocess
from pathlib import Path
from subprocess import CompletedProcess


def _run_subprocess(args: list[str]) -> None:
    result: CompletedProcess[bytes] = subprocess.run(
        args,
        cwd=Path(__file__).parent.parent.parent,
        capture_output=True,
        check=False,
    )

    assert result.returncode == 0, result.stderr.decode()


def test_gen_dd_runs_successfully() -> None:
    _run_subprocess(args=["uv", "run", "gen-structure-def"])
