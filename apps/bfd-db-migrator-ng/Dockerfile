FROM docker.io/flyway/flyway:13.12.1@sha256:3b4fae57858ee25735230e42f426fd200144fedbe692ea7789aa3e50a7c11657

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
