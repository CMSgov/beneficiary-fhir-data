import calendar
import json
from base64 import b64encode
from collections.abc import Generator
from datetime import UTC, datetime
from io import StringIO
from typing import Any
from unittest import mock

import paramiko
import pytest

from errors import (
    InvalidObjectKeyError,
    InvalidPendingDirError,
    SFTPConnectionError,
    SFTPTransferError,
    UnknownPartnerError,
    UnrecognizedFileError,
)
from sftp_outbound_transfer import handler
from sns import (
    FileDiscoveredDetails,
    StatusNotification,
    TransferFailedDetails,
    TransferSuccessDetails,
    UnknownErrorDetails,
    send_notification,
)
from ssm import GlobalSsmConfig, HostKey, PartnerSsmConfig, RecognizedFile

DEFAULT_MOCK_EVENT_TIME_ISO = "2024-02-12T00:00:00Z"
DEFAULT_MOCK_EVENT_TIME_DATETIME = datetime.fromisoformat(
    DEFAULT_MOCK_EVENT_TIME_ISO.removesuffix("Z")
).replace(tzinfo=UTC)
DEFAULT_MOCK_EVENT_TIME_UTC_TIMESTAMP = calendar.timegm(
    DEFAULT_MOCK_EVENT_TIME_DATETIME.utctimetuple()
)
DEFAULT_MOCK_EVENT_NAME = "ObjectCreated:Put"
DEFAULT_MOCK_PARTNER_NAME = "partner"
DEFAULT_MOCK_PARTNER_HOME_DIR = "partner_home_dir"
DEFAULT_MOCK_BFD_ENVIRONMENT = "mock"
DEFAULT_MOCK_BUCKET = "mock-bucket"
DEFAULT_MOCK_BUCKET_ROOT_DIR = "mock_root"
DEFAULT_MOCK_PENDING_DIR = "out"
DEFAULT_MOCK_OBJECT_FILENAME = "file"
DEFAULT_MOCK_OBJECT_KEY = f"{DEFAULT_MOCK_BUCKET_ROOT_DIR}/{DEFAULT_MOCK_PARTNER_HOME_DIR}/{DEFAULT_MOCK_PENDING_DIR}/{DEFAULT_MOCK_OBJECT_FILENAME}"  # noqa: E501
DEFAULT_MOCK_BFD_SNS_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:mock-topic"
DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER = {
    f"{DEFAULT_MOCK_PARTNER_NAME}": f"arn:aws:sns:us-east-1:123456789012:mock-topic-{DEFAULT_MOCK_PARTNER_NAME}"  # noqa: E501
}
DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER_JSON = json.dumps(DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER)
DEFAULT_MOCK_GLOBAL_CONFIG = GlobalSsmConfig(
    sftp_connect_timeout=10,
    sftp_hostname="test",
    sftp_username="test",
    sftp_host_pub_keys=[
        HostKey.model_validate({"key_type": "ignore", "key_base64": "aWdub3JlCg=="})
    ],
    sftp_user_priv_key="test",
    enrolled_partners=[DEFAULT_MOCK_PARTNER_NAME],
    home_dirs_to_partner={f"{DEFAULT_MOCK_PARTNER_HOME_DIR}": DEFAULT_MOCK_PARTNER_NAME},
)
DEFAULT_MOCK_PARTNER_CONFIG = PartnerSsmConfig(
    partner=DEFAULT_MOCK_PARTNER_NAME,
    bucket_home_dir=DEFAULT_MOCK_PARTNER_HOME_DIR,
    pending_files_dir=DEFAULT_MOCK_PENDING_DIR,
    bucket_home_path=f"{DEFAULT_MOCK_BUCKET_ROOT_DIR}/{DEFAULT_MOCK_PARTNER_HOME_DIR}",
    pending_files_full_path=f"{DEFAULT_MOCK_BUCKET_ROOT_DIR}/{DEFAULT_MOCK_PARTNER_HOME_DIR}/{DEFAULT_MOCK_PENDING_DIR}",
    recognized_files=[
        RecognizedFile(
            type="test",
            filename_pattern="^file$",
            staging_folder="/staging",
            input_folder="/input",
        )
    ],
)
MODULE_UNDER_TEST = "sftp_outbound_transfer"
SEND_NOTIFICATION_PATCH_PATH = f"{MODULE_UNDER_TEST}.{send_notification.__name__}"
GLOBAL_SSM_CONFIG_PATCH_PATH = f"{MODULE_UNDER_TEST}.{GlobalSsmConfig.__name__}"
PARTNER_SSM_CONFIG_PATCH_PATH = f"{MODULE_UNDER_TEST}.{PartnerSsmConfig.__name__}"
PARAMIKO_PATCH_PATH = f"{MODULE_UNDER_TEST}.{paramiko.__name__}"

