# Client Architecture

## Overview

React SPA implementing an activity recommendation system with priority scoring and field value synchronization. Built with role-based authentication and recommendation algorithms.

## Core Architecture

### Application Structure
- **Entry Point**: `main.tsx` - React StrictMode with context providers
- **Routing**: `App.tsx` - Protected routes with role-based access control
- **Authentication**: JWT-based with sessionStorage for enhanced security
- **State Management**: React Context (no external state library)

### Key Research Components

#### Recommendation System
- **Priority Scoring**: Users can mark categories for 2x scoring multiplier
- **Field Synchronization**: Client-first approach with server fallback
- **Age Range Validation**: Centralized constants (6-15 years) for form validation
- **Break Integration**: Embedded breaks via `break_after` field for better organization

#### Authentication & Security
- **Session-based Storage**: `secureStorage` utility uses sessionStorage for automatic cleanup
- **Role-based Access**: ADMIN/TEACHER roles with different capabilities
- **Token Management**: Automatic refresh with graceful error handling

## Component Architecture

### Service Layer
- **`authService`** - JWT token management with sessionStorage integration
- **`apiService`** - Comprehensive API service with standardized request handling
- **`logger`** - Environment-aware logging for development debugging
- **`secureStorage`** - Session-based token storage with error resilience

### UI Component System
- **shadcn/ui** - Radix UI primitives with Tailwind styling
- **Modular Components**: Form fields, layout, file handling, and display components
- **Form Components**: BadgeSelector, FormField, FormSection, NumberField, PriorityToggle, RangeSlider, SelectField
- **Layout Components**: MainLayout, NavigationMenu, NavigationItem, UserHeader
- **File Components**: FileUploadArea, SelectedFileInfo
- **Display Components**: TagList, TimelineContainer, TimelineItem, BreakCard
- **Custom Hooks**: `useFieldValues` for field synchronization, `useApiError` for error handling

### Key Patterns

#### Error Handling & Resilience
- **Global Error Boundary**: `ErrorBoundary` for unhandled React errors
- **Break Duration Fallback**: Handles server field name inconsistencies
- **Automatic Retry Logic**: Retries without breaks when server has processing errors
- **Graceful Degradation**: Users always get recommendations, even when breaks fail

#### Data Flow
1. User actions trigger API calls via service layer
2. Services handle authentication and error responses
3. Components respond to API responses
4. Context providers manage global state

## File Organization

```
src/
├── components/          # Reusable UI components
│   ├── ui/             # shadcn/ui + custom form/display components
│   ├── forms/          # Form-specific components
│   ├── layout/         # Navigation and layout components
│   └── favourites/     # Favourites-specific components
├── services/           # API and business logic
├── types/              # TypeScript definitions (activity.ts, api.ts, test.ts)
├── utils/              # Utilities (secureStorage, converters)
└── hooks/              # Custom React hooks
```

## Research-Relevant Features

### Field Values Synchronization
- **Client-First Approach**: Field values primarily sourced from client-side constants
- **Type Safety**: TypeScript types generated from field values for compile-time validation
- **Priority Categories**: `age_appropriateness`, `bloom_level_match`, `topic_relevance`, `duration_fit`

### Recommendation Display
- **Structured Layout**: Individual recommendations as distinct rows with embedded breaks
- **Visual Quality Indicators**: Color-coded score indicators with detailed tooltips
- **Interactive Selection**: Dedicated action buttons for lesson plan creation

### Favourites System
- **Two Types**: Separate favourites for activities and lesson plans
- **Real-time Status**: Live favourite status checking with visual indicators
- **Custom Names**: Users can add custom names to lesson plan favourites

## Testing Strategy

### Test Structure
- **Framework**: Vitest with React Testing Library for component testing
- **Focus**: High-value tests covering authentication, utilities, and custom hooks

### Test Organization
- **Authentication service testing** - Token management, API calls, error handling
- **Custom form hook testing** - Form state management and validation
- **Utility function testing** - SessionStorage operations and error resilience

### Test Commands
- `npm run test` - Run tests in watch mode
- `npm run test:run` - Run tests once
- `npm run test:ui` - Run tests with UI interface
- `make test` - Run tests via Makefile

### Test Philosophy
- **Quality over Quantity**: Focus on critical business logic, not exhaustive coverage
- **Essential Functionality**: Authentication, token management, form state, storage utilities
- **Error Handling**: Network failures, invalid data, edge cases

### Test Environment
- **Vitest Configuration**: Single-threaded execution for stability
- **Global Mocks**: Mocking setup in `src/test/setup.ts`
- **SessionStorage Mocking**: Security-focused token storage testing
- **Component Mocking**: Heavy components mocked for performance

## Implementation Details

For detailed implementation, see:
- **Authentication**: `src/services/authService.ts`, `src/utils/secureStorage.ts`
- **API Integration**: `src/services/apiService.ts`, `src/types/api.ts`
- **Form Components**: `src/components/forms/`, `src/components/ui/`
- **Field Values**: `src/constants/fieldValues.ts`, `src/hooks/useFieldValues.ts`
- **Error Handling**: `src/components/ErrorBoundary.tsx`, `src/services/logger.ts`
- **Testing**: `src/test/setup.ts`, test files in `src/services/`, `src/hooks/`, `src/utils/`

## Technical Decisions

### SessionStorage Implementation
- **Security**: Automatic cleanup when browser session ends
- **User Experience**: Reduced token persistence for better security
- **Implementation**: `secureStorage` utility handles all token operations

### Field Values Strategy
- **Reliability**: Client-side constants as primary source
- **Performance**: Reduced API calls for field values
- **Fallback**: Server API as optional enhancement

### Component Architecture
- **Reusability**: Common UI patterns extracted into reusable components
- **Maintainability**: Reduced code duplication across forms
- **Type Safety**: Strongly typed component interfaces