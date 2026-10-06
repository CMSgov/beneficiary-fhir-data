import logging
import random
import string
import sys
from abc import ABC, abstractmethod
from collections.abc import Callable, Iterable
from dataclasses import dataclass, field
from typing import Any

from constants import (
    BENE_HSTRY,
    BENE_MAPD_ENRLMT_RX,
    BENE_MBI_ID,
    CLM,
    CLM_DCMTN,
    CLM_DT_SGNTR,
    CLM_LINE,
    CLM_RLT_COND_SGNTR_MBR,
    CNTRCT_PBP_NUM,
    PRVDR_HSTRY,
)
from load_synthetic_output import SnowflakeWriter

logger = logging.getLogger(__name__)


class IdGenerator(ABC):
    def gen_basic_id(self, field: str, length: int, alphabet: str = string.digits) -> str:
        return self.multipart_id(field=field, parts=[(alphabet, length)])

    def calculate_npi_checksum(self, npi_9: str) -> str:
        full_str = "80840" + npi_9
        digits = [int(char) for char in full_str]
        for i in range(len(digits)):
            if (len(digits) - 1 - i) % 2 == 0:
                val = digits[i] * 2
                if val > 9:
                    val = val - 9
                digits[i] = val
        total = sum(digits)
        check_digit = (10 - (total % 10)) % 10
        return str(check_digit)

    @abstractmethod
    def multipart_id(self, field: str, parts: list[tuple[str, int]]) -> str: ...

    @abstractmethod
    def numeric_id(self, field: str, start: int = -1, end: int = -(sys.maxsize - 1)) -> str: ...

    @abstractmethod
    def npi_id(self, field: str) -> str: ...

    @abstractmethod
    def bene_sk(self) -> int: ...

    @abstractmethod
    def mbi(self) -> str: ...

    @abstractmethod
    def claim_num_sk(self, clm_type_cd: str, clm_dt_sgntr_sk: str, geo_bene_sk: str) -> str: ...


def _sorted_chars(chars: Iterable[str]) -> str:
    return "".join(sorted(set(chars)))


_MBI_ALPHABETS = [
    _sorted_chars("123456789"),
    _sorted_chars("LOIBZ"),
    _sorted_chars((set(string.ascii_uppercase) - set("SLOIBZ")) | set(string.digits)),
    _sorted_chars(string.digits),
    _sorted_chars(set(string.ascii_uppercase) - set("SLOIBZ")),
    _sorted_chars((set(string.ascii_uppercase) - set("SLOIBZ")) | set(string.digits)),
    _sorted_chars(string.digits),
    _sorted_chars(set(string.ascii_uppercase) - set("SLOIBZ")),
    _sorted_chars(set(string.ascii_uppercase) - set("SLOIBZ")),
    _sorted_chars(string.digits),
    _sorted_chars(string.digits),
]


class RandomIdGenerator(IdGenerator):
    def __init__(self) -> None:
        self._used_by_field: dict[str, set[str]] = {}
        self._used_bene_sk: set[int] = set()
        self._used_mbi: set[str] = set()

    def _gen_id(self, field: str, gen_func: Callable[[], str]) -> str:
        id_set = self._used_by_field.setdefault(field, set())
        while True:
            id = gen_func()
            if id not in id_set:
                id_set.add(id)
                return id

    def multipart_id(self, field: str, parts: list[tuple[str, int]]) -> str:
        return self._gen_id(
            field=field,
            gen_func=lambda: (
                f"-{
                    ''.join(
                        [
                            ''.join(random.choices(population=allowed_chars, k=length))
                            for (allowed_chars, length) in parts
                        ]
                    )
                }"
            ),
        )

    def numeric_id(self, field: str, start: int = -1, end: int = -(sys.maxsize - 1)) -> str:
        if start > 0 or end > 0 or end > start:
            raise ValueError(
                "'end' and 'start' must be negative and 'end' must be less than 'start'"
            )

        return self._gen_id(field=field, gen_func=lambda: str(random.randint(end, start)))

    def npi_id(self, field: str) -> str:
        def make_npi():
            first_digit = random.choice(["1", "2"])
            rest = "".join(random.choices(population=string.digits, k=8))
            npi_9 = first_digit + rest
            check_digit = self.calculate_npi_checksum(npi_9)
            return npi_9 + check_digit

        return self._gen_id(field=field, gen_func=make_npi)

    def bene_sk(self) -> int:
        while True:
            bene_sk = random.randint(-1000000000, -1000)
            if bene_sk not in self._used_bene_sk:
                self._used_bene_sk.add(bene_sk)
                return bene_sk

    def mbi(self) -> str:
        full_mbi = "".join(random.choice(a) for a in _MBI_ALPHABETS)
        if full_mbi in self._used_mbi:
            return self.mbi()
        self._used_mbi.add(full_mbi)
        return full_mbi

    def claim_num_sk(self, clm_type_cd: str, clm_dt_sgntr_sk: str, geo_bene_sk: str) -> str:  # noqa: ARG002
        return self.numeric_id(field="CLM_NUM_SK")


