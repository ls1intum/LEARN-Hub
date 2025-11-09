# Client Development and Deployment

## Overview

This document describes the build system, testing strategy, and deployment procedures for the LEARN-Hub client.

## Build System

### Vite Build Tool

The application uses Vite, which uses native ES modules during development for fast server startup and hot module replacement (HMR). Production builds use Rollup for optimization (tree shaking, asset minification, hash-based filenames for cache invalidation).

**Development**:
```bash
cd client/
make dev        # Starts Vite dev server on port 3001
```

Development features: HMR, Fast Refresh, error overlay, source maps, no bundling, no minification.

**Production Build**:
```bash
make build      # Compiles TypeScript, bundles assets, optimizes output
```

Production optimizations:
- JavaScript/CSS minification
- Image optimization
- Hash-based filenames for aggressive caching
- Tree shaking to remove unused code

Build output structure:
```
dist/
├── index.html              # Entry point
├── assets/
│   ├── index-[hash].js    # Bundled application code
│   ├── index-[hash].css   # Bundled styles
│   └── ...                # Other generated assets
```

## Testing Strategy

The testing strategy prioritizes high-value tests over comprehensive coverage:

**Focus Areas**: Authentication flows, form state management, utility functions, error handling

**Not Tested**: Component visual rendering (manual testing), third-party libraries, simple getters/setters

**Framework**: Vitest (Vite-native, fast, TypeScript support) with React Testing Library (user-perspective component testing)

**Running Tests**:
```bash
cd client/
make test           # Watch mode
npm run test:run    # Single run (CI)
npm run test:ui     # UI mode
```

Tests are colocated with code (`src/services/__tests__/`, `src/hooks/__tests__/`, `src/utils/__tests__/`) and use single-threaded execution for stability.

## Code Quality

**ESLint**: React rules, TypeScript rules, accessibility rules
**Prettier**: Automated formatting
**TypeScript**: Static type checking with `npx tsc --noEmit`

**Pre-commit Workflow**:
```bash
make format && make lint && make test
```

## Production Deployment

### Multi-Stage Docker Build

**Build Stage**: Node.js 20 Alpine image installs dependencies (`npm ci`) and builds the application

**Production Stage**: Nginx 1.25 Alpine image serves built artifacts from `dist/`
- Only built artifacts copied (no source/dependencies)
- Runs as non-root nginx user
- Final image ~40MB vs ~400MB with full Node.js

### Nginx Configuration

Nginx was chosen for performance (optimized static file serving), security (built-in headers), production readiness, and SPA routing support.

**Key Features**:

Security headers (X-Frame-Options, X-Content-Type-Options, CSP) protect against clickjacking, MIME sniffing, and XSS.

SPA routing via `try_files $uri $uri/ /index.html` ensures all routes serve `index.html` for client-side routing.

Asset caching with 1-year expiration for hash-named files.

API proxying to server (proxies `/api/*` requests to server container).

### Container Orchestration

Docker Compose manages the multi-container application:

```bash
# Development
docker compose up --build -d

# Production (pre-built images from GitHub Container Registry)
docker compose -f compose.prod.yml up -d

# View logs
docker compose logs -f client
```

Production compose file uses pre-built images, implements health checks, defines resource limits, and configures service dependencies.

Health checks (`wget --spider http://localhost:3001/`) enable automatic recovery, rolling deployments, and monitoring integration.

## Performance Optimization

**Build-Time**:
- Tree shaking removes unused code
- Vite dependency optimization (pre-bundling, CommonJS to ES modules conversion)

**Runtime**:
- Debounced inputs for search/filter operations
- Efficient state management with React Context
- Component-level error boundaries for fault isolation

Performance optimizations are applied based on profiling results rather than speculative optimization.

## Environment Configuration

**Build-Time**: Bundler settings, TypeScript options, CSS processing

**Runtime**: API endpoints, feature flags, environment identification

Runtime configuration fetched from `/api/meta/environment`, enabling the same build to run across environments by connecting to different server instances.

**Development vs Production**:
- Development: Source maps, full logging, HMR, no minification, detailed errors, Vite proxy to `localhost:5001`
- Production: Minified code, generic errors, security headers, Nginx proxy to server container

## Related Documentation

- [Client Architecture](client-architecture.md) - Architecture decisions and design patterns
- [API Integration](api-integration.md) - Server integration patterns
- [Server Deployment](../server/server-cicd.md) - Server deployment procedures
