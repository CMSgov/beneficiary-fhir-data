FROM docker.io/flyway/flyway:13.12.1@sha256:3b4fae57858ee25735230e42f426fd200144fedbe692ea7789aa3e50a7c11657

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
