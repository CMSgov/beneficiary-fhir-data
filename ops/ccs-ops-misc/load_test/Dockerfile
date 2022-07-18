FROM public.ecr.aws/lambda/python:3.8 as base
RUN yum install git gcc python3-devel -y
# Manually increase open file limits for the container to double Locust's
# minimum of 10,000.
RUN echo "root hard nofile 20000" >> /etc/security/limits.conf
RUN echo "root soft nofile 20000" >> /etc/security/limits.conf
RUN echo "* hard nofile 20000" >> /etc/security/limits.conf
RUN echo "* soft nofile 20000" >> /etc/security/limits.conf

FROM base as python
# Copy function code
COPY requirements.txt  .
RUN  pip3 install -r requirements.txt --target "${LAMBDA_TASK_ROOT}"
RUN  pip3 install -r requirements.txt

# Install the function's dependencies using file requirements.txt
# from your project folder.
FROM python as app
RUN git clone https://github.com/CMSgov/beneficiary-fhir-data.git /beneficiary-fhir-data
COPY . ${LAMBDA_TASK_ROOT}

# Set the CMD to your handler (could also be done as a parameter override outside of the Dockerfile)
CMD [ "app.handler" ]
