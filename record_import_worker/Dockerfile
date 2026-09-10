FROM dockerhub.kubekey.local/enlin/python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN python -m pip install --progress-bar off --no-cache-dir -r requirements.txt

COPY worker.py .

ENV PYTHONUNBUFFERED=1

CMD ["python", "worker.py"]