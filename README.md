<div align="center">

# 🚚 FleetFlow
**Next-Generation Modular Fleet & Logistics Management System**

*Built for the [Odoo Hackathon] - Empowering transport operations with intelligence and efficiency.*

</div>

---

## 🌟 The Team
We built this platform during the Odoo Hackathon to solve modern logistics tracking and management issues. 
Meet the brilliant minds behind FleetFlow:
* **Sofin Mansuri**
* **Ismail Mansuri**
* **Sulem Pomchawala**
* **Meet Sharma**

---

## 🎯 About FleetFlow
FleetFlow is a comprehensive system designed to manage fleets, track vehicles in real-time, handle maintenance forecasts, and manage drivers and logistics operations efficiently using predictive analytics and real-time mapping technology.

## 🚀 Key Features

### 📍 Real-Time GPS Tracking & Routing
- Visual live map representation of all active vehicles.
- Polyline route drawing showing completed and remaining trip segments.
- Turn-by-turn ETA calculations using the OSRM road-following network.
- Integration to immediately open dispatched trips in Google Maps.

### 🧠 AI Predictive Analytics 
- **Maintenance Forecast (30 Days):** Deterministically predicts upcoming vehicle maintenance, allowing proactive servicing before breakdowns occur.
- **Demand Forecasting:** Analyzes the last 14 days of trip data to generate a rolling forecast for upcoming weekly demand.
- **Revenue Projections:** Calculates a moving average to predict 30-day projected revenue streams.
- **Driver Risk Profiling:** Consolidates safety scores, trip completion rates, and license expiry timelines into a unified driver risk probability model.

### 📧 Automated Dispatch Communications
- Zero-friction background emailing utilizing Spring `@Async`.
- Emails trip manifests directly to assigned drivers automatically upon trip creation.

### � Comprehensive Personnel Management
- Intuitive "Add Driver" forms, tracking licenses to specific vehicle categories (Truck, Van, Bike).
- Leaderboard dashboard for dispatchers tracking on-time percentages, efficiency metrics, and highest-performing drivers.

---

## 💻 Tech Stack

### Backend Infrastructure
- **Java 17 & Spring Boot 3.2**
- **Spring Security (JWT):** Securing endpoints via role-based access tokens.
- **Spring WebSocket (STOMP):** Architecture ready for real-time driver coordinate streaming.
- **PostgreSQL & Flyway:** Robust relational databases with version-controlled schema migrations.
- **JavaMailSender:** Programmatic email dispatches for logistics notifications.
- **Lombok:** Boilerplate code reduction.

### Frontend Client
- **React 18 & Vite:** Lightning-fast compilation and highly responsive component rendering.
- **React Router:** Seamless Single Page Application (SPA) navigation.
- **React Leaflet:** Advanced open-source mapping.
- **Recharts:** Clean, responsive data visualization.

---

## 🛠️ Setup Instructions

### 1. Database Setup
Ensure you have a PostgreSQL server running. FleetFlow connects to a cloud NeonDB instance by default, but you can configure a local database in `backend/src/main/resources/application.properties`.

### 2. Backend Setup
```bash
git clone <repository-url>
cd fleet-flow-odoo/backend

# Build the project using the packaged Maven wrapper
./mvnw clean install

# Run the API server
./mvnw spring-boot:run
```
*The Spring Boot server will initialize and listen on `http://localhost:8080`.*

### 3. Frontend Setup
```bash
cd fleet-flow-odoo/frontend

# Install node dependencies
npm install

# Boot the Vite development server
npm run dev
```
*The React application will be available at `http://localhost:5173`.*

---

## 📄 License
This codebase is an open submission for the Odoo Hackathon. Feel free to explore the repository!