mock_boto3_client = mock.Mock()
mock_boto3_resource = mock.Mock()


def mock_new_topic(topic_arn: str) -> str:
    return topic_arn


"""Lambda that modifies the Topic function to return a string representing the Topic ARN passed to
the function for use in tests so that the Topic can be discerned for a given notification and
asserted upon"""
mock_boto3_resource.Topic.side_effect = mock_new_topic
mock_boto3_resource_func = mock.Mock(return_value=mock_boto3_resource)
mock_lambda_context = mock.Mock()
mock_global_ssm = mock.Mock()
mock_partner_ssm = mock.Mock()
mock_paramiko = mock.Mock()
mock_ssh_client = mock.Mock()
mock_sftp_client = mock.Mock()
mock_ssh_client_open_sftp_func = mock.MagicMock()
mock_ssh_client_func = mock.MagicMock()
mock_send_notification = mock.Mock()


def get_mock_send_notification_calls() -> list[tuple[str, StatusNotification]]:
    return [
        (str(x.kwargs["topic"]), x.kwargs["notification"])
        for x in mock_send_notification.call_args_list
    ]


def generate_event(
    key: str = DEFAULT_MOCK_OBJECT_KEY,
    event_time_iso: str = DEFAULT_MOCK_EVENT_TIME_ISO,
    event_name: str = DEFAULT_MOCK_EVENT_NAME,
) -> dict[str, Any]:
    return {
        "Records": [
            {
                "Sns": {
                    "Message": json.dumps(
                        {
                            "Records": [
                                {
                                    "eventName": event_name,
                                    "eventTime": event_time_iso,
                                    "s3": {"object": {"key": key}},
                                }
                            ]
                        }
                    )
                }
            }
        ]
    }


