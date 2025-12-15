# Quick Local Development Setup

This guide provides a streamlined setup for running the LEARN-Hub server locally with a Docker-based PostgreSQL database.

## Prerequisites

Ensure you have the following tools installed:

- **uv** - Required for running the server locally
- **npm** - Required for running the client locally
- **Docker** - Required for running PostgreSQL locally

## Environment Variables
Create a `.env` file in the project root based on `example.env`:

```bash
cp example.env .env
```

You must update:
- `EMAIL_ADDRESS` - the from address on login emails
    - any address you have setup in your TUM account
    - default to tum_id@mytum.de
- `EMAIL_USERNAME` - your 7-character tum id (xx99abc)
- `EMAIL_PASSWORD` - your TUM password
- `PDF_PATH` - where pdfs will be stored on your machine
- `LLM_API_KEY` - LLM-assisted activity creation, chair's infra

## Setup

### 1. Install dependencies (server and client)
```bash
make setup
```

### 2. Database Setup

Start the PostgreSQL database using Docker Compose:

```bash
docker compose -f compose.dev.yml up -d
```

Run migrations (create db tables)
```bash
make db-setup
```

(Recommended) Populate DB with mock or thesis dataset
```bash
make db-mock
```
OR
```bash
make db-dataset
```
> both of the above print auto-generated admin credentials


### 3. Run

Run both client and server locally
```bash
make dev
```

## Verification

Once the server is running, verify the setup:

1. **API Health Check**: Visit [http://localhost:5001/api/hello](http://localhost:5001/api/hello)
2. **API Documentation**: Visit [http://localhost:5001/api/openapi/swagger](http://localhost:5001/api/openapi/swagger)
3. **Client UI**: Visit [http://localhost:3001](http://localhost:3001)

You should see the Swagger UI with all available endpoints.

## Stopping Services

To stop the database:

```bash
docker compose -f compose.dev.yml down
```

To stop the server, press `Ctrl+C` in the terminal where it's running.
