import subprocess
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[3]
SOURCE_DIR = str(PROJECT_ROOT / "bfd-pipeline-idr" / "test_samples1")


def test_generate_prior_auth_sample_runs_successfully() -> None:
    result = subprocess.run(
        [
            "uv",
            "run",
            "generate-prior-auth-sample",
            "--utn",
            "-X3QFSBY07ICRD",
            "--output-directory",
            "./out-test",
            "--source-directory",
            SOURCE_DIR,
        ],
        cwd=Path(__file__).parent.parent.parent,
        capture_output=True,
        check=False,
    )

    assert result.returncode == 0, result.stderr.decode()
