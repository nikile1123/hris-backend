# HRIS Back-End Application

## Overview

This project is a back-end implementation of a Human Resource Information System (HRIS). It is built using Kotlin, Ktor, Kodein for dependency injection, and Exposed for database interactions. The system is designed to be scalable, asynchronous, and optimized for performance. The application is organized into several modules:
- **Employees:** Manages employee data, including personal information, organizational hierarchy (manager, subordinates, colleagues), and team membership.
- **Performance Reviews:** Handles the performance review history of employees.
- **Notification:** Processes notifications using RabbitMQ for performance reviews (targeted to individual employees) and team changes (targeted to the entire team).
- **Teams:** Manages team information.

## Key Features

- **Employee Information:**
    - CRUD operations for employee data.
    - Hierarchy endpoints for retrieving an employee's manager, subordinates, and colleagues.
    - Dedicated endpoints to navigate up/down the organizational structure.

- **Performance Reviews:**
    - Record weekly performance reviews with multiple evaluation criteria.

- **Notification System:**
    - Implements the outbox pattern to decouple business operations from notification processing.
    - Uses RabbitMQ with topic exchanges and routing keys to route messages appropriately.
    - Supports different notification types (e.g., email and browser UI).

- **OpenAPI Documentation:**
    - Automatic generation of OpenAPI documentation for your API endpoints.

- **Metrics & Monitoring:**
    - Integrates Micrometer with Prometheus to expose HTTP and JVM metrics.
    - Grafana dashboards provide real-time monitoring of key performance indicators.

- **Containerized Deployment:**
    - The entire system is containerized using Docker and orchestrated via docker-compose.
    - Services include PostgreSQL (with Flyway migrations), RabbitMQ, and monitoring tools.

## Architectural Decisions

- **Modular Monolith:**  
  The application is organized into clearly defined modules (employees, reviews, notifications, teams) to ensure separation of concerns and ease of future refactoring or migration to microservices.

- **Asynchronous Processing:**  
  Kotlin coroutines and non-blocking I/O via Ktor enable high concurrency and efficient resource usage.

- **Robust Data Management:**  
  Exposed ORM is used for type-safe database interactions, and Flyway is used to manage schema migrations.

- **Dependency Injection:**  
  Kodein DI decouples components, facilitating testing and future modifications.

- **Event-Driven Notifications:**  
  The outbox pattern is implemented to log events that are later routed through RabbitMQ based on topic routing keys.

## Database Schema

The database consists of the following key tables:

- **teams:**
    - **id:** UUID, primary key
    - **name:** Unique team name
    - **created_at:** Timestamp

- **employees:**
    - **id:** UUID, primary key
    - **first_name, last_name, email, position:** Employee details
    - **supervisor_id:** Self-referencing foreign key
    - **subordinates_count:** Number of direct subordinates
    - **team_id:** Foreign key referencing `teams`

- **performance_reviews:**
    - **id:** UUID, primary key
    - **employee_id:** Foreign key referencing `employees`
    - **team_id:** Foreign key referencing `teams`
    - **review_date, performance, soft_skills, independence, aspiration_for_growth:** Review details

- **outbox:**
    - **id:** UUID, primary key
    - **employee_id:** ID of the employee related to the event
    - **team_id:** ID of the team related to the event
    - **event_type:** Type of event (e.g., `employee.created`, `review.created`)
    - **message:** A short textual description of the event
    - **processed:** Flag indicating whether the event was processed
    - **created_at:** Timestamp

*Include diagrams here if available (e.g., ER diagrams, system architecture diagrams).*

## Deployment

The project uses Docker and docker-compose for containerized deployment. The docker-compose file defines services for:

- **PostgreSQL** (with Flyway migrations)
- **RabbitMQ** (with Management UI)
- **Employee Service, Review Service, and Notification Service**
- **Prometheus and Grafana** for monitoring

### Running Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/hris-backend.git
   cd hris-backend
