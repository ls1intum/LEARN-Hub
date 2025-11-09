# Client Architecture

## Overview

The LEARN-Hub client implements a React single-page application (SPA) designed for educational activity discovery and lesson planning. The architecture emphasizes simplicity, type safety, and user experience while maintaining clear separation between presentation, business logic, and data access layers.

## Technology Stack

**React 19** with **TypeScript**: Modern React features with compile-time type safety. The research prototype status prioritizes access to current features over backward compatibility.

**Vite**: Fast build tool with hot module replacement for rapid iteration during development.

**Tailwind CSS**: Utility-first CSS colocated with components, eliminating separate stylesheet files.

**shadcn/ui + Radix UI**: Accessible component primitives copied into the project for full customization and transparency.

## State Management

### React Context over Redux

The application uses React Context for global state management rather than Redux or Zustand. Two context providers manage authentication (`AuthContext`) and theme preferences (`ThemeContext`).

**Rationale**: The limited scope (two pieces of global state) and infrequent state changes don't justify Redux's complexity. Context is built into React, has no learning curve, and provides sufficient abstraction for the manageable component tree depth. Separating authentication and theme concerns prevents unnecessary coupling.

## Authentication and Security

### Token Storage Strategy

JWT tokens are stored in `sessionStorage` with tokens sent via `Authorization` headers, providing strong CSRF protection by design.

**Security Benefits**:
- Immune to CSRF attacks (no automatic cookie transmission)
- Automatic cleanup when browser session ends
- Tab isolation prevents token sharing across tabs
- Limited persistence reduces exposure on shared computers

**Educational Context**: The application is designed for school environments where students use shared lab computers for short class periods. Session-based storage aligns with this usage pattern—tokens naturally expire when students close their browsers, preventing the next student from inheriting an authenticated session.

**Implementation**: The `secureStorage` utility abstracts storage operations, handles errors gracefully (e.g., private browsing mode), and isolates implementation details for easy future changes.

### Authentication Flow

Token-based pattern: credentials exchanged for access/refresh tokens → stored in sessionStorage → included in API Authorization headers → automatic token refresh on 401 responses → cleared on logout. This eliminates server-side session management, improving scalability.

## Data Synchronization

### Field Values

Client-side constants (`client/src/constants/fieldValues.ts`) mirror server enums (`server/app/core/models.py`) for activity formats, topics, and Bloom levels.

**Rationale**: Instant form rendering without API calls. Simplicity over API fetch complexity. Requires manual synchronization enforced by development guidelines and server-side validation.

**Trade-off**: Risk of desynchronization mitigated by clear documentation, server validation, and integration tests.

### API Integration

The `apiService` centralizes all server communication with automatic token management, standardized error handling, TypeScript type safety, and development logging. Consistent patterns across the application simplify debugging.

## Component Architecture

Feature-based organization over technical classification:

```
src/components/
├── ui/          # Reusable primitives (shadcn/ui)
├── forms/       # Form components
├── layout/      # Navigation and page layout
└── favourites/  # Feature-specific components
```

Custom form components (`BadgeSelector`, `PriorityToggle`, `RangeSlider`, `FormField`, `FormSection`) abstract common patterns for consistency.

## Error Handling

**Error Boundary**: Top-level `ErrorBoundary` component catches unhandled React errors, displays user-friendly messages, and logs to console.

**API Errors**: Automatic token refresh retry for 401 responses. Manual retry via UI buttons for other failures. Client-side validation provides immediate feedback.

## Styling and Theming

**Dark Mode**: System preference detection with manual override. Theme choice persisted in localStorage. Smooth CSS transitions.

**Tailwind Configuration**: Semantic color naming (primary, secondary, destructive) rather than appearance-based. Colors defined for both light and dark themes. Separates design decisions from component implementations.

## Performance

The application prioritizes simplicity over premature optimization. Performance enhancements are applied based on profiling results rather than speculation.

## Development Experience

**Hot Module Replacement**: Vite's HMR preserves application state during development for rapid iteration.

**TypeScript**: Compile-time type checking catches errors before runtime. IDE autocomplete and refactoring support.

**Code Quality**: ESLint catches common mistakes and enforces best practices. Prettier ensures consistent code style.

## Testing Philosophy

The client employs a targeted testing approach prioritizing critical business logic over comprehensive coverage, achieving **108 tests in ~900ms**.

**Tested**: Authentication flows, API layer, state management hooks, data transformers, secure storage  
**Not Tested**: UI components, visual rendering, third-party libraries

**Tools**: Vitest (test runner), React Testing Library (hooks/components), MSW (API mocking)

Tests colocated in `__tests__` directories maximize regression protection while maintaining fast feedback loops.

## Design Principles

**Simplicity**: Straightforward solutions over complex abstractions. Context over Redux. Client-side field values over API synchronization.

**Type Safety**: TypeScript catches errors at compile time and enables confident refactoring.

**Security**: Technical decisions consider security implications (sessionStorage for tokens, client-side validation).

**Error Handling**: Error boundaries catch unhandled React errors. Graceful degradation with clear error messages.

These principles guided technical decisions throughout the implementation.
