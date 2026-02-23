# Movento - Microservices Streaming Platform

A comprehensive microservices-based streaming platform built with Spring Boot, featuring user management, content streaming, recommendations, and payment processing.

##  Architecture Overview

Movento follows a **microservices architecture** pattern with service discovery, API gateway, and containerized deployment.

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Nginx LB      │    │   API Gateway   │    │  Service Registry│
│   (Port 80/443) │───▶│   (Port 8080)   │───▶│   (Port 8761)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
        ┌─────────────────────────────────────────────────────────┐
        │                    Microservices                      │
        │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
        │  │User Service │  │Streaming    │  │Payment      │    │
        │  │ (Port 8081) │  │Service      │  │Service      │    │
        │  │             │  │ (Port 8083) │  │ (Port 8082) │    │
        │  └─────────────┘  └─────────────┘  └─────────────┘    │
        │  ┌─────────────┐                                      │
        │  │Recommendation│                                     │
        │  │Service      │                                     │
        │  │ (Port 8084) │                                     │
        │  └─────────────┘                                      │
        └─────────────────────────────────────────────────────────┘
                                │
                                ▼
        ┌─────────────────────────────────────────────────────────┐
        │                  Infrastructure                       │
        │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
        │  │PostgreSQL   │  │   Redis     │  │  RabbitMQ   │    │
        │  │Master/Replica│ │  (Port 6379) │  │ (Port 5672) │    │
        │  │ (Port 5432) │  │             │  │             │    │
        │  └─────────────┘  └─────────────┘  └─────────────┘    │
        │  ┌─────────────┐                                      │
        │  │Elasticsearch│                                      │
        │  │ (Port 9200) │                                      │
        │  └─────────────┘                                      │
        └─────────────────────────────────────────────────────────┘
                                │
                                ▼
        ┌─────────────────────────────────────────────────────────┐
        │                  Monitoring                            │
        │  ┌─────────────┐  ┌─────────────┐                    │
        │  │Prometheus   │  │   Grafana   │                    │
        │  │ (Port 9090) │  │ (Port 3000) │                    │
        │  └─────────────┘  └─────────────┘                    │
        └─────────────────────────────────────────────────────────┘
```

##  Microservices

### User Service (`user-service`)
- **Port**: 8081
- **Purpose**: User authentication, registration, and profile management
- **Features**: JWT-based authentication, user CRUD operations
- **Database**: PostgreSQL (movento_user_db)

### Streaming Service (`streaming-service`)
- **Port**: 8083
- **Purpose**: Content streaming and media management
- **Features**: Video/audio streaming, content metadata
- **Database**: PostgreSQL (movento_streaming_db)
- **Message Queue**: RabbitMQ for async processing

### Payment Service (`payment-service`)
- **Port**: 8082
- **Purpose**: Payment processing and subscription management
- **Features**: Payment gateway integration, subscription handling
- **Database**: PostgreSQL (movento_payment_db)

### Recommendation Service (`recommendation-service`)
- **Port**: 8084
- **Purpose**: Content recommendations and user analytics
- **Features**: ML-based recommendations, user behavior tracking
- **Database**: PostgreSQL (movento_recommendation_db)
- **Search**: Elasticsearch for content indexing

### API Gateway (`api-gateway`)
- **Port**: 8080
- **Purpose**: Single entry point, routing, and cross-cutting concerns
- **Features**: Request routing, rate limiting, authentication
- **Load Balancer**: Nginx for production deployment

### Service Registry (`service-registry`)
- **Port**: 8761
- **Purpose**: Service discovery and registration
- **Technology**: Eureka Server

##  Technology Stack

### Backend Framework
- **Spring Boot 3.x** - Main application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access layer
- **Spring Cloud** - Microservices infrastructure
- **Eureka** - Service discovery

### Database & Persistence
- **PostgreSQL 14** - Primary database with master-replica replication
- **Flyway** - Database migration management
- **Redis 7** - Caching and session storage
- **Elasticsearch 8.5** - Search and analytics

### Message Queue
- **RabbitMQ 3.11** - Asynchronous messaging and event streaming

### Containerization & Orchestration
- **Docker** - Containerization
- **Docker Compose** - Local development and multi-container deployment
- **Nginx** - Load balancing and reverse proxy

### Monitoring & Observability
- **Prometheus** - Metrics collection
- **Grafana** - Metrics visualization and dashboards
- **Spring Boot Actuator** - Application health and metrics

### Development Tools
- **Maven** - Build automation and dependency management
- **Java 17** - Runtime platform
- **Lombok** - Code generation and boilerplate reduction

### Security
- **JWT (JSON Web Tokens)** - Stateless authentication
- **BCrypt** - Password hashing
- **HTTPS/TLS** - Secure communication

##  Deployment

### Development Environment
```bash
# Start all services for development
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Production Environment
```bash
# Start full production stack with monitoring
docker-compose -f docker-compose-full.yml up -d

# Scale API services
docker-compose -f docker-compose-full.yml up -d --scale api=3
```

