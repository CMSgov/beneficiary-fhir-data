FROM docker.io/flyway/flyway:13.2.0@sha256:cbd95910e0175318456b6e9927b04dd31f51c0e153ab573bf546964978bd3e30

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
