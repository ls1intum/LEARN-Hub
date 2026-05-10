# Client Architecture

## Overview

The LEARN-Hub client implements a React single-page application for educational activity discovery and lesson planning. The architecture emphasises simplicity, type safety, and user experience while maintaining clear separation between presentation, business logic, and data access layers.

## Technology Stack

**React 19** with **TypeScript** for compile-time type safety and modern React features, appropriate for research prototype prioritising current features.

**Vite** for fast build tooling with hot module replacement enabling rapid iteration.

**Tailwind CSS** for utility-first CSS colocated with components.

**shadcn/ui + Radix UI** for accessible component primitives copied into the project for full customisation and transparency.

## State Management

The application uses **React Context** for global state management rather than Redux or Zustand. Two context providers manage authentication (`AuthContext`) and theme preferences (`ThemeContext`).

**Rationale**: The limited scope (two pieces of global state) and infrequent state changes do not justify Redux's complexity. Context is built into React with no learning curve and provides sufficient abstraction for the component tree.

## Authentication and Security

### Session Strategy

Authentication uses a server-side Spring Security session identified by an `HttpOnly` cookie:
- **Server Authority**: Authentication state stays on the backend, simplifying logout and revocation
- **Persistent Login**: Session cookies can survive app/browser restarts when configured with a max age
- **CSRF Protection**: The client fetches a CSRF token cookie and sends it on mutating requests

Implementation: `authService` automatically includes `credentials: "include"` on API requests and attaches the `X-XSRF-TOKEN` header when needed.

### Authentication Flow

Session pattern: credentials exchanged for a server-side session → browser stores the session cookie → client rehydrates user state via `/api/auth/me` → logout invalidates the server-side session.

## Data Synchronization

### Field Values

The client defines default field values in `src/constants/fieldValues.ts` for instant form rendering. The server exposes `/api/meta/field-values` for API-driven clients.

**Trade-off**: Hardcoded defaults risk drift; server-side validation remains the source of truth preventing invalid values from being persisted.

### API Integration

The `apiService` centralises server communication with session-aware requests, CSRF handling, standardised error handling, and type-safe request/response processing. This abstracts HTTP communication from UI components.

## Component Architecture

Feature-based organisation over technical classification:

```
src/components/
├── ui/          # Reusable primitives (shadcn/ui)
├── forms/       # Form components
├── layout/      # Navigation and page layout
└── favourites/  # Feature-specific components
```

Custom form components (`BadgeSelector`, `PriorityToggle`, `RangeSlider`) abstract common patterns for consistency.

## Error Handling

**Error Boundary**: Top-level component catches unhandled React errors, displays user-friendly messages, and logs to console.

**API Errors**: Session expiry resolves to a 401 and the UI falls back to the login flow; manual retry via UI buttons handles other failures. Client-side validation provides immediate feedback.

## Styling and Theming

**Dark Mode**: System preference detection with manual override; theme choice persisted in localStorage.

**Semantic Color Naming**: Primary, secondary, destructive colours defined for both light and dark themes, separating design decisions from component implementations.

## Performance

The application prioritises simplicity over premature optimisation. Key optimisation: **N+1 Query Elimination** via activity list pages fetching all favourites in a single API call, then passing pre-computed status to individual buttons for O(1) lookups instead of N individual status checks.

## Development Experience

- **Hot Module Replacement**: Vite's HMR preserves application state during development
- **TypeScript**: Compile-time type checking catches errors before runtime
- **Code Quality**: ESLint enforces best practices; Prettier ensures consistent style
- **Testing**: Vitest, React Testing Library, MSW with 108 tests running in ~900ms

## Design Principles

**Simplicity**: Straightforward solutions over complex abstractions. Context over Redux.

**Type Safety**: TypeScript catches errors at compile time and enables confident refactoring.

**Security**: Technical decisions consider security implications (HttpOnly cookies, CSRF protection, client-side validation).

**Error Handling**: Error boundaries catch unhandled React errors with graceful degradation.

These principles guided technical decisions throughout the implementation.
