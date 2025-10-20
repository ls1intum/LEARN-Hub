# CI/CD & Deployment

## Overview

Modern React SPA with Vite build system, testing strategy, and Docker deployment. Focus on performance optimization, type safety, and development workflow efficiency.

## Build System

### Development Stack
- **Vite** - Fast development server with HMR
- **TypeScript** - Type checking and compilation
- **React** - React features and performance improvements
- **Tailwind CSS** - Utility-first styling with dark mode support

### Production Build
- **Optimized Bundle** - Tree shaking, code splitting, asset optimization
- **Type Safety** - TypeScript compilation with strict mode
- **Static Serving** - Built files served via npx serve
- **Docker Multi-stage** - Optimized container image

## Testing Strategy

### Test Structure
- **Vitest Framework** - Fast unit testing with Vite integration
- **Testing Library** - Component testing utilities for hooks
- **Quality over Quantity** - Essential functionality, not implementation details

### Test Coverage
- **Authentication Service** - Token management, API calls, error handling
- **Custom Hooks** - Form state management and validation
- **Utility Functions** - SessionStorage operations and error resilience

### Test Commands
```bash
npm run test        # Watch mode
npm run test:run    # Single run
npm run test:ui     # UI mode
make test           # Via Makefile
```

### Test Philosophy
- **High-Value Testing** - Focus on critical business logic and error paths
- **Essential Functionality** - Authentication, form state, storage utilities
- **Error Handling** - Network failures, invalid data, edge cases

## Code Quality

### Linting & Formatting
- **ESLint** - JavaScript/TypeScript linting with React hooks rules
- **Prettier** - Consistent code formatting
- **Type Checking** - TypeScript compilation before build

### Pre-commit Checks
- Lint and format validation
- Type checking before build
- Test execution for critical paths

## Docker Configuration

### Multi-stage Build
```dockerfile
# Dependencies stage
FROM node:20-alpine AS deps
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

# Build stage  
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Production stage
FROM node:20-alpine AS runner
WORKDIR /app
COPY --from=builder /app/dist ./dist
EXPOSE 3000
CMD ["npx", "serve", "-s", "dist", "-l", "3000"]
```

### Container Features
- **Node.js 20 Alpine** - Lightweight base image
- **Non-root user** - Security best practices
- **Static file serving** - Production-ready serving with npx serve
- **Multi-stage build** - Optimized image size

## Development Workflow

### Local Development
```bash
# Install dependencies
make install

# Start development server
make dev

# Run tests
make test

# Format and lint
make format
make lint
```

### Production Build
```bash
# Build for production
make build

# Build Docker image
make docker-build

# Run Docker container
make docker-run
```

## Performance Optimization

### Build Optimizations
- **Tree shaking** - Remove unused code
- **Code splitting** - Route-based splitting
- **Asset optimization** - Image and CSS minification
- **Bundle analysis** - Size monitoring

### Runtime Optimizations
- **Lazy loading** - Route-based code splitting
- **Memoization** - React.memo for expensive components
- **Debounced inputs** - Search and form inputs

## Features & Components

### Type System
- **API types** - Strongly typed request/response interfaces
- **Form data types** - Form field handling with proper typing
- **Error handling types** - Standardized error response structures

### UI Components
- **Form Components** - BadgeSelector, FormField, FormSection, NumberField, PriorityToggle, RangeSlider, SelectField
- **File Handling** - FileUploadArea, SelectedFileInfo
- **Layout Components** - MainLayout, NavigationMenu, NavigationItem, UserHeader
- **Display Components** - TagList, TimelineContainer, TimelineItem, BreakCard

### API Service
- **API Methods** - Full API coverage with standardized responses
- **Type Safety** - Strongly typed API responses with type system
- **Authentication** - Automatic JWT token management with sessionStorage
- **Logging Integration** - Development logging for API calls and errors

## Security Considerations

### Application Security
- **HTTPS only** - Secure communication
- **CSP headers** - Content Security Policy
- **XSS protection** - Input sanitization
- **CSRF protection** - Same-origin requests

## Troubleshooting

### Common Issues
- **Build failures** - TypeScript errors or dependency issues
- **Storage errors** - SessionStorage access issues in private browsing
- **Type errors** - API type mismatches or missing type definitions
- **Authentication issues** - Token storage or refresh problems

### Debug Commands
```bash
# Check build output
npm run build

# Run tests with verbose output
npm run test:run -- --reporter=verbose

# Run specific test file
npm run test:run -- services/__tests__/authService.test.ts

# Test secure storage functionality
# Check browser dev tools > Application > Session Storage

# Debug logging in development
# Check browser console for logger output (debug/info only in dev mode)
```

## Implementation References

For detailed implementation, see:
- **Build Configuration**: `vite.config.ts`, `tsconfig.json`
- **Docker Setup**: `Dockerfile`, `compose.yml`
- **Test Configuration**: `vitest.config.ts`, `src/test/setup.ts`
- **Test Files**: `src/services/__tests__/authService.test.ts`, `src/hooks/__tests__/useForm.test.ts`, `src/utils/__tests__/secureStorage.test.ts`
- **Package Management**: `package.json`, `Makefile`
- **Components**: `src/components/ui/`, `src/components/layout/`
- **Services**: `src/services/apiService.ts`, `src/services/authService.ts`, `src/services/logger.ts`
- **Utilities**: `src/utils/secureStorage.ts`