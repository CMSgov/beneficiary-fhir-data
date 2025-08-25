import json


class BaseTransferError(Exception):
    def __init__(self, message: str, s3_object_key: str, partner: str | None) -> None:
        super().__init__(message, s3_object_key, partner)

        self.message = message
        self.s3_object_key = s3_object_key
        self.partner = partner

    def __str__(self) -> str:
        return json.dumps(
            {
                "message": self.message,
                "s3_object_key": self.s3_object_key,
                "partner": self.partner,
            }
        )


class UnknownPartnerError(BaseTransferError): ...


class InvalidObjectKeyError(BaseTransferError): ...


class InvalidPendingDirError(BaseTransferError): ...


class UnrecognizedFileError(BaseTransferError): ...


class SFTPConnectionError(BaseTransferError): ...


class SFTPTransferError(BaseTransferError): ...
