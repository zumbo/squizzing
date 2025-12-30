# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Squizzing is an online quiz application for SQV (Swiss Quizzing Association). It's a Spring Boot 4 application written in Kotlin, using Thymeleaf for server-side rendering and PostgreSQL as the database.

## Build Commands

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=SquizzingApplicationTests

# Run a single test method
./mvnw test -Dtest=SquizzingApplicationTests#contextLoads
```

## Technology Stack

- **Language**: Kotlin 2.2 with Java 21
- **Framework**: Spring Boot 4.0
- **Template Engine**: Thymeleaf + Bootstrap 5
- **Database**: PostgreSQL + Flyway migrations
- **Security**: Spring Security with magic link (passwordless) authentication
- **Build Tool**: Maven

## Project Structure

```
src/main/kotlin/ch/quizzing/squizzing/
├── config/          - AppProperties, SecurityConfig
├── controller/      - MVC controllers (Auth, Admin, Quiz, Scoreboard, Image)
├── domain/          - JPA entities (User, Round, Question, AnswerOption, PlayerRound, PlayerAnswer, MagicToken)
├── repository/      - Spring Data JPA repositories
├── service/         - Business logic (Auth, Quiz, Scoreboard, User, Round, QuestionImport, ImageStorage, Email)

src/main/resources/
├── templates/
│   ├── layout/      - Thymeleaf layout (main.html)
│   ├── auth/        - Login, check-email
│   ├── admin/       - Dashboard, rounds, questions, users
│   └── quiz/        - Home, question, answer-result, result, history
├── static/css/      - Custom styles
├── db/migration/    - Flyway SQL migrations
└── application.yaml - Configuration
```

## Architecture

- **Authentication**: Passwordless via email magic links. Emails logged to console in dev mode.
- **Quiz Flow**: Server-validated timing (10s timer). Score: 100 pts at 0s, 50 pts at 10s, linear interpolation.
- **Image Storage**: Local filesystem (configurable via `squizzing.upload-dir`)
- **Question Import**: Excel (.xlsx) or CSV files via Apache POI

## Kotlin Configuration

The project uses strict JSR-305 null-safety annotations (`-Xjsr305=strict`) and the Spring Kotlin compiler plugins for open classes and JPA entities.

## Database Setup

Requires PostgreSQL. Default connection: `jdbc:postgresql://localhost:5432/squizzing`

Environment variables:
- `DB_USERNAME` / `DB_PASSWORD` - Database credentials
- `UPLOAD_DIR` - Image upload directory (default: ./uploads)
- `BASE_URL` - Application base URL for magic links
