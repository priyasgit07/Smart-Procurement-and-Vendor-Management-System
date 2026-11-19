# Smart Procurement & Vendor Management System

A comprehensive enterprise procurement and vendor management system built with **Spring Boot 3.2.5**, **Spring Security 6**, **Thymeleaf**, and **MySQL**.

---

## 🌟 Key Features

* **Vendor Management**: Register and maintain supplier directories with contact details, address, ratings, and status (`ACTIVE`, `INACTIVE`, `PENDING_APPROVAL`). Export supplier reports to PDF and Excel via JasperReports.
* **Requisition Workflow**: Create procurement requests with detailed item specifications, quantities, and unit pricing. Role-based data isolation ensures employees see only their requests, while managers and administrators oversee all requisitions.
* **Purchase Orders**: Generate POs linked directly to verified `ACTIVE` suppliers and `APPROVED` requisitions. Auto-creates approval tasks for management review.
* **Approval Center**: Unified review hub for managers and administrators to inspect requisitions and purchase orders, supply feedback/rejection comments, and record decision audit trails.
* **Real-time Metrics Dashboard**: Centralized dashboard displaying live statistics for vendors, requisitions, purchase orders, and pending approval tasks.
* **Role-Based Access Control (RBAC)**:
  * **ADMIN**: Full system access, vendor management, requisition review, purchase order lifecycle, and approval center.
  * **PROCUREMENT_MANAGER**: Supplier management, purchase order generation, and approval decisions.
  * **EMPLOYEE**: Requisition creation and tracking of own requests.
* **Audit Trail**: Detailed logging of security, procurement, status changes, and administrative operations.

---

## 🛠️ Tech Stack

* **Backend**: Java 17+, Spring Boot 3.2.5, Spring Data JPA, Spring Security, JJWT
* **Frontend**: Thymeleaf, Responsive Modern CSS
* **Database**: MySQL 8.x (H2 for integration testing)
* **Reporting**: JasperReports (PDF & Excel generation)
* **Build Tool**: Apache Maven

---

## 🚀 Getting Started

### 1. Prerequisites
* Java Development Kit (JDK 17 or higher)
* MySQL Server (running on `localhost:3306`)
* Apache Maven

### 2. Environment Configuration
Copy `.env.example` to create your environment configuration or export environment variables:
```bash
# Database Configuration
export SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/procurement_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="your_database_password"

# Server Port
export SERVER_PORT=8083

# JWT Configuration (minimum 32 characters for HS256)
export JWT_SECRET="your_jwt_secret_key_at_least_32_characters_long"
export JWT_EXPIRATION_MS=3600000

# Optional Demo Account Seeding (default is false)
export SEED_DEMO_USERS=true
export DEMO_ADMIN_PASSWORD="YourSecureAdminPassword!"
export DEMO_MANAGER_PASSWORD="YourSecureManagerPassword!"
export DEMO_EMPLOYEE_PASSWORD="YourSecureEmployeePassword!"
```

### 3. Database Setup
Create the database in MySQL:
```sql
CREATE DATABASE procurement_db;
```

### 4. Run the Application
Open a terminal in the project directory:
```bash
mvn clean spring-boot:run
```

### 5. Access the Application
* **Web UI**: [http://localhost:8083/login](http://localhost:8083/login)
* **Swagger API Docs**: [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) *(Requires ADMIN or PROCUREMENT_MANAGER credentials)*

---

## 🔒 Security & Demo Credentials

For production environments, set `SEED_DEMO_USERS=false`. When enabled for development/testing, demo accounts are created using the secure passwords defined in your environment variables (`DEMO_ADMIN_PASSWORD`, `DEMO_MANAGER_PASSWORD`, `DEMO_EMPLOYEE_PASSWORD`).
