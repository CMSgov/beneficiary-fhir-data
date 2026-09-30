def main() -> None:
    # gen_dd.py runs its generation logic at import time
    from . import gen_dd  # noqa: F401


if __name__ == "__main__":
    main()
