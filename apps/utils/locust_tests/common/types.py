"""Defines common types"""

from typing import Collection, Protocol, TypeVar

TElement = TypeVar("TElement", covariant=True)
T = TypeVar("T")


class Copyable(Protocol):
    def copy(self: T) -> T:
        ...


class Gettable(Protocol[TElement]):
    def __getitem__(self: "Gettable", __k: int) -> TElement:
        ...


class CopyableEnumerable(Collection[TElement], Copyable, Gettable[TElement], Protocol[TElement]):
    ...
