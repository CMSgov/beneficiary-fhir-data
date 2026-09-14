import json
from typing import Any
from unittest import mock

import pytest

from app.main import AlreadyRunningResult, PipelineStartedResult, result_handler

TASK_ARN = "arn:aws:ecs:us-east-1:123456789012:task/test-cluster/test-task"


@pytest.fixture(autouse=True)
def lambda_settings(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("BFD_ENVIRONMENT", "test")
    monkeypatch.setenv("ECS_CLUSTER_ARN", "arn:aws:ecs:us-east-1:123456789012:cluster/test")
    monkeypatch.setenv("IDR_TASK_DEFINITION_ARN", "idr-pipeline:1")
    monkeypatch.setenv("IDR_CONTAINER_NAME", "idr-pipeline")
    monkeypatch.setenv("IDR_TASK_GROUP", "idr-pipeline")
    monkeypatch.setenv("IDR_TASK_SUBNET_IDS_JSON", json.dumps(["subnet-test"]))
    monkeypatch.setenv("IDR_TASK_SECURITY_GROUP_ID", "sg-test")
    monkeypatch.setenv("IDR_JOB_ID", "1")
    monkeypatch.setenv(
        "IDR_TASK_CAPACITY_PROVIDER_STRATEGIES_JSON",
        json.dumps([{"capacity_provider": "FARGATE", "weight": 1, "base": 0}]),
    )
    monkeypatch.setenv("IDR_TASK_SCHEDULES_GROUP", "test-schedules")
    monkeypatch.setenv("IDR_TASK_SCHEDULER_ROLE_ARN", "arn:aws:iam::123456789012:role/test")


def active_task(job_id: str) -> dict[str, Any]:
    return {
        "taskArn": TASK_ARN,
        "group": "idr-pipeline",
        "desiredStatus": "RUNNING",
        "overrides": {
            "containerOverrides": [
                {
                    "name": "idr-pipeline",
                    "environment": [{"name": "IDR_JOB_ID", "value": job_id}],
                }
            ]
        },
    }


def test_no_running_tasks_starts_default_pipeline() -> None:
    ecs_client = mock.Mock()
    ecs_client.list_tasks.return_value = {"taskArns": []}
    ecs_client.run_task.return_value = {"tasks": [{"taskArn": "new-task"}]}

    with mock.patch("app.main.boto3.client", return_value=ecs_client):
        result = result_handler({}, mock.Mock())

    assert result == PipelineStartedResult(task_arn="new-task")
    ecs_client.run_task.assert_called_once()
    assert "overrides" not in ecs_client.run_task.call_args.kwargs


def test_matching_second_job_id_does_start_second_pipeline() -> None:
    ecs_client = mock.Mock()
    ecs_client.list_tasks.return_value = {"taskArns": [TASK_ARN]}
    ecs_client.describe_tasks.return_value = {"tasks": [active_task("1")]}
    ecs_client.run_task.return_value = {"tasks": [{"taskArn": "second-job-task"}]}

    with mock.patch("app.main.boto3.client", return_value=ecs_client):
        result = result_handler({"env": {"IDR_JOB_ID": "2"}}, mock.Mock())

    assert result == PipelineStartedResult(task_arn="second-job-task")
    ecs_client.run_task.assert_called_once()
    assert ecs_client.run_task.call_args.kwargs["overrides"] == {
        "containerOverrides": [
            {
                "name": "idr-pipeline",
                "environment": [{"name": "IDR_JOB_ID", "value": "2"}],
            }
        ]
    }


def test_default_job_id_does_not_start_second_time_when_already_running() -> None:
    ecs_client = mock.Mock()
    ecs_client.list_tasks.return_value = {"taskArns": [TASK_ARN]}
    ecs_client.describe_tasks.return_value = {"tasks": [active_task("1")]}

    with mock.patch("app.main.boto3.client", return_value=ecs_client):
        result = result_handler({"env": {"IDR_JOB_ID": "1"}}, mock.Mock())

    assert result == AlreadyRunningResult(running_task_arns=[TASK_ARN])
    ecs_client.run_task.assert_not_called()


def test_secondary_job_id_does_not_start_second_time_when_already_running() -> None:
    ecs_client = mock.Mock()
    ecs_client.list_tasks.return_value = {"taskArns": [TASK_ARN]}
    ecs_client.describe_tasks.return_value = {"tasks": [active_task("2")]}

    with mock.patch("app.main.boto3.client", return_value=ecs_client):
        result = result_handler({"env": {"IDR_JOB_ID": "2"}}, mock.Mock())

    assert result == AlreadyRunningResult(running_task_arns=[TASK_ARN])
    ecs_client.run_task.assert_not_called()


def test_seondary_running_and_default_starts() -> None:
    ecs_client = mock.Mock()
    ecs_client.list_tasks.return_value = {"taskArns": [TASK_ARN]}
    ecs_client.describe_tasks.return_value = {"tasks": [active_task("2")]}
    ecs_client.run_task.return_value = {"tasks": [{"taskArn": "new-default-task"}]}

    with mock.patch("app.main.boto3.client", return_value=ecs_client):
        result = result_handler({}, mock.Mock())

    assert result == PipelineStartedResult(task_arn="new-default-task")
    ecs_client.run_task.assert_called_once()
    assert "overrides" not in ecs_client.run_task.call_args.kwargs
