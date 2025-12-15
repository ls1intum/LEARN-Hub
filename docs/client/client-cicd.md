# Client Development and Deployment

## Quick Start

```bash
cd client/
make dev        # Starts Vite dev server on port 3001
make build      # Production build
make test       # Run tests in watch mode
make test:run   # Single test run (CI)
```

## Build System

### Development

Vite with native ES modules during development for fast server startup and hot module replacement (HMR):

```bash
make dev        # Dev server with HMR, source maps, full logging
```

### Production

Rollup optimisation with tree shaking, minification, and hash-based filenames for cache invalidation:

```bash
make build
```

Build output:
```
dist/
├── index.html              # Entry point
├── assets/
│   ├── index-[hash].js    # Bundled application code
│   └── index-[hash].css   # Bundled styles
```

## Testing Strategy

The test suite prioritises high-value regression protection with **108 tests running in ~900ms**.

**Tested**: Authentication state, API layer, custom hooks, data transformers, secure storage  
**Not Tested**: UI components, visual rendering, third-party libraries

**Running Tests**:
```bash
make test           # Watch mode
make test:run       # Single run (CI)
make test:ui        # UI mode
```

## Code Quality

- **ESLint**: React rules, TypeScript rules, accessibility rules
- **Prettier**: Automated formatting
- **TypeScript**: Static type checking with `npx tsc --noEmit`

**Pre-commit Workflow**:
```bash
make format && make lint && make test
```

## Production Deployment

### Multi-Stage Docker Build

- **Build Stage**: Node.js 20 Alpine installs dependencies and builds
- **Production Stage**: Nginx 1.25 Alpine serves built artifacts (~40MB final image)

### Nginx Configuration

Serves built assets via Nginx with:
- Security headers (X-Frame-Options, X-Content-Type-Options, CSP)
- SPA routing via `try_files $uri $uri/ /index.html`
- 1-year cache expiration for hash-named assets
- API proxying to server container at `/api/*`

### Container Orchestration

Docker Compose manages the application:

```bash
# Development
docker compose up --build -d

# Production (pre-built images)
docker compose -f compose.prod.yml up -d

# View logs
docker compose logs -f client
```

Health checks enable automatic recovery and rolling deployments.

## Environment Configuration

**Runtime Configuration**: Fetched from `/api/meta/environment`, enabling the same build to run across environments by connecting to different server instances.

**Development vs Production**:
- Development: Source maps, full logging, HMR, no minification, Vite proxy to `localhost:5001`
- Production: Minified code, generic error messages, security headers, Nginx proxy to server container

## Performance Optimisations

**Build-Time**:
- Tree shaking removes unused code
- Vite dependency optimisation (pre-bundling, CommonJS conversion)

**Runtime**:
- Debounced inputs for search/filter operations
- React Context for lightweight state management
- N+1 query elimination via bulk API fetches with component-level caching

## Related Documentation

- [Client Architecture](client-architecture.md) - Architecture decisions and design patterns
- [API Integration](api-integration.md) - Server integration patterns
- [Server Deployment](../server/server-cicd.md) - Server deployment procedures
