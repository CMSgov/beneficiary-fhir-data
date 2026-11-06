<!-- markdownlint-disable MD013 -->

# `ops/utils/scripts`

Small utilities used by developers and by the `just` recipes. `just --list` shows which recipe calls which script.

- `build-image.sh`: builds any of the repository's container images locally with podman (see below).
- `lambda-error-metrics`: queries Lambda error metrics from CloudWatch.
- `py-check.sh`: runs the four checks a Python change must pass against the workspace root, from any directory. Called by `just py-check`.
- `update-tofu-docs`: regenerates the OpenTofu documentation blocks in `ops/`.

## Building container images locally

`just docker-build <image>` builds one image with podman. It uses the same build matrix CI uses: `.github/workflows/build_container_images_matrix.json`. Anything in that file is buildable here. There is no separate list of images to keep up to date.

```sh
just docker-images                                        # what can be built, and from which Dockerfile
just docker-build bfd-platform-run-idr-pipeline           # build one image
./ops/utils/scripts/build-image.sh bfd-platform-run-idr-pipeline   # the same thing, without just
```

| Option | Effect |
|---|---|
| `--list` | Print every buildable image, its Dockerfile and its target platform, then exit. |
| `--tag <tag>` | Tag the image as `<name>:<tag>` in addition to `<name>`. Tag any base images built along the way the same way. Dockerfiles that declare `ARG base_version` are then built against `<tag>`. Without it the image is tagged `latest`. |
| `--platform <platform>` | Build for a different platform than the matrix entry. |
| `--no-base` | Do not build the base images the requested image depends on. |

```sh
just docker-build bfd-platform-idr-pipeline --tag 2.999.0
```

### Base images

`ops/images/bfd-platform-base-python` is the base of the IDR pipeline, the validator and the cert-stores image. Those Dockerfiles resolve `FROM bfd-platform-base-python:${base_version}` from the local image store. `build-image.sh` reads the Dockerfile, finds any base that is itself in the matrix, and builds it first. It is rebuilt on every run.

`--tag <tag>` pins the chain: the base is tagged `<tag>`, and the derived image is built with `base_version=<tag>` so it uses that base rather than an untagged one from a previous run. With `--no-base` nothing is built for the bases, so the version the Dockerfile asks for must already be in the local image store. The script checks first and, when it is missing, says which image to build.
