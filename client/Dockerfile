# syntax=docker/dockerfile:1
# Multi-stage build for optimized production image
# Target both arm64 and amd64 architectures

# Build stage
FROM --platform=$BUILDPLATFORM node:20-alpine AS builder

# Install dependencies for building native modules
RUN apk add --no-cache libc6-compat

# Set working directory
WORKDIR /app

# Copy ONLY dependency files first (most stable)
COPY package.json package-lock.json* ./

# Cache dependencies separately
RUN --mount=type=cache,target=/root/.npm \
    npm ci --no-audit --no-fund

# Copy source code (changes invalidate cache only from here)
COPY src ./src
COPY public ./public
COPY index.html vite.config.ts tsconfig.json tailwind.config.js postcss.config.js eslint.config.js ./

# Build the application
RUN npm run build

# Production stage with nginx
FROM nginx:1.25-alpine AS runner

# Add labels for better image management
LABEL org.opencontainers.image.title="LEARN-Hub Client"
LABEL org.opencontainers.image.description="React client for LEARN-Hub application"
LABEL org.opencontainers.image.vendor="LEARN-Hub Project"

# Use existing nginx user for security (already exists in nginx:alpine)

# Copy built application
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Set proper permissions
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    chown -R nginx:nginx /etc/nginx/conf.d && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

# Switch to non-root user (nginx user already exists)
USER nginx

# Expose port
EXPOSE 3001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:3001/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
