# Client Testing

This directory contains the minimal testing setup for the client, focused on high-value business logic.

## Test Structure

- **`setup.ts`** - Global test configuration and mocks
- **`../services/__tests__/`** - Service layer tests (AuthService)
- **`../hooks/__tests__/`** - Custom hook tests
- **`../contexts/__tests__/`** - Context provider tests (AuthContext)
- **`../lib/__tests__/`** - Utility function tests

## Running Tests

```bash
# Run all tests
npm run test:run

# Run tests in watch mode
npm run test

# Run tests with UI
npm run test:ui

# Using Makefile
make test
make test-ui
```

## Testing Philosophy

This minimal test suite focuses on:

1. **Critical Business Logic** - Authentication, token management, API integration
2. **Complex State Management** - Custom hooks with localStorage and API calls
3. **Error Handling** - Network failures, invalid data, edge cases
4. **Integration Points** - Service layer interactions

## What We Test

### ✅ High Value (Included)

- **AuthService** - Token management, API calls, error handling
- **AuthContext** - Authentication state management
- **Utils** - Pure utility functions

### ❌ Lower Value (Excluded)

- Simple UI components without business logic
- Basic form inputs without validation
- Static content rendering
- CSS/styling tests

## Test Coverage

The tests focus on:

- **Happy paths** - Normal operation
- **Error scenarios** - Network failures, invalid data
- **Edge cases** - Empty states, corrupted data
- **State transitions** - Loading, success, error states

This approach provides maximum value with minimal maintenance overhead.
