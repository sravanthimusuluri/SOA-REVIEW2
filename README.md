# Enterprise Academic Resource & Circulation Management Platform
### Client: Bibliotech Circulation Systems
**Course**: 24SDCS03A - SOA Programming & Microservices | **Project Code**: PS021  
**Institution**: Koneru Lakshmaiah Education Foundation (KL University), Department of CSE

---

## 👥 Project Team & Contributors

| Member Name | Student ID | Email | Primary Responsibilities |
|:---|:---|:---|:---|
| **Musuluri Sravanthi** *(Lead)* | `2400030661` | 2400030661@kluniversity.in | Multi-Module Architecture, Eureka Service Discovery, Spring Cloud API Gateway, JWT Security Pipeline |
| **Malisetty Naga Sai Nikitha** | `2400033191` | 2400033191@kluniversity.in | Book Service Catalog Management, Inventory Counter Logic, OpenFeign Inter-Service Restocking |
| **Karri Venkata Lakshmi Khyati** | `2400033157` | 2400033157@kluniversity.in | Rental Service Circulation Rules, Duplicate Borrow Prevention, Fine Service Automated Penalty Math |

---

## 🏛️ System Architecture

```
                                  +---------------------------------------+
                                  |         Web Frontend Client           |
                                  |   (Modern Glassmorphic SPA / HTML5)   |
                                  +---------------------------------------+
                                                     |
                                                     | HTTP + Bearer JWT
                                                     v
                                  +---------------------------------------+
                                  |     Spring Cloud API Gateway          |
                                  |             (:8080)                   |
                                  |  - Reactive JWT Token Verification    |
                                  |  - Load-Balanced Microservice Routing |
                                  |  - Header Mutation (X-User-Role/Name) |
                                  +---------------------------------------+
                                          |           |           |
               +--------------------------+           |           +---------------------------+
               |                                      |                                       |
               v                                      v                                       v
    +--------------------+                 +--------------------+                  +--------------------+
    |    auth-service    |                 |    book-service    |                  |    fine-service    |
    |      (:8081)       |                 |      (:8082)       |                  |      (:8084)       |
    | - BCrypt Hashing   |                 | - Catalog CRUD     |                  | - Penalty Engine   |
    | - JWT Generation   |                 | - Stock Counters   |                  | - Rate Math        |
    | - Seeded Accounts  |                 | - Availability API |                  | - Payment Ledger   |
    +--------------------+                 +--------------------+                  +--------------------+
               ^                                      ^                                       ^
               |                                      | OpenFeign                             | OpenFeign
               |                                      | Restock / Decrement                   | Calculate Fine
               |                                      +-------------------+                   |
               |                                                          |                   |
               |                                               +--------------------+         |
               |                                               |   rental-service   |---------+
               |                                               |      (:8083)       |
               |                                               | - Duplicate Guard  |
               |                                               | - Issue / Return   |
               |                                               | - Feign Client     |
               |                                               +--------------------+
               |                                                          |
               +----------------------------------------------------------+
                                              |
                                              | Heartbeat & Registry
                                              v
                                  +---------------------------------------+
                                  |     Netflix Eureka Server             |
                                  |             (:8761)                   |
                                  |  - Dynamic Microservice Discovery     |
                                  |  - Centralized Service Topology       |
                                  +---------------------------------------+
```

---

## 🔑 Pre-Seeded Accounts (Zero-Setup Ready)

| Role | Username | Password | Full Name / Identity | Features Accessible |
|:---|:---|:---|:---|:---|
| **STUDENT (Lead)** | `2400030661` | `pass123` | Musuluri Sravanthi | Catalog, Borrow Book, My Loans, Return Book, Pay Fines |
| **STUDENT** | `2400033191` | `pass123` | Malisetty Naga Sai Nikitha | Catalog, Borrow Book, My Loans, Return Book, Pay Fines |
| **STUDENT** | `2400033157` | `pass123` | Karri Venkata Lakshmi Khyati | Catalog, Borrow Book, My Loans, Return Book, Pay Fines |
| **LIBRARIAN** | `librarian` | `lib123` | Chief Librarian | Add Books, Master Circulation Records, Overdue Books Audit |
| **ADMIN** | `admin` | `admin123` | Platform Administrator | Add Books, Delete Books, Master Fines Ledger, Full Privileges |

