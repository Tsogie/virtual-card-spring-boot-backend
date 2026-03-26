# Backend: NFC Mobile Transit Payment
A full-stack NFC-based payment system built with Spring Boot and Android.

## Live Demo
- Backend API: https://virtual-card-spring-boot-backend.onrender.com
- Android APK: https://github.com/Tsogie/virtual-card-react-native-cli-frontend/releases/latest/download/app-release.apk

## Tech Stack
- Backend: Java, Spring Boot, PostgreSQL
- Security: JWT Authentication
- CI/CD: GitHub Actions
- Deployment: Render

## Run Locally (Docker)
Prerequisites: Docker Desktop

1. Clone the repo
2. Create a `.env` file:
   DB_PASSWORD=your_password
3. Run:
   docker-compose up
4. App available at http://localhost:3000

## API Documentation
- Health check: GET /health
