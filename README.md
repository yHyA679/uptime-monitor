# Uptime Monitor

A full-stack website monitoring platform that tracks uptime, response times, incidents, and service health.

## Live Demo

Frontend:
https://uptime-monitor-alpha-bay.vercel.app

Backend Health:
https://uptime-monitor-2lby.onrender.com/actuator/health

## Tech Stack

### Frontend
- React
- TypeScript
- Vite

### Backend
- Java 17
- Spring Boot
- Spring Data JPA
- Flyway
- PostgreSQL

### DevOps
- Docker
- Docker Compose
- GitHub Actions
- Render
- Vercel

## Features

- Website uptime monitoring
- Automatic scheduled checks
- Manual health checks
- Monitoring history
- Uptime statistics
- Incident tracking
- Alerts
- Public status page
- Dashboard metrics
- Configurable monitoring intervals
- Pagination and filtering
- Validation and global error handling

## Architecture

Frontend (React + Vite)
        |
        v
Backend (Spring Boot REST API)
        |
        v
PostgreSQL

Deployment:
- Frontend: Vercel
- Backend: Render
- Database: Render PostgreSQL
- CI: GitHub Actions

