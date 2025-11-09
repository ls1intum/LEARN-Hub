# Server Architecture

## Overview

The LEARN-Hub server implements a Flask-based REST API designed to support educational activity recommendations through category-based scoring algorithms. The architecture emphasizes modularity, type safety, and scalability while maintaining a clear separation of concerns across application layers.

## System Architecture

### Layered Architecture

The server follows a classical layered architecture pattern with distinct responsibilities:

**API Layer** (`app/api/`): Exposes RESTful endpoints with automated OpenAPI documentation generation via Flask-OpenAPI3. This layer handles request validation, authentication enforcement, and response formatting.

**Service Layer** (`app/services/`): Encapsulates business logic and orchestrates interactions between different system components. Services are designed as independent units with clear interfaces, facilitating testing and modification.

**Core Engine** (`app/core/`): Implements the recommendation algorithm as a standalone package. This isolation allows the recommendation logic to be tested independently and potentially reused in other contexts.

**Data Access Layer** (`app/db/`): Manages database interactions through SQLAlchemy ORM models. This layer abstracts database operations and provides a consistent interface for data manipulation.

### Technology Choices

#### Flask Framework

Flask was selected for its lightweight nature and flexibility, which aligns well with the prototype scope of this project. Unlike heavier frameworks like Django, Flask allows for explicit control over components and dependencies, making architectural decisions more transparent. The Flask-OpenAPI3 extension provides automatic API documentation generation, reducing the overhead of maintaining separate documentation.

#### PostgreSQL Database

PostgreSQL serves as the primary data store for both development and production environments. This choice was driven by several factors:

**Relational Integrity**: The application's data model involves complex relationships between activities, topics, and user preferences. PostgreSQL's robust support for foreign keys, constraints, and transactions ensures data consistency.

**Production Readiness**: Unlike SQLite, PostgreSQL is designed for concurrent access and production workloads, eliminating the need for a different database system between development and deployment.

**Advanced Features**: PostgreSQL's support for array types, JSON columns, and full-text search provides flexibility without requiring external tools.

The use of SQLAlchemy as an ORM provides database abstraction while maintaining the ability to optimize queries when needed. Alembic manages schema migrations, allowing version-controlled database evolution that can be reviewed and tested before deployment.

#### Python 3.13

The project leverages Python 3.13's modern language features, including enhanced type hints and performance improvements. The decision to use current Python versions reflects a forward-looking approach while acknowledging that this is a research prototype rather than a system requiring long-term backward compatibility.

## Authentication Architecture

### Dual Authentication Model

The system implements two distinct authentication pathways to accommodate different user roles and security requirements:

**Administrative Authentication**: Administrators use traditional email and password authentication with JWT token management. This approach provides robust security for users who have full system access, including the ability to create, modify, and delete activities.

**Teacher Authentication**: Teachers can authenticate using either email verification codes or passwords. The verification code system was designed to reduce friction in the teacher onboarding process. Teachers can receive a 6-digit code via email and gain immediate access without remembering passwords, addressing a common usability challenge in educational software where teachers may access the system infrequently.

This dual approach represents a design tradeoff: prioritizing security for administrative functions while optimizing usability for the primary user group (teachers). The verification code system accepts slightly higher security risk (time-limited codes can be intercepted) in exchange for significantly improved user experience.

### JWT Token Management

The system uses JSON Web Tokens (JWT) for stateless authentication:

- **Access Tokens**: Short-lived (15 minutes) to limit exposure if compromised
- **Refresh Tokens**: Longer-lived (30 days) to maintain user sessions without repeated logins
- **Automatic Refresh**: Client applications automatically refresh expired access tokens

This token-based approach eliminates the need for server-side session storage, improving scalability and simplifying deployment across multiple server instances.

## Service Architecture

### Service Layer Pattern

The service layer implements the business logic as a collection of specialized services:

**PDFService**: Manages PDF document storage, retrieval, and lesson plan generation. This service consolidates all PDF-related operations, providing a single interface for document handling.

**RecommendationService**: Orchestrates the recommendation engine, translating API requests into core engine inputs and formatting results for API responses. This separation allows the recommendation algorithm to remain independent of web framework concerns.

**UserService**: Handles user management operations including registration, authentication, and profile updates. Complex operations like user deletion implement the Unit of Work pattern, ensuring all related data changes occur atomically within a single database transaction.

**EmailService**: Manages email delivery for verification codes. The service is configured differently for development (local SMTP with MailHog) and production (external SMTP provider), demonstrating environment-specific configuration management.