DEFAULT_INITIAL_BENE_SK = -1000
DEFAULT_INITIAL_CLM_NUM_SK = 0
DEFAULT_INITIAL_NPI = 100_000_000


@dataclass
class SnowflakeIdState:
    bene_sk_next: int = DEFAULT_INITIAL_BENE_SK
    numeric_id_next: dict[str, int] = field(default_factory=dict)
    multipart_id_next: dict[str, int] = field(default_factory=dict)
    mbi_next: int = 0
    npi_next: int = DEFAULT_INITIAL_NPI
    claim_num_sk_next: dict[tuple[str, str, str], int] = field(default_factory=dict)


def _query(writer: SnowflakeWriter, sql: str) -> Any:
    result = writer.session.sql(sql).collect()
    return result[0][0] if result else None


def _query_multiple(writer: SnowflakeWriter, sql: str) -> list[tuple[Any, ...]]:
    return writer.session.sql(sql).collect()


def load_id_state(writer: SnowflakeWriter, truncate: bool = False) -> SnowflakeIdState:
    state = SnowflakeIdState()

    if not truncate:
        # Get or set the min ben_sk
        existing_min_bene_sk = _query(
            writer, f"SELECT MIN(BENE_SK) FROM {writer.qualified_table(BENE_HSTRY)}"
        )
        if existing_min_bene_sk is not None:
            state.bene_sk_next = int(existing_min_bene_sk) - 1

        # Get or set the max mbi
        existing_max_mbi = _query(
            writer, f"SELECT MAX(BENE_MBI_ID) FROM {writer.qualified_table(BENE_MBI_ID)}"
        )
        if existing_max_mbi is not None:
            state.mbi_next = _decode_identifier(existing_max_mbi, _MBI_ALPHABETS) + 1

        # Get or set the max npi
        existing_max_npi = _query(
            writer, f"SELECT MAX(PRVDR_NPI_NUM) FROM {writer.qualified_table(PRVDR_HSTRY)}"
        )
        if existing_max_npi is not None:
            state.npi_next = int(existing_max_npi[:9]) + 1

    _NUMERIC_MULTIPART_FIELDS: dict[str, str] = {
        "CNTRCT_PBP_SK": CNTRCT_PBP_NUM,
        "CLM_UNIQ_ID": CLM,
        "CLM_DT_SGNTR_SK": CLM_DT_SGNTR,
        "GEO_BENE_SK": CLM,
        "CLM_RLT_COND_SGNTR_SK": CLM_RLT_COND_SGNTR_MBR,
        "BENE_PDP_ENRLMT_MMBR_ID_NUM": BENE_MAPD_ENRLMT_RX,
    }

    _TEXT_MULTIPART_FIELDS: dict[str, tuple[str, list[tuple[str, int]]]] = {
        "CLM_LINE_PMD_UNIQ_TRKNG_NUM": (CLM_LINE, [(string.ascii_uppercase + string.digits, 13)]),
        "CLM_CNTL_NUM": (CLM, [(string.digits, 14), (string.ascii_uppercase, 3)]),
        "CLM_ORIG_CNTL_NUM": (CLM, [(string.digits, 14), (string.ascii_uppercase, 3)]),
        "PRVDR_EMPLR_ID_NUM": (PRVDR_HSTRY, [(string.digits, 9)]),
        "PRVDR_OSCAR_NUM": (PRVDR_HSTRY, [(string.digits, 6)]),
        "CLM_PTNT_CNTL_NUM": (CLM_DCMTN, [(string.digits, 14), (string.ascii_uppercase, 3)]),
    }

    # Get or set the min numeric ids
    for field_name, table_name in _NUMERIC_MULTIPART_FIELDS.items():
        existing_min = None
        if not truncate:
            existing_min = _query(
                writer, f"SELECT MIN({field_name}) FROM {writer.qualified_table(table_name)}"
            )
        if existing_min is not None:
            state.numeric_id_next[field_name] = int(existing_min) - 1
        else:
            state.numeric_id_next[field_name] = 0

    # Get or set the max multipart ids
    for field_name, (table_name, parts) in _TEXT_MULTIPART_FIELDS.items():
        existing_max = None
        if not truncate:
            existing_max = _query(
                writer, f"SELECT MAX({field_name}) FROM {writer.qualified_table(table_name)}"
            )

        if existing_max is not None:
            value = existing_max.removeprefix("-")
            alphabets = _expand_parts(parts)
            try:
                state.multipart_id_next[field_name] = _decode_identifier(value, alphabets) + 1
            except ValueError:
                logger.info(
                    f"Value Error when decoding multipart id field_name: {value} Setting max id to 0"
                )
                state.multipart_id_next[field_name] = 0
        else:
            state.multipart_id_next[field_name] = 0

    if not truncate:
        # Get or set the max clm_num_sk per (clm_type_cd, clm_dt_sgntr_sk, geo_bene_sk)
        claims_rows = _query_multiple(
            writer,
            f"""
            SELECT
                CLM_TYPE_CD,
                CLM_DT_SGNTR_SK,
                GEO_BENE_SK,
                MAX(CLM_NUM_SK) AS MAX_CLM_NUM_SK
            FROM {writer.qualified_table(CLM)}
            GROUP BY
                CLM_TYPE_CD,
                CLM_DT_SGNTR_SK,
                GEO_BENE_SK
            """,
        )

        for clm_type_cd, clm_dt_sgntr_sk, geo_bene_sk, max_clm_num_sk in claims_rows:
            key_columns = (str(clm_type_cd), str(clm_dt_sgntr_sk), str(geo_bene_sk))
            state.claim_num_sk_next[key_columns] = int(max_clm_num_sk) + 1

    return state