@mock.patch(f"{MODULE_UNDER_TEST}.boto3.client", new=mock_boto3_client)
@mock.patch(f"{MODULE_UNDER_TEST}.boto3.resource", new=mock_boto3_resource_func)
@mock.patch(f"{MODULE_UNDER_TEST}.BFD_ENVIRONMENT", DEFAULT_MOCK_BFD_ENVIRONMENT)
@mock.patch(f"{MODULE_UNDER_TEST}.BUCKET", DEFAULT_MOCK_BUCKET)
@mock.patch(f"{MODULE_UNDER_TEST}.BUCKET_ROOT_DIR", DEFAULT_MOCK_BUCKET_ROOT_DIR)
@mock.patch(f"{MODULE_UNDER_TEST}.BFD_SNS_TOPIC_ARN", DEFAULT_MOCK_BFD_SNS_TOPIC_ARN)
@mock.patch(
    f"{MODULE_UNDER_TEST}.SNS_TOPIC_ARNS_BY_PARTNER_JSON",
    DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER_JSON,
)
@mock.patch(GLOBAL_SSM_CONFIG_PATCH_PATH, new=mock_global_ssm)
@mock.patch(PARTNER_SSM_CONFIG_PATCH_PATH, new=mock_partner_ssm)
@mock.patch(PARAMIKO_PATCH_PATH, new=mock_paramiko)
@mock.patch(SEND_NOTIFICATION_PATCH_PATH, new=mock_send_notification)
class TestUpdatePipelineSlisHandler:
    @pytest.fixture(autouse=True)
    def run_before_and_after_tests(self) -> Generator[Any, Any, Any]:
        # Reset global/partner SSM config default return values before each test
        mock_global_ssm.from_ssm.return_value = DEFAULT_MOCK_GLOBAL_CONFIG
        mock_partner_ssm.from_ssm.return_value = DEFAULT_MOCK_PARTNER_CONFIG
        # Reset context manager mocking before each test
        mock_ssh_client_open_sftp_func.__enter__.return_value = mock_sftp_client
        mock_ssh_client.open_sftp.return_value = mock_ssh_client_open_sftp_func
        mock_ssh_client_func.__enter__.return_value = mock_ssh_client
        mock_paramiko.SSHClient.return_value = mock_ssh_client_func
        mock_sftp_client.open = mock.MagicMock()
        yield
        # Remove modifications to mocked objects returned by above context manager mocking after
        # each test
        mock_ssh_client.reset_mock(side_effect=True, return_value=True)
        mock_sftp_client.reset_mock(side_effect=True, return_value=True)
        # Reset other mocks, as well
        mock_boto3_client.reset_mock(side_effect=True, return_value=True)
        mock_global_ssm.reset_mock(side_effect=True, return_value=True)
        mock_partner_ssm.reset_mock(side_effect=True, return_value=True)
        mock_send_notification.reset_mock(side_effect=True, return_value=True)

    @pytest.mark.parametrize(
        ("event", "expected_error"),
        [
            (None, TypeError),
            ("", TypeError),
            ({"Records": []}, ValueError),
            ({"Records": ""}, ValueError),
            ({"Records": [""]}, TypeError),
            ({"Records": [{"OtherKey": ""}]}, KeyError),
            ({"Records": [{"Sns": {"OtherKey": ""}}]}, KeyError),
            ({"Records": [{"Sns": {"Message": "invalid"}}]}, json.JSONDecodeError),
            ({"Records": [{"Sns": {"Message": {"NotJsonStr": ""}}}]}, TypeError),
            ({"Records": [{"Sns": {"Message": "{}"}}]}, KeyError),
            ({"Records": [{"Sns": {"Message": '{"Records":[]}'}}]}, ValueError),
            ({"Records": [{"Sns": {"Message": '{"Records":""}'}}]}, ValueError),
            ({"Records": [{"Sns": {"Message": '{"Records":[{}]}'}}]}, KeyError),
            (generate_event(event_name="invalid"), ValueError),
            (
                {
                    "Records": [
                        {
                            "Sns": {
                                "Message": json.dumps(
                                    {"Records": [{"eventName": DEFAULT_MOCK_EVENT_NAME}]}
                                )
                            }
                        }
                    ]
                },
                KeyError,
            ),
            (generate_event(event_time_iso="24-01-19T00:00:00Z"), ValueError),
            (
                {
                    "Records": [
                        {
                            "Sns": {
                                "Message": json.dumps(
                                    {
                                        "Records": [
                                            {
                                                "eventName": DEFAULT_MOCK_EVENT_NAME,
                                                "eventTime": DEFAULT_MOCK_EVENT_TIME_ISO,
                                                "s3": {"object": {}},
                                            }
                                        ]
                                    }
                                )
                            }
                        }
                    ]
                },
                KeyError,
            ),
        ],
    )
    def test_it_raises_exception_and_sends_notification_if_event_is_invalid(
        self,
        event: Any,  # noqa: ANN401
        expected_error: type[Exception],
    ) -> None:
        with pytest.raises(expected_error):
            handler(event=event, context=mock_lambda_context)

        assert mock_send_notification.call_count == 1

        actual_topic_arn, actual_notification = get_mock_send_notification_calls()[0]
        assert actual_topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN
        assert isinstance(actual_notification.details, UnknownErrorDetails)
        assert actual_notification.details.error_name == expected_error.__name__

    def test_it_raises_invalid_key_error_and_sends_notification_if_event_is_invalid(self) -> None:
        # Missing the root dir
        event_with_invalid_key = generate_event(
            key=f"{DEFAULT_MOCK_PARTNER_HOME_DIR}/out/{DEFAULT_MOCK_OBJECT_FILENAME}"
        )

        with pytest.raises(InvalidObjectKeyError):
            handler(event=event_with_invalid_key, context=mock_lambda_context)

        assert mock_send_notification.call_count == 1

        actual_topic_arn, actual_notification = get_mock_send_notification_calls()[0]
        assert actual_topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN
        assert isinstance(actual_notification.details, TransferFailedDetails)
        assert actual_notification.details.error_name == InvalidObjectKeyError.__name__

    def test_it_raises_unknown_partner_error_and_sends_notification_if_global_config_is_invalid(
        self,
    ) -> None:
        event = generate_event()
        mock_global_ssm.from_ssm.return_value = GlobalSsmConfig(
            sftp_connect_timeout=10,
            sftp_hostname="test",
            sftp_username="test",
            sftp_host_pub_keys=DEFAULT_MOCK_GLOBAL_CONFIG.sftp_host_pub_keys,
            sftp_user_priv_key="test",
            # With no enrolled partners and an empty home directory to partner dict, the handler is
            # unable to determine which partner the file belongs to (as the home directory may be a
            # different name than the partner's name)
            enrolled_partners=[],
            home_dirs_to_partner={},
        )

        with pytest.raises(UnknownPartnerError):
            handler(event=event, context=mock_lambda_context)

        assert mock_send_notification.call_count == 1

        actual_topic_arn, actual_notification = get_mock_send_notification_calls()[0]
        assert actual_topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN
        assert isinstance(actual_notification.details, TransferFailedDetails)
        assert actual_notification.details.error_name == UnknownPartnerError.__name__

    def test_it_raises_invalid_partner_error_and_sends_notifications_if_event_is_invalid(
        self,
    ) -> None:
        invalid_event = generate_event(
            key=f"{DEFAULT_MOCK_BUCKET_ROOT_DIR}/{DEFAULT_MOCK_PARTNER_HOME_DIR}/invalid/{DEFAULT_MOCK_OBJECT_FILENAME}"
        )

        with pytest.raises(InvalidPendingDirError):
            handler(event=invalid_event, context=mock_lambda_context)

        mocked_calls = get_mock_send_notification_calls()
        assert mock_send_notification.call_count == 2
        assert all(
            isinstance(actual_notification.details, TransferFailedDetails)
            and actual_notification.details.error_name == InvalidPendingDirError.__name__
            for _, actual_notification in mocked_calls
        )
        assert any(topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN for topic_arn, _ in mocked_calls)
        assert any(
            topic_arn == DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER[DEFAULT_MOCK_PARTNER_NAME]
            for topic_arn, _ in mocked_calls
        )

    def test_it_raises_unrecognized_file_error_and_sends_notifications_if_file_is_unrecognized(
        self,
    ) -> None:
        invalid_event = generate_event(
            key=f"{DEFAULT_MOCK_BUCKET_ROOT_DIR}/{DEFAULT_MOCK_PARTNER_HOME_DIR}/{DEFAULT_MOCK_PENDING_DIR}/unknown_file"
        )

        with pytest.raises(UnrecognizedFileError):
            handler(event=invalid_event, context=mock_lambda_context)

        mocked_calls = get_mock_send_notification_calls()
        assert mock_send_notification.call_count == 2
        assert all(
            isinstance(actual_notification.details, TransferFailedDetails)
            and actual_notification.details.error_name == UnrecognizedFileError.__name__
            for _, actual_notification in mocked_calls
        )
        assert any(topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN for topic_arn, _ in mocked_calls)
        assert any(
            topic_arn == DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER[DEFAULT_MOCK_PARTNER_NAME]
            for topic_arn, _ in mocked_calls
        )

    def test_it_raises_sftp_connection_error_and_sends_notifications_if_connection_fails(
        self,
    ) -> None:
        event = generate_event()
        mock_ssh_client.connect.side_effect = paramiko.SSHException()

        with pytest.raises(SFTPConnectionError):
            handler(event=event, context=mock_lambda_context)

        mocked_calls = get_mock_send_notification_calls()
        assert mock_send_notification.call_count == 4
        assert (
            sum(
                isinstance(actual_notification.details, FileDiscoveredDetails)
                for _, actual_notification in mocked_calls
            )
            == 2
        )
        assert (
            sum(
                isinstance(actual_notification.details, TransferFailedDetails)
                and actual_notification.details.error_name == SFTPConnectionError.__name__
                for _, actual_notification in mocked_calls
            )
            == 2
        )
        assert (
            sum(topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN for topic_arn, _ in mocked_calls) == 2
        )
        assert (
            sum(
                topic_arn == DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER[DEFAULT_MOCK_PARTNER_NAME]
                for topic_arn, _ in mocked_calls
            )
            == 2
        )

    def test_it_raises_sftp_transfer_error_and_sends_notifications_if_transfer_fails(self) -> None:
        event = generate_event()
        mock_sftp_client.putfo.side_effect = OSError()

        with pytest.raises(SFTPTransferError):
            handler(event=event, context=mock_lambda_context)

        mocked_calls = get_mock_send_notification_calls()
        assert mock_send_notification.call_count == 4
        assert (
            sum(
                isinstance(actual_notification.details, FileDiscoveredDetails)
                for _, actual_notification in mocked_calls
            )
            == 2
        )
        assert (
            sum(
                isinstance(actual_notification.details, TransferFailedDetails)
                and actual_notification.details.error_name == SFTPTransferError.__name__
                for _, actual_notification in mocked_calls
            )
            == 2
        )
        assert (
            sum(topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN for topic_arn, _ in mocked_calls) == 2
        )
        assert (
            sum(
                topic_arn == DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER[DEFAULT_MOCK_PARTNER_NAME]
                for topic_arn, _ in mocked_calls
            )
            == 2
        )

    def test_it_executes_sftp_transfer_process_if_event_is_valid(self) -> None:
        event = generate_event()
        mock_host_keys = mock.Mock()
        mock_ssh_client.get_host_keys.return_value = mock_host_keys

        def mock_rsakey_func(data: bytes) -> str:
            return b64encode(data).decode("utf-8")

        mock_paramiko.RSAKey.side_effect = mock_rsakey_func

        def mock_rsakey_privkey_func(file_obj: StringIO) -> str:
            return file_obj.getvalue()

        mock_paramiko.RSAKey.from_private_key.side_effect = mock_rsakey_privkey_func
        mock_s3_client = mock.Mock()
        mock_boto3_client.return_value = mock_s3_client

        handler(event=event, context=mock_lambda_context)

        actual_ssh_connect_data = mock_ssh_client.connect.call_args.kwargs
        expected_ssh_connect_data = {
            "hostname": DEFAULT_MOCK_GLOBAL_CONFIG.sftp_hostname,
            "username": DEFAULT_MOCK_GLOBAL_CONFIG.sftp_username,
            "pkey": DEFAULT_MOCK_GLOBAL_CONFIG.sftp_user_priv_key,
            "look_for_keys": False,
            "allow_agent": False,
            "timeout": DEFAULT_MOCK_GLOBAL_CONFIG.sftp_connect_timeout,
        }
        assert actual_ssh_connect_data == expected_ssh_connect_data

        actual_s3_download_obj_key = mock_s3_client.download_fileobj.call_args.kwargs["Key"]
        expected_s3_download_obj_key = DEFAULT_MOCK_OBJECT_KEY
        assert actual_s3_download_obj_key == expected_s3_download_obj_key

        actual_sftp_putfo_path = mock_sftp_client.putfo.call_args.kwargs["remotepath"]
        expected_sftp_putfo_path = f"{DEFAULT_MOCK_PARTNER_CONFIG.recognized_files[0].staging_folder}/{DEFAULT_MOCK_OBJECT_FILENAME}"  # noqa: E501
        assert actual_sftp_putfo_path == expected_sftp_putfo_path

        actual_sftp_chmod_data = mock_sftp_client.chmod.call_args.kwargs
        expected_sftp_chmod_data = {"path": expected_sftp_putfo_path, "mode": 0o664}
        assert actual_sftp_chmod_data == expected_sftp_chmod_data

        actual_sftp_rename_data = mock_sftp_client.rename.call_args.kwargs
        expected_sftp_rename_data = {
            "oldpath": expected_sftp_putfo_path,
            "newpath": f"{DEFAULT_MOCK_PARTNER_CONFIG.recognized_files[0].input_folder}/{DEFAULT_MOCK_OBJECT_FILENAME}",  # noqa: E501
        }
        assert actual_sftp_rename_data == expected_sftp_rename_data

        mocked_calls = get_mock_send_notification_calls()
        assert mock_send_notification.call_count == 4
        assert (
            sum(
                isinstance(actual_notification.details, FileDiscoveredDetails)
                and actual_notification.details.object_key == DEFAULT_MOCK_OBJECT_KEY
                and actual_notification.details.partner == DEFAULT_MOCK_PARTNER_NAME
                and actual_notification.details.file_type
                == DEFAULT_MOCK_PARTNER_CONFIG.recognized_files[0].type
                for _, actual_notification in mocked_calls
            )
            == 2
        )
        assert (
            sum(
                isinstance(actual_notification.details, TransferSuccessDetails)
                and actual_notification.details.object_key == DEFAULT_MOCK_OBJECT_KEY
                and actual_notification.details.partner == DEFAULT_MOCK_PARTNER_NAME
                and actual_notification.details.file_type
                == DEFAULT_MOCK_PARTNER_CONFIG.recognized_files[0].type
                for _, actual_notification in mocked_calls
            )
            == 2
        )
        assert (
            sum(topic_arn == DEFAULT_MOCK_BFD_SNS_TOPIC_ARN for topic_arn, _ in mocked_calls) == 2
        )
        assert (
            sum(
                topic_arn == DEFAULT_MOCK_SNS_TOPIC_ARNS_BY_PARTNER[DEFAULT_MOCK_PARTNER_NAME]
                for topic_arn, _ in mocked_calls
            )
            == 2
        )
