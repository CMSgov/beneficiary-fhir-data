import subprocess
from pathlib import Path


def _run_subprocess(args: list[str]):
    result = subprocess.run(
        args,
        cwd=Path(__file__).parent.parent.parent,
        capture_output=True,
        check=False,
    )

    assert result.returncode == 0, result.stderr.decode()


def test_gen_dd_runs_successfully() -> None:
    _run_subprocess(["npm", "install"])
    _run_subprocess(["npm", "run", "sushi-build"])
    _run_subprocess(["uv", "run", "gen-dd"])
