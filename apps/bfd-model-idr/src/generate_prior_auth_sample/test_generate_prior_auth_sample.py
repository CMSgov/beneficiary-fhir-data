import subprocess
from pathlib import Path


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
            "../bfd-pipeline-idr/test_samples1",
        ],
        cwd=Path(__file__).parent.parent.parent,
        capture_output=True,
        check=False,
    )

    assert result.returncode == 0, result.stderr.decode()
