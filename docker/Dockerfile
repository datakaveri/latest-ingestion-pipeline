FROM python:3.8-slim-buster

WORKDIR /usr/share/app
COPY requirements.txt .
RUN apt update && python -m pip install --upgrade pip && pip install -r requirements.txt 
ENTRYPOINT ["python", "redis-client-async.py"]

