![Banner](app/src/main/res/drawable/banner.png)

# Vibra


# Android Application Deployment Guide

## Prerequisites
- **IDE**: IntelliJ IDEA or Android Studio installed
- **Docker**: Installed and running on your system

## Setup Instructions

1. **Clone the repositories**:
   ```bash
   git clone <this-repository-url>
   git clone <backend-repository-url>
   ```

2. **Set up the backend**:
   ```bash
   cd backend/docker
   ```
   Then start the containers using either:

   **Option 1: Docker Compose**
   ```bash
   # Linux/macOS (use sudo if required)
   (sudo) docker compose up -d
   # Windows
   docker-compose up -d
   ```

   **Option 2: Using provided scripts (if available)**
   ```bash
   ./new_run_master.sh
   ```

3. **Run the Android application**:
   - Open the project in IntelliJ IDEA or Android Studio
   - Sync Gradle dependencies
   - Connect your Android device or start an emulator
   - Click the "Run" button (▶)

## Notes
- The `-d` flag runs containers in detached mode
- Ensure Docker has proper permissions if using sudo
- Check the repository for any additional setup scripts

## Troubleshooting
- If you encounter port conflicts, stop services using ports 8080/5432
- For Docker issues, try rebuilding containers:
  ```bash
  docker compose down && docker compose up --build
  ```
- For Android connection problems, enable USB debugging in Developer Options


## Aclarations

