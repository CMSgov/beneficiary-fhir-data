FROM python:3.11-slim as base

COPY . /app
WORKDIR /app
RUN mkdir logs

RUN pip3 install --upgrade pip
RUN pip3 install -r requirements.txt

WORKDIR /app/services/server-load/controller
EXPOSE 5557
CMD ["python3", "controller.py"]
