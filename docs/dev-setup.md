# Quick Local Development Setup

This guide provides a streamlined setup for running the LEARN-Hub server locally with a Docker-based PostgreSQL database.

## The Development Flow at a Glance

LEARN-Hub has three moving parts that you run locally:

| Part | Tech | Runs at | Started by |
|------|------|---------|------------|
| **Client** | React + Vite | http://localhost:3001 | `npm run dev` (hot reload) |
| **Server** | Spring Boot (Java 21) | http://localhost:5001 | `make dev` |
| **Database** | PostgreSQL 17 | localhost:5432 | Docker Compose |

The typical loop is:

1. **Once:** install prerequisites, copy `example.env` → `.env`, start PostgreSQL in Docker, run migrations, (optionally) seed demo data.
2. **Every day:** `make dev` from the repo root to run client + server together. The Vite client hot-reloads on save; restart the server to pick up Java changes.
3. **Before committing:** `make format` and `make test`.

If you prefer to run everything in containers instead of locally, jump to [Docker-based Development](#docker-based-development).

## Prerequisites

Ensure you have the following tools installed:

- **Java 21 or higher** - Required for running the server
- **Maven 3.9+** - Required for building (or use included wrapper `./mvnw`)
- **npm** - Required for running the client locally
- **Docker** - Required for running PostgreSQL locally
- **LibreOffice** - Required when running the Spring Boot server locally because DOCX generation uses `soffice`

### Verifying Prerequisites

```bash
# Check Java version
java -version  # Should show version 21 or higher

# Check npm version
npm -version

# Check Docker version
docker --version

# Check LibreOffice / soffice availability
soffice --version
```

On macOS, install LibreOffice with:

```bash
brew install --cask libreoffice
```

The local development profile expects LibreOffice at `/Applications/LibreOffice.app/Contents/MacOS/soffice`.

## Environment Variables

Create a `.env` file in the project root based on `example.env`:

```bash
cp example.env .env
```

`example.env` already ships with working defaults for local development – ports (`SERVER_PORT=5001`, `CLIENT_PORT=3001`), the PostgreSQL connection, and a TUM-hosted LLM endpoint. The minimum you should set before the first run:

- `PDF_PATH` - an existing folder on your machine where uploaded PDFs are stored
- `LLM_API_KEY` - API key for the OpenAI-compatible chat endpoint (needed for activity creation; leave the default `LLM_BASE_URL` / `LLM_MODEL_NAME` unless you use your own model)
- `INITIAL_ADMIN_EMAIL` / `INITIAL_ADMIN_PASSWORD` - a known admin login created by the seeder (see [Database Seeding](#3-database-seeding-optional-but-recommended))

Required only if you need teacher email verification / credential emails:
- `EMAIL_ADDRESS` - the from address on login emails
- `EMAIL_USERNAME` / `EMAIL_PASSWORD` - SMTP credentials
- `SMTP_SERVER` / `SMTP_PORT` - SMTP host and port (default: `postout.lrz.de:587`)

Optional variables (add to `.env` manually – not all are listed in `example.env`):
- `LLM_IMAGE_MODEL_NAME` - image model for exercise illustrations; reuses `LLM_BASE_URL`/`LLM_API_KEY` and is enabled when set
- `ADOBE_PDF_SERVICES_CLIENT_ID` / `ADOBE_PDF_SERVICES_CLIENT_SECRET` - Adobe PDF Services for PDF-to-DOCX conversion
- `DOCX_CACHE_PATH` - cache directory for converted DOCX files
- `SESSION_TIMEOUT` / `SESSION_COOKIE_MAX_AGE` - session and persistent-cookie lifetime overrides
- `VERBOSE_LOGGING` - set `true` for more detailed server logs

## Setup

### 1. Install dependencies (server and client)

```bash
make setup
```

This will:
- Resolve Java dependencies for the Spring Boot server
- Install npm dependencies for the React client

`make setup` does not install LibreOffice. Install that separately before starting the server locally.

### 2. Database Setup

Start the PostgreSQL database using Docker Compose:

```bash
docker compose -f docker/compose.yml up postgres -d
```

Run migrations (create db tables):

```bash
make db-migrate
```

### 3. Database Seeding (Optional but Recommended)

To populate the database with demo data, set this in your `.env`:

```properties
DB_SEED_ENABLED=true
```

Then start the server (next step). The seeder will:
- Load the full dataset if `dataset/dataset.csv` and `dataset/pdfs/` are present, otherwise fall back to a handful of demo activities
- Create an initial **admin** user so you can log in immediately

**Set a known admin login** so you don't have to dig through the logs. Add these to your `.env` before the first start:

```properties
INITIAL_ADMIN_EMAIL=admin@learnhub.com
INITIAL_ADMIN_PASSWORD=choose-a-strong-password
```

If you leave them blank, the seeder generates a random admin password and prints it to the server logs on first startup. Either way, log in via the **Password Login** tab at [http://localhost:3001/login](http://localhost:3001/login).

### 4. Run

Run both client and server locally:

```bash
make dev
```

This starts:
- **Server** on http://localhost:5001
- **React client** on http://localhost:3001

To run just the server:

```bash
cd server
make dev
```

To run just the client:

```bash
cd client
npm run dev
```

## Verification

Once the server is running, verify the setup:

1. **API Health Check**: Visit [http://localhost:5001/api/hello](http://localhost:5001/api/hello)
   - Should return: `{"message": "Hello, world!"}`

2. **API Documentation**: Visit [http://localhost:5001/api/openapi/swagger](http://localhost:5001/api/openapi/swagger)
   - Should show the Swagger UI with all available endpoints

3. **Client UI**: Visit [http://localhost:3001](http://localhost:3001)
   - Should display the LEARN-Hub landing page
   - Browse as a guest, or sign in at [/login](http://localhost:3001/login) with your `INITIAL_ADMIN_EMAIL` / `INITIAL_ADMIN_PASSWORD` (Password Login tab) to reach the admin features (user management, drafts)
   - Use the controls in the top-right to switch language (English / German) and theme (light / dark)

## Stopping Services

To stop the database:

```bash
docker compose -f docker/compose.yml down
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
docker compose -f docker/compose.yml ps
```

If not running, start it:

```bash
docker compose -f docker/compose.yml up postgres -d
```

### Flyway Migration Errors

If you encounter migration errors, you can clean and reapply:

```bash
make db-clean
make db-migrate
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
docker compose -f docker/compose.yml up --build
```

The server Docker image already installs LibreOffice (`libreoffice-writer`), so no extra host setup is required for containerized runs.

Access:
- Client: http://localhost:3001
- Server: http://localhost:5001
- API Docs: http://localhost:5001/api/openapi/swagger

## Next Steps

- Review the [Server README](../server/README.md) for detailed server documentation
- Check the [API Documentation](http://localhost:5001/api/openapi/swagger) for available endpoints
- Read the [Main README](../README.md) for project architecture and design decisions
