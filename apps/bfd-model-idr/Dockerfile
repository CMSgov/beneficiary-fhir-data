FROM ghcr.io/astral-sh/uv:0.13.6-python3.14-alpine@sha256:a069b57ec30ef94da43700a7332b162a13adb0b8a1cfba4e0b62c1a9727153c4

RUN apk update && apk add build-base compiler-rt clang
# building snowpark with gcc produces "error: unrecognized command-line option '--rtlib=compiler-rt'"
# so we have to force usage of clang here
ENV CC=clang
ENV CXX=clang++
