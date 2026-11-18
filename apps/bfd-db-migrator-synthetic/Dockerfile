FROM docker.io/flyway/flyway:13.12.0@sha256:3704e17c99e22adb8385552cdf8dd7fbc05ae6c420cb78215d9ac6cf1261a15c

RUN apt-get update \
  && apt-get install -y --no-install-recommends --only-upgrade ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY ./migrations /app/migrations

ENTRYPOINT [ \
  "flyway", \
  "-workingDirectory=/app", \
  "-locations=filesystem:migrations", \
  "-schemas=CMS_VDM_VIEW_MDCR_PRD,CMS_EDP_VIEW_CVM_PRAU_PRD" \
  ]
CMD ["migrate"]