**UserSearchHistoryService & UserFavouritesService**: Track user preferences and interactions for personalized experiences and analytics.

### Service Design Rationale

Services are instantiated with explicit dependency injection, making dependencies visible and testable. This design choice favors clarity over convenience—while more verbose than implicit dependencies, it makes the system's structure explicit and facilitates unit testing with mock dependencies.

**Transaction Management**: Services that modify data support optional transaction control through an `auto_commit` parameter. This allows orchestrating services to manage complex multi-step operations atomically, ensuring data consistency. For example, user deletion removes verification codes, search history, favourites, and the user record in a single transaction—either all succeed or all roll back.

## External Integrations

### LLM Integration

The system integrates with Ollama (via LangChain) for PDF content extraction. The integration uses LangChain's native `ChatOllama` client with authentication via the `OLLAMA_API_KEY` environment variable.

**PDF Content Extraction**: Automated extraction of structured activity metadata from uploaded PDF documents. The LLM analyzes PDF text and extracts fields like activity name, description, age range, Bloom level, topics, duration, and resource requirements.

**Configuration**: 
- `LLM_BASE_URL`: Base URL of the Ollama server (e.g., `https://gpu.aet.cit.tum.de/ollama`)
- `LLM_API_KEY`: Bearer token for authentication (set as `OLLAMA_API_KEY` during client initialization)
- `LLM_MODEL_NAME`: Model name to use (e.g., `qwen3:30b-a3b`)

**Authentication**: The system sets `OLLAMA_API_KEY` environment variable temporarily during LLM client initialization, allowing LangChain's `ChatOllama` to authenticate with the remote Ollama server. The original environment variable value is restored after initialization.

**Error Handling**: The system includes comprehensive error handling:
- `LLMAuthenticationError`: Raised when authentication fails (401 errors)
- `LLMServiceError`: Raised for other LLM service errors (includes timeout errors after retry)
- `LLMTimeoutError`: Internal exception for timeout scenarios
- Automatic retry mechanism (one retry on timeout)
- Dynamic timeout calculation (1 second per 1000 tokens, minimum 10 seconds)
- Graceful handling of optional fields (empty lists allowed for topics and resources)

**Design Rationale**: Integrating LLM-based content processing addresses a significant challenge in educational content management—manually extracting and structuring information from diverse document formats is time-consuming and error-prone. The LLM approach provides:

- **Consistency**: Structured data extraction follows a defined schema regardless of document formatting
- **Efficiency**: Automated processing eliminates manual data entry
- **Flexibility**: The system can handle documents with varying layouts and structures

**Trade-offs**: This integration introduces external dependencies and API costs. The system requires network connectivity and is subject to API rate limits and costs. Additionally, LLM outputs require validation to ensure accuracy. These trade-offs were deemed acceptable for a research prototype where the benefits of automation outweigh the operational complexity.

**Lesson Plan Generation**: Lesson plan PDFs are generated programmatically using ReportLab (Python PDF library), not the LLM. The system creates summary pages with search criteria and activity details, then merges them with individual activity PDFs.

### Email Service Integration

Email delivery uses SMTP with environment-specific configuration. In development, MailHog provides a local SMTP server with a web interface for testing without sending real emails. In production, an external SMTP provider ensures reliable delivery. This configuration demonstrates the system's adaptability to different deployment contexts.

## API Architecture

### RESTful Design Principles

The API follows REST principles with resource-based URLs and appropriate HTTP methods:

- **GET**: Retrieve resources (activities, user data, metadata)
- **POST**: Create resources and trigger complex operations (recommendations, lesson plans)
- **PUT**: Update existing resources
- **DELETE**: Remove resources

Resource organization reflects the domain model:
- `/api/activities/` - Activity discovery and management
- `/api/auth/` - Authentication and user management
- `/api/history/` - User history and favourites
- `/api/documents/` - PDF upload and retrieval
- `/api/meta/` - System metadata and configuration

### Flask-OpenAPI3 Integration

Flask-OpenAPI3 automatically generates OpenAPI 3.0 specifications from Pydantic model annotations. This approach provides several benefits:

**Single Source of Truth**: API documentation is generated from the same type definitions used for validation, ensuring documentation accuracy.

**Type Safety**: Pydantic models provide runtime validation with detailed error messages, reducing the need for manual input validation code.

**Interactive Documentation**: The automatically generated Swagger UI allows developers and researchers to explore and test the API without writing test scripts.

This tooling choice reflects a preference for automation and type safety over manual documentation maintenance.

## Data Model Architecture

### Unified Data Flow

The system uses Pydantic models (`ActivityModel`) as the primary data transfer object throughout the application stack:

**Database → API → Core Engine**

This design eliminates the need for multiple intermediate representations and reduces the risk of data transformation errors. SQLAlchemy ORM models are converted to Pydantic models at the data access layer, and these Pydantic models flow through the entire application.

**Benefits**:
- Type safety with runtime validation
- Consistent data structures across layers
- Automatic serialization/deserialization
- Single source of truth for data schemas

### Database Models

Core entities include:

**User**: Stores user accounts with role-based permissions (ADMIN, TEACHER). The role system enables different authentication flows and access controls without requiring separate user tables.

**Activity**: Represents educational activities with comprehensive metadata. Each activity requires a PDF document reference, enforced at the database level through foreign key constraints and NOT NULL constraints on the `document_id` column.

**UserSearchHistory**: Tracks teacher search queries for analytics and personalized recommendations.

**UserFavourites**: Stores both individual activity favourites and complete lesson plans (activity sequences with breaks). Lesson plans are stored as JSON snapshots, capturing the complete state at the time of favouriting to ensure the experience can be replayed even if activities are modified.

### Migration Strategy

Alembic manages database schema evolution through version-controlled migration scripts. Each schema change is captured in a migration file that can be reviewed, tested, and applied consistently across environments. This approach ensures that database changes are traceable and reversible, critical for maintaining data integrity during system evolution.

## Configuration Management

### Environment-Based Configuration

The system uses Pydantic Settings for type-safe configuration management. Environment variables are mapped to strongly-typed configuration objects, preventing runtime errors from configuration mistakes. This approach also makes configuration requirements explicit—missing required values cause startup failures with clear error messages.

Configuration categories include:
- Database connection parameters
- LLM service credentials
- Email service settings
- Security keys (JWT, Flask session)
- Environment identification (local, staging, production)

The `ENVIRONMENT` variable enables runtime behavior adaptation without code changes, allowing the same container image to run in different deployment contexts.

## Performance Considerations

### Recommendation Engine Optimization

The recommendation engine implements several optimizations to maintain responsive performance:

**Two-Stage Scoring**: Activities are first scored without duration considerations (computationally expensive), then only top candidates are re-scored with complete criteria. This approach significantly reduces computation time for large activity sets.

**Combination Limits**: Lesson plan generation is limited to combinations of the top 20 activities rather than all available activities. This reduces the computational complexity from O(n^k) to O(20^k), where k is the number of activities per lesson plan.

**Early Filtering**: Hard filters eliminate incompatible activities before scoring, reducing the number of activities that require detailed evaluation.

These optimizations reflect a pragmatic approach to algorithm design—perfect accuracy is less valuable than timely responses in an interactive educational tool.

### Database Performance

The relatively small expected dataset (hundreds of activities, thousands of users) allowed for straightforward relational design without extensive optimization. Indexes on commonly queried fields (activity name, user email) provide adequate performance for the expected scale.

**Transaction Design**: Complex operations are implemented as atomic transactions to ensure data consistency. While this increases lock duration compared to multiple smaller transactions, the trade-off is acceptable given that these operations are infrequent (administrative tasks) and the benefits of guaranteed consistency outweigh the minimal performance impact.

## Deployment Architecture

### Containerization Strategy

The application is containerized using Docker with multi-stage builds to optimize image size and security:

**Build Stage**: Uses the `uv` tool to install dependencies and prepare the application environment.

**Production Stage**: Uses a minimal Python slim image with only runtime dependencies. The application runs with Gunicorn as the WSGI server, configured for concurrent request handling.

**Health Checks**: The container includes health check endpoints (`/api/hello`) that container orchestrators can use to verify application availability.

This containerization approach ensures consistent deployment across development and production environments while minimizing the attack surface through minimal base images.

### Resource Management

Docker Compose configurations specify resource limits (memory, CPU) to ensure predictable performance and prevent resource exhaustion. These limits are tuned based on expected load patterns and available infrastructure.

## Design Philosophy

The architecture reflects several guiding principles:

**Explicitness over Implicitness**: Dependencies, configurations, and data flows are explicit rather than hidden through framework magic. This increases verbosity but improves understandability for research and educational purposes.

**Type Safety**: Strong typing with Pydantic models and Python type hints catches errors during development rather than runtime.

**Modularity**: Clear boundaries between layers and services facilitate independent testing and modifications.

**Pragmatic Optimization**: Performance optimizations focus on actual bottlenecks rather than premature optimization, balancing simplicity with efficiency.

These principles guided technical decisions throughout the implementation, prioritizing clarity and maintainability appropriate for a research prototype.
