# Quick Local Development Setup (Spring Boot)

This guide provides a streamlined setup for running the LEARN-Hub Spring Boot server locally with a Docker-based PostgreSQL database.

## Prerequisites

Ensure you have the following tools installed:

- **Java 21 or higher** - Required for running the Spring Boot server
- **Maven 3.9+** - Required for building (or use included wrapper `./mvnw`)
- **npm** - Required for running the client locally
- **Docker** - Required for running PostgreSQL locally

### Verifying Prerequisites

```bash
# Check Java version
java -version  # Should show version 21 or higher

# Check Maven version
mvn -version   # Should show version 3.9 or higher

# Check npm version
npm -version

# Check Docker version
docker --version
```

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
- `JWT_SECRET_KEY` - Secret key for JWT token signing

## Setup

### 1. Install dependencies (server and client)

```bash
make setup
```

This will:
- Install Java dependencies for the Spring Boot server
- Install npm dependencies for the React client

### 2. Database Setup

Start the PostgreSQL database using Docker Compose:

```bash
docker compose -f compose-spring.yml up postgres -d
```

Run migrations (create db tables):

```bash
cd server-spring
make db-migrate
cd ..
```

### 3. Database Seeding (Optional but Recommended)

To populate the database with demo data, set this in your `.env`:

```properties
app.db-seed.enabled=true
```

Then start the server (next step). The seeder will:
- Create 5 demo activities
- Create an admin user with auto-generated credentials (check logs)

Alternatively, you can use the legacy Flask seeding:

```bash
cd server
make db-mock
cd ..
```

### 4. Run

Run both client and server locally:

```bash
make dev
```

This starts:
- **Spring Boot server** on http://localhost:5001
- **React client** on http://localhost:3001

To run just the server:

```bash
cd server-spring
make dev
```

To run just the client:

```bash
cd client
make dev
```

## Verification

Once the server is running, verify the setup:

1. **API Health Check**: Visit [http://localhost:5001/api/hello](http://localhost:5001/api/hello)
   - Should return: `{"message": "Hello, world!"}`

2. **API Documentation**: Visit [http://localhost:5001/api/openapi/swagger](http://localhost:5001/api/openapi/swagger)
   - Should show the Swagger UI with all available endpoints

3. **Client UI**: Visit [http://localhost:3001](http://localhost:3001)
   - Should display the LEARN-Hub web interface

## Stopping Services

To stop the database:

```bash
docker compose -f compose-spring.yml down
```

To stop the server, press `Ctrl+C` in the terminal where it's running.

## Common Issues

### Port Already in Use

If port 5001 is already in use, change it in `.env`:

```properties
SERVER_PORT=5002
```

### Database Connection Failed

Check if PostgreSQL is running:

```bash
docker compose -f compose-spring.yml ps
```

If not running, start it:

```bash
docker compose -f compose-spring.yml up postgres -d
```

### Flyway Migration Errors

If you encounter migration errors, you can clean and reapply:

```bash
cd server-spring
make db-clean
make db-migrate
cd ..
```

**Warning**: This will delete all data in the database.

### Java Version Mismatch

Ensure you have Java 21 or higher:

```bash
java -version
```

If you have multiple Java versions, set `JAVA_HOME`:

```bash
export JAVA_HOME=/path/to/java21
```

## Docker-based Development

To run the entire stack (PostgreSQL + Spring Boot + React) in Docker:

```bash
docker compose -f compose-spring.yml up --build
```

Access:
- Client: http://localhost:3001
- Server: http://localhost:5001
- API Docs: http://localhost:5001/api/openapi/swagger

## Next Steps

- Review the [Spring Boot Server README](../server-spring/README.md) for detailed documentation
- Check the [API Documentation](http://localhost:5001/api/openapi/swagger) for available endpoints
- Read the [Main README](../README.md) for project architecture and design decisions

## Differences from Flask Version

### Database Migrations
- **Flask**: Uses Alembic with Python migration scripts
- **Spring Boot**: Uses Flyway with SQL migration scripts

### Running the Server
- **Flask**: `cd server && make dev`
- **Spring Boot**: `cd server-spring && make dev`

### Database Seeding
- **Flask**: `cd server && make db-mock`
- **Spring Boot**: Set `app.db-seed.enabled=true` in `.env` and start server

### Building
- **Flask**: No build step needed (Python is interpreted)
- **Spring Boot**: `cd server-spring && make build` (compiles to JAR)

## Migration Status

✅ **Completed**:
- All database models (JPA entities)
- Authentication & authorization (Spring Security + JWT)
- Activity endpoints (list, get, create, update, delete)
- User management endpoints
- Email service (verification codes)
- Database migrations (Flyway)
- LLM integration (Spring AI + Ollama)
- Docker setup
- OpenAPI documentation

The Spring Boot server is **fully functional** and maintains **100% API compatibility** with the Flask version.
