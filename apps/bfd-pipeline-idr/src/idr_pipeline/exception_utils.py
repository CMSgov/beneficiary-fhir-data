from dataclasses import dataclass
from types import TracebackType
from typing import Any, cast

from tblib import Traceback  # type: ignore

type SerializedExceptionChain = list[SerializedException | SerializedExceptionGroup]


@dataclass(frozen=True, eq=True)
class SerializedException:
    ex: BaseException
    tb_dict: dict[str, Any]


@dataclass(frozen=True, eq=True)
class SerializedExceptionGroup:
    ex_group: BaseExceptionGroup[Exception] | ExceptionGroup[Exception]
    tb_dict: dict[str, Any]
    exceptions: list[SerializedException]


def get_exception_tb_dict(ex: BaseException) -> dict[str, Any]:
    return Traceback(ex.__traceback__).as_dict()  # type: ignore


def get_traceback_from_dict(tb_dict: dict[str, Any]) -> TracebackType | None:
    return cast(Traceback, Traceback.from_dict(tb_dict)).as_traceback()  # type: ignore


def serialize_exception_chain(exc: BaseException) -> SerializedExceptionChain:
    chain: list[SerializedException | SerializedExceptionGroup] = []
    current: BaseException | None = exc

    while current is not None:
        if isinstance(current, BaseExceptionGroup | ExceptionGroup):
            current = cast(BaseExceptionGroup[Exception], current)
            serialized_inner: list[SerializedException] = [
                SerializedException(ex=inner, tb_dict=get_exception_tb_dict(inner))
                for inner in current.exceptions
            ]
            chain.append(
                SerializedExceptionGroup(
                    ex_group=current,
                    tb_dict=get_exception_tb_dict(current),
                    exceptions=serialized_inner,
                )
            )
        else:
            chain.append(SerializedException(ex=current, tb_dict=get_exception_tb_dict(current)))

        current = current.__cause__

    return chain


def rebuild_exception_chain(chain: SerializedExceptionChain) -> BaseException:
    if not chain:
        raise ValueError("Chain must contain at least one exception")

    # Materialise each entry into a (BaseException, tb_dict) pair so the
    # linking loop below stays uniform.
    resolved: list[tuple[BaseException, dict[str, Any]]] = []

    for entry in chain:
        if isinstance(entry, SerializedExceptionGroup):
            # Restore tracebacks on every inner exception.
            restored_inners: list[Exception] = []
            for serialized_inner in entry.exceptions:
                inner_exc = serialized_inner.ex
                inner_exc.__traceback__ = get_traceback_from_dict(serialized_inner.tb_dict)
                restored_inners.append(inner_exc)  # type: ignore[arg-type]

            # Re-create the group with the restored inner exceptions so that
            # the group's own .exceptions tuple reflects the restored state.
            restored_group = entry.ex_group.derive(restored_inners)
            resolved.append((restored_group, entry.tb_dict))
        else:
            resolved.append((entry.ex, entry.tb_dict))

    # Re-link the chain (cause → effect) and restore top-level tracebacks.
    for i, (exc, tb_dict) in enumerate(resolved):
        exc.__traceback__ = get_traceback_from_dict(tb_dict)
        exc.__cause__ = resolved[i + 1][0] if i < len(resolved) - 1 else None

    return resolved[0][0]