> **Note**: The frontend dashboard also includes a **"Quick Demo Login"** dropdown menu to switch between any of these accounts in 1 click!

---

## 🚀 How to Run the Platform

### Prerequisites
- **Java 21** (JDK 21 LTS)
- **Maven 3.8+**
- A modern web browser (Chrome, Edge, Firefox)

---

### Option 1: One-Click Launch via PowerShell (Recommended for Windows)

From the project root directory (`c:\Users\srava\Desktop\snk`):

```powershell
.\run-all.ps1
```

This script will automatically:
1. Verify and package the Maven modules.
2. Launch `eureka-server` (:8761) and wait for initialization.
3. Launch `auth-service` (:8081), `book-service` (:8082), and `fine-service` (:8084).
4. Launch `rental-service` (:8083) with OpenFeign bindings.
5. Launch `api-gateway` (:8080).
6. Automatically open `frontend/index.html` in your default browser.

To stop all services cleanly at any time:
```powershell
.\stop-all.ps1
```

---

### Option 2: Running in VS Code

1. Open the `snk` folder in VS Code (`File` -> `Open Folder...`).
2. Install the **Extension Pack for Java** and **Spring Boot Extension Pack**.
3. In the **Spring Boot Dashboard** panel on the left sidebar, start the services in the following order:
   1. `eureka-server`
   2. `auth-service`
   3. `book-service`
   4. `fine-service`
   5. `rental-service`
   6. `api-gateway`
4. Right-click `frontend/index.html` and select **Open with Live Server** (or open it directly in your browser).

---

### Option 3: Running in Spring Tool Suite (STS) / Eclipse

1. Select `File` -> `Import...` -> `Existing Maven Projects`.
2. Browse to `c:\Users\srava\Desktop\snk` and import all modules.
3. In the **Boot Dashboard**:
   - Start `eureka-server` (wait for port `8761`).
   - Start `auth-service`, `book-service`, `fine-service`.
   - Start `rental-service`.
   - Start `api-gateway`.
4. Open `frontend/index.html` in a web browser.

---

### Option 4: Running via Maven CLI in Separate Terminals

If starting manually via command line, execute each command in its respective module directory:

```bash
# Terminal 1: Eureka Server (Port 8761)
cd eureka-server
mvn spring-boot:run

# Terminal 2: Auth Service (Port 8081)
cd auth-service
mvn spring-boot:run

# Terminal 3: Book Service (Port 8082)
cd book-service
mvn spring-boot:run

# Terminal 4: Fine Service (Port 8084)
cd fine-service
mvn spring-boot:run

# Terminal 5: Rental Service (Port 8083)
cd rental-service
mvn spring-boot:run

# Terminal 6: API Gateway (Port 8080)
cd api-gateway
mvn spring-boot:run
```

---

### Option 5: Containerized Deployment via Docker Compose

```bash
mvn clean package -DskipTests
docker-compose up -d --build
```

Docker Compose starts MySQL on port `3306`, creates the four service databases, and
uses the `bibliotech` user with password `bibliotech`. For local Maven runs, create
the same databases and credentials before starting the services. Hibernate creates
the tables and the auth/book seed runners create initial users and books.

---

## 🧪 Automated Unit & Integration Tests

All core business requirements are thoroughly tested with **JUnit 5** and **Mockito**:

- **Duplicate Borrow Guard**: Verifies that active loans trigger a `DuplicateBorrowException` (HTTP 409).
- **Inventory Synchronization**: Verifies OpenFeign decrementing when borrowing and incrementing when returning.
- **Automated Fine Math**: Verifies that late returns compute overdue days $\times$ daily rate (₹10/day) and trigger `FineClient`.
- **Payment Lifecycle**: Verifies transition from `UNPAID` to `PAID`.

To run all test suites across the multi-module project:

```powershell
mvn clean test
```

---

## 📡 API Gateway Endpoints Reference

All requests route through `http://localhost:8080`:

