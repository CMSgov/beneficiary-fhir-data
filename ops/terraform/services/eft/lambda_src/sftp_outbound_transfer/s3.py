from enum import StrEnum


class S3EventType(StrEnum):
    """Represents the types of S3 events that this Lambda is invoked by and supports.

    The value of each Enum is a substring that is matched for on the "eventName" property of an
    invocation event
    """

    OBJECT_CREATED = "ObjectCreated"

    @classmethod
    def from_event_name(cls, event_name: str) -> "S3EventType":
        try:
            return next(x for x in S3EventType if x in event_name)
        except StopIteration as ex:
            raise ValueError(
                f"Invalid event name {event_name}; no corresponding, supported event found"
            ) from ex
