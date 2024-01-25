import calendar
from dataclasses import asdict, dataclass
from datetime import datetime, timedelta, timezone
from typing import TYPE_CHECKING, Any, Optional

from common import RifFileType

# Solve typing issues in Lambda as mypy_boto3 is not included
if TYPE_CHECKING:
    from mypy_boto3_dynamodb.service_resource import Table
else:
    Table = object


@dataclass(frozen=True, eq=True)
class RifAvailableEventItem:
    group_iso_str: str
    """The load/group that this event is associated with"""
    rif: str
    """The RIF that generated this event"""
    event_timestamp: int
    """The time, as a unix timestamp in seconds, at which this event occurred"""
    dynamo_expiration: int
    """The time, as a unix timestamp in seconds, at which this item will expire in the DynamoDB
    Table"""

    @classmethod
    def from_item_dict(cls, item_dict: dict[str, Any]) -> "RifAvailableEventItem":
        return RifAvailableEventItem(
            group_iso_str=str(item_dict["group_iso_str"]),
            rif=str(item_dict["rif"]),
            event_timestamp=int(item_dict["event_timestamp"]),
            dynamo_expiration=int(item_dict["dynamo_expiration"]),
        )


@dataclass(frozen=True, eq=True)
class LoadAvailableEventItem:
    group_iso_str: str
    """The load/group that this event is associated with"""
    event_timestamp: int
    """The time, as a unix timestamp in seconds, at which this event occurred"""
    dynamo_expiration: int
    """The time, as a unix timestamp in seconds, at which this item will expire in the DynamoDB
    Table"""

    @classmethod
    def from_item_dict(cls, item_dict: dict[str, Any]) -> "LoadAvailableEventItem":
        return LoadAvailableEventItem(
            group_iso_str=str(item_dict["group_iso_str"]),
            event_timestamp=int(item_dict["event_timestamp"]),
            dynamo_expiration=int(item_dict["dynamo_expiration"]),
        )


@dataclass(frozen=True, eq=True)
class RifAvailableEvent:
    date_time: datetime
    """The time at which this event occurred"""
    group_iso_str: str
    """The load/group that this event is associated with"""
    rif: RifFileType
    """The RIF that generated this event"""

    @classmethod
    def from_item(cls, item: RifAvailableEventItem) -> "RifAvailableEvent":
        return RifAvailableEvent(
            date_time=datetime.utcfromtimestamp(item.event_timestamp).replace(tzinfo=timezone.utc),
            group_iso_str=item.group_iso_str,
            rif=RifFileType(item.rif),
        )

    def to_item(self) -> RifAvailableEventItem:
        return RifAvailableEventItem(
            group_iso_str=self.group_iso_str,
            rif=self.rif.value,
            event_timestamp=calendar.timegm(self.date_time.utctimetuple()),
            dynamo_expiration=calendar.timegm((self.date_time + timedelta(days=7)).utctimetuple()),
        )


@dataclass(frozen=True, eq=True)
class LoadAvailableEvent:
    date_time: datetime
    """The time at which this event occurred"""
    group_iso_str: str
    """The load/group that this event is associated with"""

    @classmethod
    def from_item(cls, item: LoadAvailableEventItem) -> "LoadAvailableEvent":
        return LoadAvailableEvent(
            date_time=datetime.utcfromtimestamp(item.event_timestamp).replace(tzinfo=timezone.utc),
            group_iso_str=item.group_iso_str,
        )

    def to_item(self) -> LoadAvailableEventItem:
        return LoadAvailableEventItem(
            group_iso_str=self.group_iso_str,
            event_timestamp=calendar.timegm(self.date_time.utctimetuple()),
            dynamo_expiration=calendar.timegm((self.date_time + timedelta(days=7)).utctimetuple()),
        )


def get_rif_available_event(
    table: Table, group_iso_str: str, rif: RifFileType
) -> Optional[RifAvailableEvent]:
    response = table.get_item(Key={"group_iso_str": group_iso_str, "rif": rif.value})

    try:
        return RifAvailableEvent.from_item(
            RifAvailableEventItem.from_item_dict(item_dict=response["Item"])
        )
    except (KeyError, ValueError):
        return None


def get_load_available_event(table: Table, group_iso_str: str) -> Optional[LoadAvailableEvent]:
    response = table.get_item(Key={"group_iso_str": group_iso_str})

    try:
        return LoadAvailableEvent.from_item(
            LoadAvailableEventItem.from_item_dict(item_dict=response["Item"])
        )
    except (KeyError, ValueError):
        return None


def put_rif_available_event(table: Table, event: RifAvailableEvent):
    table.put_item(Item=asdict(event.to_item()))


def put_load_available_event(table: Table, event: LoadAvailableEvent):
    table.put_item(Item=asdict(event.to_item()))