| Service | HTTP Method | Route / Path | Role Required | Description |
|:---|:---|:---|:---|:---|
| **Auth** | `POST` | `/api/auth/login` | *Public* | Authenticates credentials and issues Bearer JWT |
| **Auth** | `POST` | `/api/auth/register` | *Public* | Registers new student or librarian account |
| **Auth** | `GET` | `/api/auth/validate` | *Public* | Validates active JWT token |
| **Auth** | `POST` | `/api/auth/refresh` | Authenticated | Issues a new JWT from a valid Bearer token |
| **Books** | `GET` | `/api/books` | *Public* | List books with search, category, and branch filters |
| **Books** | `GET` | `/api/books/{id}` | *Public* | Retrieve book details |
| **Books** | `GET` | `/api/books/{id}/availability` | *Public* | Real-time copy availability check |
| **Books** | `POST` | `/api/books` | `LIBRARIAN`, `ADMIN` | Add new book to institutional inventory |
| **Books** | `PUT` | `/api/books/{id}` | `LIBRARIAN`, `ADMIN` | Update book title or copy count |
| **Books** | `DELETE` | `/api/books/{id}` | `LIBRARIAN`, `ADMIN` | Remove book title |
| **Rentals** | `POST` | `/api/rentals/borrow` | `STUDENT` | Borrow a book (enforces duplicate prevention & stock) |
| **Rentals** | `POST` | `/api/rentals/{id}/return` | Authenticated | Return book (restocks book and triggers fine if overdue) |
| **Rentals** | `GET` | `/api/rentals/my-rentals` | `STUDENT` | List active and returned loans for current student |
| **Rentals** | `GET` | `/api/rentals` | `LIBRARIAN`, `ADMIN` | View all circulation records campus-wide |
| **Rentals** | `GET` | `/api/rentals/overdue` | `LIBRARIAN`, `ADMIN` | Audit report of all overdue loans |
| **Rentals** | `POST` | `/api/rentals/{id}/simulate-overdue` | Authenticated | Demo tool: backdates loan due date to test fine math |
| **Fines** | `GET` | `/api/fines/my-fines` | `STUDENT` | List student penalties and outstanding balance |
| **Fines** | `PUT` | `/api/fines/{id}/pay` | `STUDENT` | Settle fine and mark as `PAID` |
| **Fines** | `GET` | `/api/fines` | `LIBRARIAN`, `ADMIN` | Master institutional fines ledger |

---

## 🎯 Step-by-Step Evaluation & Grading Guide

Follow this quick walkthrough to demonstrate all required features:

1. **Verify Service Discovery**:
   - Open `http://localhost:8761`. You will see `API-GATEWAY`, `AUTH-SERVICE`, `BOOK-SERVICE`, `RENTAL-SERVICE`, and `FINE-SERVICE` registered.

2. **Open Web Frontend**:
   - Open `frontend/index.html`. Notice the live green indicators for Gateway (:8080) and Eureka (:8761).

3. **Login as Student (Musuluri Sravanthi)**:
   - Click **Quick Demo Login** -> Select **Musuluri Sravanthi (2400030661)**.

4. **Borrow a Book**:
   - Find *"Building Microservices"* in the catalog.
   - Click **Borrow Book**. The available copies immediately decrease from 5 to 4.
   - Switch to the **My Active Loans** tab: The loan appears with status `ACTIVE LOAN` and a due date 14 days in the future.

5. **Test Duplicate Borrow Prevention**:
   - Go back to the **Book Catalog** tab and attempt to borrow *"Building Microservices"* again.
   - The platform rejects the request with an error:  
     `"DUPLICATE BORROW PROHIBITED: Student already has an active loan for this book!"` (HTTP 409 Conflict).

6. **Test Overdue Penalty Calculation (OpenFeign In Action)**:
   - In the **My Active Loans** tab, click **Simulate Overdue** on the active loan.
   - The loan card updates to an alert state: `OVERDUE` (5 days past due).
   - Click **Return Book**.
   - The system restocks the inventory back to 5 copies in `book-service`, calls `fine-service` via OpenFeign, and assesses a penalty:  
     $5\text{ days} \times ₹10 = \mathbf{₹50.00}$.

7. **Pay the Fine**:
   - Switch to the **Fines & Late Fees** tab.
   - The ₹50.00 fine appears under Outstanding Balance.
   - Click **Pay ₹50.00**. The fine is marked as `PAID` and the balance clears to ₹0.00.

8. **Test Staff Circulation Controls**:
   - Click **Quick Demo Login** -> Select **Chief Librarian** (`librarian / lib123`).
   - Notice the **Circulation Desk (Staff)** tab is now unlocked.
   - View campus-wide circulation records, inspect the overdue audit report, and view the institutional fines ledger.
   - Click **Add New Book** to expand institutional inventory.
