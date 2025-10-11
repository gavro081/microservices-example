# Microservices Example

A microservices architecture demonstration built with Spring Boot, featuring service discovery, API gateway, and event-driven communication.

## Architecture Overview

This project demonstrates a complete microservices ecosystem with the following components:

### Core Services

- **User Service** - Manages user accounts and balances
- **Product Service** - Handles product inventory and catalog
- **Order Service** - Processes orders and transactions
- **Eureka Server** - Service discovery and registration
- **API Gateway** - Single entry point with load balancing and routing

### Infrastructure

- **PostgreSQL Databases** - Separate database per service
- **RabbitMQ** - Message broker for asynchronous communication
- **Docker & Docker Compose** - Containerized deployment

### Frontend

- **HTML/JavaScript** - Simple web interface with real-time WebSocket updates

## Architecture Diagrams

The `diagrams/` folder contains the following diagrams:

- `microservices-architecture.excalidraw.png` - Overall system architecture
- `order-placement-saga.excalidraw.png` - Order placement workflow and saga pattern

## Key Features

- **Service Discovery** - Services automatically register and discover each other via Eureka
- **API Gateway** - All external requests route through a single entry point
- **Database per Service** - Each microservice has its own PostgreSQL database
- **Event-Driven Architecture** - Services communicate via RabbitMQ messages
- **Real-time Updates** - WebSocket integration for live order status updates
- **Data Seeding** - Automatic database population with sample data
- **Health Checks** - Built-in health monitoring for all services

## How to Run the Project

### Prerequisites

- Docker and Docker Compose installed
- Ports 8080-8083, 8761, 5672, 5433-5435, 15672 available

### Quick Start

1. **Clone the repository**

   ```bash
   git clone https://github.com/gavro081/microservices-example.git
   cd microservices-example
   ```

2. **Start all services**

   ```bash
   cd microservices-example
   docker-compose up --build
   ```

3. **Wait for services to start**

   - Watch the logs for "Started [ServiceName]Application" messages
   - All databases will be automatically seeded with sample data

> note: First startup takes longer as Docker builds all images and downloads dependencies

4. **Access the application**
   - **Frontend**: Open `frontend/index.html` in your browser
   - **API Gateway**: http://localhost:8080
   - **Eureka Dashboard**: http://localhost:8761
   - **RabbitMQ Management**: http://localhost:15672 (guest/guest)

### Individual Service URLs

| Service         | URL                   | Database Port |
| --------------- | --------------------- | ------------- |
| API Gateway     | http://localhost:8080 | -             |
| User Service    | http://localhost:8082 | 5434          |
| Product Service | http://localhost:8081 | 5435          |
| Order Service   | http://localhost:8083 | 5433          |
| Eureka Server   | http://localhost:8761 | -             |

### API Endpoints (via Gateway)

All API calls should go through the gateway at port 8080:

```bash
# Get all products
GET http://localhost:8080/api/products

# Get all users
GET http://localhost:8080/api/users

# Get latest order
GET http://localhost:8080/api/orders/last

# Place new order
POST http://localhost:8080/api/orders
{
  "username": "gavro",
  "productName": "MacBook Pro M4Pro",
  "quantity": 1
}
```

### Stopping the Application

```bash
# Stop all containers
docker-compose down

# Stop and remove volumes (deletes all data)
docker-compose down -v
```

### Rebuild After Changes

```bash
# Rebuild specific service
docker-compose up -d --build user-service

# Rebuild all services
docker-compose up --build
```
