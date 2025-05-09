# Leave Management Service

## Docker Usage

### Build Docker Image
```
docker build -t leave-management-service .
```

### Run with Docker
```
docker run -p 8082:8082 --env-file .env leave-management-service
```

## With Docker Compose
This service is ready to be used in a multi-service Docker Compose setup. Ensure your compose file includes:

```
  leave-service:
    build: ./leave-management-service
    ports:
      - "8082:8082"
    env_file:
      - ./leave-management-service/.env
    depends_on:
      - db
```

- Exposes port 8082
- Reads environment variables from `.env`
- Connects to a shared database (see your compose file for DB service name)