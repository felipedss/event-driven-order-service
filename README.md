# Order Service

A Spring Boot microservice that handles order creation and lifecycle management in an event-driven platform. 
Works in tandem with the [Saga Orchestrator](../event-driven-saga-orchestrator) to coordinate distributed transactions across Payment, Inventory, and Notification services.

## Overview

The Order Service exposes a REST API to create and query orders. 
On creation, it persists the order to PostgreSQL and publishes an event to Kafka. 
The Saga Orchestrator picks up that event, runs the distributed transaction, and replies back on a 
separate topic — the Order Service then updates the order status accordingly.

```
POST /api/v1/orders
        │
        ▼
  Persist Order
  (status: CREATED)
        │
        ▼
  Publish OrderCreatedEvent
  ──────────────────────────► Kafka: order.created
                                        │
                              Saga Orchestrator
                                        │
              ┌─────────────────────────┴─────────────────────────┐
              │                                                   │
  Kafka: order.confirmed                             Kafka: order.canceled
              │                                                   │
              ▼                                                   ▼
  Update Order → CONFIRMED                       Update Order → CANCELED
```

## Tech Stack

- **Java 17**, Spring Boot 4.0.3
- **PostgreSQL** — order persistence (Hibernate, `ddl-auto: update`)
- **Apache Kafka** — event publishing and consumption
- **Lombok** — boilerplate reduction
- **Spotless** — code formatting (Google Java Format)

## Prerequisites

- Docker (for Kafka and PostgreSQL)
- Java 17+
- Maven

## Infrastructure Setup

Start Kafka and PostgreSQL via the shared Docker Compose configuration:

```bash
cd ~/Workspace/event-driven-simulator/infrastructure/docker-compose
docker compose up -d
```

This starts:
- **PostgreSQL** on `localhost:5432` — database `order_db`, credentials `postgres/postgres`
- **Kafka** on `localhost:9094` (external) — KRaft mode, no Zookeeper
- **Kafka UI** at `http://localhost:8090`

## Running the Service

```bash
mvn spring-boot:run
```

The service starts on **port 8081**.

## API Reference

### Create Order

```
POST /api/v1/orders
Content-Type: application/json

{ "quantity": 3 }
```

**Response `200 OK`:**
```json
{
  "orderId": "773d07c8-c295-40db-a6e9-5a686026db2d",
  "quantity": 3,
  "status": "CREATED"
}
```

### Get Order

```
GET /api/v1/orders/{orderId}
```

**Response `200 OK`:**
```json
{
  "orderId": "773d07c8-c295-40db-a6e9-5a686026db2d",
  "quantity": 3,
  "status": "CONFIRMED"
}
```

Returns `404 Not Found` if the order does not exist.

## Order Lifecycle

| Status | Description |
|---|---|
| `CREATED` | Order persisted; saga started |
| `CONFIRMED` | All saga steps succeeded |
| `CANCELED` | Saga failed; order rolled back |
| `COMPLETED` | Order fulfilled (future use) |

## Kafka Topics

| Topic | Direction | Description |
|---|---|---|
| `order.created` | Outbound | Published after order is persisted |
| `order.confirmed` | Inbound | Consumed when saga succeeds |
| `order.canceled` | Inbound | Consumed when saga fails |

## Build Commands

```bash
mvn clean install       # Build and run all tests
mvn test                # Run tests only
mvn spotless:apply      # Format code (run before committing)
mvn spotless:check      # Verify formatting
```

## Package Structure

```
com.platform.orderservice
├── command/        # CreateOrderCommand — REST input DTO
├── config/         # KafkaProducerConfig, KafkaConsumerConfig
├── controller/     # OrderController — REST endpoints
├── event/
│   ├── inbound/    # OrderConfirmedEvent, OrderCanceledEvent
│   └── outbound/   # OrderCreatedEvent
├── exception/      # OrderNotFoundException, GlobalExceptionHandler
├── messaging/
│   ├── consumer/   # OrderEventConsumer — Kafka listeners
│   └── producer/   # KafkaProducerService — async event publishing
├── model/          # Order entity, OrderStatus enum
├── repository/     # OrderRepository (Spring Data JPA)
└── service/        # OrderService — core business logic
```

## Related Services

| Service | Port | Role |
|---|---|---|
| **event-driven-saga-orchestrator** | 8085 | Coordinates the distributed transaction |
| Payment Service | — | Part of the saga (managed by orchestrator) |
| Inventory Service | — | Part of the saga (managed by orchestrator) |
| Notification Service | — | Part of the saga (managed by orchestrator) |
