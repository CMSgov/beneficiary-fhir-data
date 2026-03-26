from dataclasses import dataclass
from functools import lru_cache
from typing import TYPE_CHECKING, Any, TypeVar, cast

from pydantic.main import BaseModel


@dataclass(frozen=True)
class _GetFields:
    _model: type[BaseModel]

    def __getattr__(self, item: str) -> Any:  # noqa: ANN401
        if item in self._model.model_fields:
            return item

        return getattr(self._model, item)


TModel = TypeVar("TModel", bound=type[BaseModel])


def fields[TModel: type[BaseModel]](model: TModel, /) -> TModel:
    return cast(TModel, _GetFields(model))


if not TYPE_CHECKING:
    fields = lru_cache(maxsize=256)(fields)