### Environment Variables
Create a `.env` file for configuration:
```env
REDIS_PASSWORD=your_redis_password
RABBITMQ_USER=your_rabbitmq_user
RABBITMQ_PASSWORD=your_rabbitmq_password
JWT_SECRET=your_jwt_secret_key
STRIPE_SECRET_KEY=your_stripe_secret_key
STRIPE_PUBLIC_KEY=your_stripe_public_key
STRIPE_WEBHOOK_SECRET=your_stripe_webhook_secret
```

##  Configuration Profiles

- **`dev`** - Development configuration with local databases
- **`docker`** - Docker containerized development
- **`prod`** - Production configuration with monitoring and scaling

##  Database Architecture

### Master-Replica Setup
- **Master**: Handles all write operations
- **Replica**: Handles read operations and provides failover capability
- **Replication**: Streaming replication using WAL (Write-Ahead Log)

### Database Schema
Each microservice has its own database schema to ensure loose coupling:
- `movento_user_db` - User management
- `movento_streaming_db` - Content and media
- `movento_payment_db` - Payments and subscriptions
- `movento_recommendation_db` - Recommendations and analytics

##  API Documentation

### Gateway Endpoints
- **Authentication**: `/api/auth/*` - Login, registration, token refresh
- **Users**: `/api/users/*` - User profile management
- **Streaming**: `/api/streaming/*` - Content streaming
- **Payments**: `/api/payments/*` - Payment processing
- **Recommendations**: `/api/recommendations/*` - Content recommendations

### Service Registry
- **Eureka Dashboard**: `http://localhost:8761`
- **Service Health**: Actuator endpoints on each service

### Prerequisites
- Docker & Docker Compose
- Java 17 (for local development)
- Maven 3.8+

### Quick Start
1. Clone the repository
2. Copy `.env.example` to `.env` and configure
3. Run `docker-compose up -d`
4. Access services:
   - API Gateway: http://localhost:8080
   - Eureka: http://localhost:8761
   - Grafana: http://localhost:3000 (admin/admin)

### Development Mode
```bash
# Build all services
mvn clean install

# Run individual service
cd user-service
mvn spring-boot:run
```

##  Security Features

- **JWT Authentication**: Stateless token-based authentication
- **Password Security**: BCrypt hashing for password storage
- **API Security**: Rate limiting and request validation
- **Database Security**: Encrypted connections and least privilege access
- **Container Security**: Non-root users and minimal base images

##  Monitoring & Observability

### Metrics Collection
- **Prometheus**: Collects application and infrastructure metrics
- **Grafana**: Visualizes metrics with custom dashboards
- **Spring Boot Actuator**: Exposes health, metrics, and info endpoints

### Logging
- **Structured Logging**: JSON format for log aggregation
- **Log Levels**: Configurable per environment
- **Centralized Logging**: Can be integrated with ELK stack

##  CI/CD Pipeline

### GitHub Actions
- **Build**: Automated Maven builds
- **Test**: Unit and integration tests
- **Security**: Dependency vulnerability scanning
- **Deploy**: Automated deployment to staging/production



