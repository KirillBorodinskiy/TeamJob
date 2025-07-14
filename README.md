# TeamJob: Team Calendar & Resource Management

## Overview
TeamJob is a self-hosted, open-source web application for team scheduling, resource management, and event planning. 
Designed for organizations such as universities, clinics, and businesses, it provides a flexible, privacy-focused alternative to proprietary cloud-based calendars.

## Setup Guide

### 1. Install docker
### 2. Clone the Repository
### 3.Adjust values as needed for your environment.

### 4. Running with Docker Compose
This will start both the PostgreSQL database and the application.
```sh
docker-compose up --build
```
- The app will be available at [http://localhost:8080](http://localhost:8080)
- The database will be accessible on port 5433 (default: 5432 inside container)

### 6. Usage
- Access the web UI at [http://localhost:8080](http://localhost:8080)
- Default admin user: `adminadmin` / `adminadmin`
- Default regular user: `useruser` / `useruser`
- Explore calendar, room, and user management features

### Testing
- Done on docker container startup to ensure the application is functioning correctly.
- Run tests separately: `./gradlew test`

## Features
- **User Authentication** (JWT-based, secure password hashing)
- **Role-based Access Control** (Admin, Config, User)
- **Weekly & Daily Calendar Views**
- **Room and Person Availability**
- **Event Creation, Editing, and Conflict Detection**
- **Room Management**
- **Tagging for Users, Rooms, and Events**
- **REST API and Web UI (Thymeleaf + JS)**


## Technology Stack
- **Backend:** Java 21, Spring Boot, Spring Security, Spring Data JPA
- **Frontend:** Thymeleaf, JavaScript, CSS
- **Database:** PostgreSQL
- **Containerization:** Docker, Docker Compose
- **Build Tool:** Gradle
- **Testing:** JUnit 5, Spring Test

## Architecture
- **MVC Pattern:**
  - Controllers (REST & View)
  - Services (Business Logic)
  - Repositories (Data Access)
  - Entities (Users, Rooms, Events, Roles)
- **Security:** JWT authentication, BCrypt password hashing, role-based authorization
- **Deployment:** Multi-stage Docker build, health checks, environment-based configuration

### License
This project is licensed under the MIT License. (I guess?)

