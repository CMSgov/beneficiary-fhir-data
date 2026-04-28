FROM docker.io/flyway/flyway:12.3.0@sha256:016aca729095ca0bfb1d4f92c4c81a6aff439de826bd05dad712fb0f948671d2

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
