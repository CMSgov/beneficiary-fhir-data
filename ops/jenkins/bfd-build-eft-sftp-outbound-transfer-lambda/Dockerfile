FROM public.ecr.aws/lambda/python:3.12 as base

# Build required python packages
FROM base as python
COPY requirements.txt .
RUN pip3 install -r requirements.txt

# Copy function code
FROM base as lambda
COPY --from=python /var/lang/lib/python3.12/site-packages /var/lang/lib/python3.12/site-packages
COPY --from=python /var/lang/bin /var/lang/bin
COPY . "${LAMBDA_TASK_ROOT}/"
ENV PATH="/var/lang/lib/python3.12/site-packages:${PATH}"
CMD [ "sftp_outbound_transfer.handler" ]

