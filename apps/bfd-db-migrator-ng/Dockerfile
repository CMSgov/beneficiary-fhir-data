FROM docker.io/flyway/flyway:13.11.1@sha256:fd082457bd0a470f567e236e16ec886c5fd1bd579b1bd1431e65d0e05fde88e2

RUN apt-get update \
  && apt-get install -y --no-install-recommends --only-upgrade ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY ./migrations /app/migrations
COPY ./callbacks /app/callbacks

ENTRYPOINT [ \
  "flyway", \
  "-workingDirectory=/app", \
  "-locations=filesystem:migrations", \
  "-callbackLocations=filesystem:callbacks", \
  "-schemas=idr" \
  ]
CMD ["migrate"]