def _expand_parts(parts: list[tuple[str, int]]) -> list[str]:
    position_alphabets: list[str] = []
    for chars, length in parts:
        position_alphabets.extend([_sorted_chars(chars)] * length)
    return position_alphabets


# _decode_identifier converts a string identifier into its numeric position so we can treat
# these identifiers as sequential counters. This uses a mixed radix counter approach here. Each
# position can have a different number of possible values so its radix. position_alphabets contains
# the ordered alphabet for each position. We increment its numeric position and encode that back
# into the next valid identifier _encode_identifier
def _decode_identifier(value: str, position_alphabets: list[str]) -> int:
    position = 0
    multiplier = 1
    for char, alphabet in zip(reversed(value), reversed(position_alphabets), strict=False):
        position += alphabet.index(char) * multiplier
        multiplier *= len(alphabet)
    return position


def _encode_identifier(position: int, position_alphabets: list[str]) -> str:
    characters = []
    for alphabet in reversed(position_alphabets):
        position, character_position = divmod(position, len(alphabet))
        characters.append(alphabet[character_position])

    if position != 0:
        raise ValueError("position exceeds capacity for these alphabets")

    return "".join(reversed(characters))


class SequentialIdGenerator(IdGenerator):
    def __init__(self, state: SnowflakeIdState) -> None:
        self.state = state

    def multipart_id(self, field: str, parts: list[tuple[str, int]]) -> str:
        position_alphabets = _expand_parts(parts)
        if field not in self.state.multipart_id_next:
            self.state.multipart_id_next[field] = 0

        position = self.state.multipart_id_next[field]
        self.state.multipart_id_next[field] += 1

        return "-" + _encode_identifier(position, position_alphabets)

    def numeric_id(self, field: str, start: int = -1, end: int = -(sys.maxsize - 1)) -> str:
        if start > 0 or end > 0 or end > start:
            raise ValueError(
                "'end' and 'start' must be negative and 'end' must be less than 'start'"
            )
        value = self.state.numeric_id_next.setdefault(field, start)
        self.state.numeric_id_next[field] = value - 1
        return str(value)

    def bene_sk(self) -> int:
        value = self.state.bene_sk_next
        self.state.bene_sk_next -= 1
        return value

    def mbi(self) -> str:
        position = self.state.mbi_next
        self.state.mbi_next += 1
        return _encode_identifier(position, _MBI_ALPHABETS)

    def npi_id(self, field: str) -> str:  # noqa: ARG002
        position = self.state.npi_next

        if position >= 299_999_999:
            raise OverflowError("npi_id exhausted the 9-digits NPI body space")
        self.state.npi_next += 1
        npi_9 = str(position)
        return npi_9 + self.calculate_npi_checksum(npi_9)

    def claim_num_sk(self, clm_type_cd: str, clm_dt_sgntr_sk: str, geo_bene_sk: str) -> str:
        key_columns = (clm_type_cd, clm_dt_sgntr_sk, geo_bene_sk)
        value = self.state.claim_num_sk_next.setdefault(key_columns, DEFAULT_INITIAL_CLM_NUM_SK)
        self.state.claim_num_sk_next[key_columns] = value + 1
        return str(value)
