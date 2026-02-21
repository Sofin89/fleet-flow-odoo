# FleetFlow - Modular Fleet & Logistics Management System

FleetFlow is a comprehensive system designed to manage fleets, track vehicles in real-time, handle maintenance requests, and manage drivers and logistics operations efficiently.

## 🚀 Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.2**
  - Spring Web
  - Spring Data JPA
  - Spring Security (JWT Authentication)
  - Spring WebSocket
- **PostgreSQL** (Database)
- **Flyway** (Database Migrations)
- **Lombok** (Code reduction)
- **OpenCSV** (CSV processing)

### Frontend
- **React 18**
- **Vite** (Build Tool)
- **React Router** (Navigation)
- **React Leaflet** (Map Visualization)
- **Recharts** (Data Visualization)
- **SockJS & STOMP** (Real-time WebSocket communication)

---

## 📋 Prerequisites

Before you begin, ensure you have the following installed on your local machine:
- **[Java Development Kit (JDK) 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)**
- **[Node.js](https://nodejs.org/)** (v16 or higher) & **npm**
- **[PostgreSQL](https://www.postgresql.org/)**
- **[Maven](https://maven.apache.org/)** (Optional, backend includes a Maven wrapper `mvnw`)

---

## 🛠️ Setup Instructions

### 1. Clone the repository
```bash
git clone <repository-url>
cd fleet-flow-odoo
```

### 2. Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Configure the database:
   Update `src/main/resources/application.properties` or `application.yml` with your PostgreSQL credentials. Ensure you have created a database in PostgreSQL for this project.

3. Build and run the backend application:
   ```bash
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```
   *The backend server will typically start on `http://localhost:8080`.*

### 3. Frontend Setup

1. Open a new terminal and navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install the necessary dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
   *The frontend application will typically be accessible at `http://localhost:5173`.*

---

## 🌟 Key Features
- **Real-Time Tracking:** Visualize vehicle locations and routes on an interactive map.
- **Driver Management:** Keep track of driver records, assignments, and statuses.
- **Maintenance Logs:** Record and monitor vehicle maintenance requests and history.
- **Trip Management:** Plan, dispatch, and track ongoing trips.
- **Secure Access:** Role-based access control leveraging JWT authentication.

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).