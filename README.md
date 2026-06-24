# Smart Queue Management System for Public Hospital

An advanced backend system built using **Java Spring Boot** designed to solve physical overcrowding, extended waiting times, and lack of real-time visibility in Malaysian public hospital registration counters.

---

## 📌 Project Background & Problem Statement

Public healthcare facilities in Malaysia experience heavy congestion daily, especially during early morning registration hours. Patients often arrive as early as 6:00 AM just to secure a limited daily queue ticket. This physical rush causes:
* Severe overcrowding in registration and waiting lobbies.
* Prohibitively long, uncomfortable physical waiting durations for vulnerable demographics (elderly, pregnant women, disabled patients, and those traveling from distant locations).
* Total lack of remote transparency regarding current queue numbers and processing speeds.
* Inefficiencies for counter staff managing manual sequencing, unexpected lunch breaks, missed turns, and cancellations.

---

### The Solution
The **Smart Queue Management System** transitions these physical bottlenecks into a digital, pre-arrival pipeline. By allowing patients to securely issue department-specific queue tickets online starting from 6:00 AM, monitor active progress live, and arrive precisely when their number is nearing service, the system minimizes counter congestion and enhances public healthcare operational efficiency.

---

## 🏛️ System Architecture

The application uses a **Layered Architecture Pattern**. The web layer receives browser and REST API requests, the service layer applies queue business rules, and the repository layer stores and retrieves data using Spring Data JPA.

```text
src/main/java/com/hospital/queue
│
├── config          # Timezone and web/CORS configuration
├── constant        # Shared API route names, messages, and queue rules
│
├── controller      # JSP page controller and REST API controllers
│   ├── PageController.java
│   ├── QueueController.java
│   ├── QueueCallController.java
│   └── RefDataController.java
│
├── service         # Core business logic for queue tickets and reference data
│   ├── QueueService.java
│   └── RefDataService.java
│
├── repository      # Spring Data JPA repositories
│   ├── QueueTicketRepo.java
│   ├── DepartmentRepo.java
│   ├── CounterRepo.java
│   ├── PhoneCodeRepo.java
│   └── IcStateRepo.java
│
├── model           # JPA entities and enums
│   ├── QueueTicket.java
│   ├── Department.java
│   ├── Counter.java
│   ├── QueueStatus.java
│   ├── IdentityType.java
│   └── DepartmentCode.java
│
├── dto             # Request and response DTO records
│   ├── TakeQueueRequest.java
│   ├── QueueResponse.java
│   ├── CurrentQueueResponse.java
│   ├── CallRequest.java
│   ├── StatusRequest.java
│   ├── ApiResponse.java
│   └── ErrorResponse.java
│
└── exception       # Custom exceptions and global API error handling
    ├── BadRequestException.java
    ├── NotFoundException.java
    ├── ConflictException.java
    └── GlobalExceptionHandler.java
```

The web pages are located in `src/main/webapp/WEB-INF/jsp`, and the JavaScript/CSS assets are located in `src/main/resources/static`.

---

## ⚙️ Core Business Rules & Guardrails

To accurately mimic real-world hospital operational flows, the service layer enforces the following strict operational guardrails:

1. **Online Queue Window:** The online queue opens daily **at 6:00 AM**. Before this time, the system rejects queue requests.
2. **Department-Specific Daily Quotas:** Each department has a maximum number of tickets per day. After the quota is reached, patients cannot take more numbers for that department.
3. **Department Prefixes:** Queue numbers automatically utilize department-specific prefixes:
   * **General Consultation:** `GEN` (Quota: 100)
   * **Pharmacy:** `PHA` (Quota: 150)
   * **Dental Clinic:** `DEN` (Quota: 40)
   * **Blood Test Unit:** `LAB` (Quota: 80)
   * **Specialist Clinic:** `SPC` (Quota: 50)
4. **Lunch Break Intermissions:** Queue calling is paused between **12:00 PM and 2:00 PM**. Patients can still check status, but staff cannot call tickets during break time.
5. **Duplicate Ticket Prevention:** A patient cannot hold more than one active ticket for the same department on the same date.
6. **Ticket State Transitions:** Tickets move through a clear lifecycle mapped via `QueueStatus`: `WAITING`, `CALLED`, `SERVING`, `COMPLETED`, `MISSED`, or `CANCELLED`.
7. **Daily Ephemeral Reset:** Queue numbers restart from 001 every day for each department.

---

## 🚀 REST API Endpoints

The system exposes the core functionalities as RESTful API web-services returning JSON payloads. Most successful responses use this wrapper format:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {}
}
```

### Admin API Login and Bearer Token

Use this endpoint in Postman to get an admin API access token before testing protected admin APIs such as queue calling and admin queue ticket listing.

* **Endpoint:** `POST /api/admin/auth/login`
* **Full URL:** `http://localhost:8081/api/admin/auth/login`
* **Sample Request Body (`JSON`):**

```json
{
  "username": "admin",
  "password": "admin123"
}
```

* **Sample Response Body (`JSON`):**

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "<generated-jwt-token>",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

In Postman, copy the value from `data.accessToken`, open the **Authorization** tab, choose **Bearer Token**, then paste the token into the **Token** field. Postman will send it as:

```http
Authorization: Bearer <generated-jwt-token>
```

### 1. Take Queue Number Online (Required)

* **Endpoint:** `POST /api/queueTickets`
* **Sample Request Body (`JSON`):**

```json
{
  "identityType": "MALAYSIAN",
  "identityNumber": "010203011234",
  "phoneCountryCode": "+60",
  "phoneNumber": "01123456789",
  "departmentCode": "GEN"
}
```

* **Sample Response Body (`JSON`):**

```json
{
  "success": true,
  "message": "Queue number successfully generated",
  "data": {
    "queueNumber": "GEN001",
    "departmentCode": "GEN",
    "departmentName": "General Consultation",
    "status": "WAITING",
    "counterName": null,
    "peopleAhead": 0,
    "queueDate": "2026-06-24",
    "createdAt": "2026-06-24T09:00:00",
    "calledAt": null,
    "completedAt": null
  }
}
```

### 2. View Current Queue Status (Required)

* **Endpoint:** `GET /api/queues/current?departmentCode=GEN`
* **Purpose:** Allows patients to view the current queue number being served for one department. If `departmentCode` is omitted, the API returns current queue information for all departments.

### 3. Search Ticket by Queue Number

* **Endpoint:** `GET /api/queueTickets/{queueNumber}`
* **Example:** `GET /api/queueTickets/GEN001`

### 4. List Queue Tickets

* **Endpoint:** `GET /api/queueTickets`
* **Example:** `GET /api/queueTickets?departmentCode=GEN&status=WAITING&page=0&size=10&sort=createdAt,asc`

Optional filters include `departmentCode`, `status`, `queueDate`, `page`, `size`, and `sort`.

### 5. Call Next Patient

* **Endpoint:** `POST /api/queueCalls`
* **Sample Request Body (`JSON`):**

```json
{
  "departmentCode": "GEN",
  "counterName": "Counter 1"
}
```

### 6. Update Ticket Status

* **Endpoint:** `PATCH /api/queueTickets/{queueNumber}/status`
* **Sample Request Body (`JSON`):**

```json
{
  "status": "COMPLETED"
}
```

Valid status values are `WAITING`, `CALLED`, `SERVING`, `COMPLETED`, `MISSED`, and `CANCELLED`.

### 7. Reference Data Endpoints

* `GET /api/departments` - Get all departments.
* `GET /api/departments/{code}` - Get one department by code.
* `GET /api/departments/{code}/queueTickets` - Get today's tickets for a department.
* `GET /api/departments/{code}/counters` - Get counters for a department.
* `GET /api/counters` - Get all counters.
* `GET /api/phoneCodes` - Get supported phone country codes.
* `GET /api/icStates` - Get Malaysian IC state codes.

---

## 🛠️ Step-by-Step Local Setup

Follow these exact steps to clone, configure, compile, and run the system locally:

### Step 1: Clone the Project via Command Prompt

1. Open your File Explorer and navigate to your working directory: `D:\Documents`.

2. Click on the address bar, type `cmd`, and press **Enter** to launch the terminal.

3. Run the following command to clone the repository:

```
git clone https://github.com/qiqi1023/hospital-queue-system.git
```


### Step 2: Configure Environment JRE & Java Compiler in IDE

Import the project into your IDE (Spring Tool Suite) and set up the Java environment:

1. Go to **Window** > **Preferences** > **Java** > **Installed JREs**.

2. Click **Add...**, select your **JDK 17** directory path, tick it as default, then click **Apply and Close**.

3. Go to **Java Compiler** settings within Preferences and set the **Compiler compliance level** strictly to **17**.



### Step 3: Configure Application Environment Properties

Review or update your active server configurations inside `src/main/resources/application.properties`:

```properties
# System Server Port Assignment
server.port=8081

# In-Memory Database Engine Configs
spring.datasource.url=jdbc:h2:mem:hospital_queue_db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect

# Web H2 Database Engine Dashboard
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.hibernate.show-sql=true

```

### Connect to Neon PostgreSQL

This project has a `neon` Spring profile for your Neon database. Keep your Neon password in environment variables, not in `application.properties`.

1. Open your Neon project named `smart-queue-mys`.
2. Click **Connect**.
3. Choose **Java** or **JDBC** if Neon shows stack options.
4. Copy the connection details and convert them to this format:

```properties
NEON_JDBC_URL=jdbc:postgresql://<your-neon-host>/<database-name>?sslmode=require&channel_binding=require
NEON_DB_USERNAME=<your-neon-role>
NEON_DB_PASSWORD=<your-neon-password>
SPRING_PROFILES_ACTIVE=neon
```

In PowerShell, you can set them for the current terminal session like this:

```powershell
$env:NEON_JDBC_URL="jdbc:postgresql://<your-neon-host>/<database-name>?sslmode=require&channel_binding=require"
$env:NEON_DB_USERNAME="<your-neon-role>"
$env:NEON_DB_PASSWORD="<your-neon-password>"
$env:SPRING_PROFILES_ACTIVE="neon"
.\mvnw.cmd spring-boot:run
```

If Neon gives you a URL that starts with `postgresql://`, change only the beginning to `jdbc:postgresql://` for Spring Boot JDBC.

Note: when the `neon` Spring profile is active, queue ticket data is saved to Neon PostgreSQL through the existing JPA entity and repository. The application reloads the current day's queue tickets from the database when it starts.

### Step 4: Build and Launch the Server inside IDE

1. Right-click on the root project folder named `hospital-queue-system`.

2. Select **Run As** > **Run Configurations...** (or **Maven build...**).

3. In the **Goals** text box, fill in exactly: `spring-boot:run`.

4. Click **Apply** and click **Run** to launch the backend server instance.



---

## 🧪 Postman API Validation & Web Access

### Web Interface Access

Once logs track a successful startup sequence, access the system's live front-end pages using these URLs:

```text
Customer Portal: http://localhost:8081/customer
Admin Login:     http://localhost:8081/admin/login
```

Admin credentials for the assignment demo:

```text
Username: admin
Password: admin123
```

The root URL `http://localhost:8081` redirects to the customer portal.

### Postman Testing

1. Open your **Postman Client**.
2. For customer queue registration, set up a **POST** request targeting `http://localhost:8081/api/queueTickets`.
3. Under the **Body** tab, select **raw** and set the format dropdown to **JSON**.
4. Paste the example request payload detailed in the REST API section above and hit **Send**.
5. For admin API testing, send a **POST** request to `http://localhost:8081/api/admin/auth/login` using username `admin` and password `admin123`.
6. From the login response, copy `data.accessToken`.
7. Open the request's **Authorization** tab in Postman, select **Bearer Token**, and paste `data.accessToken` into the **Token** field before sending protected admin API requests.

---

## 📝 Academic Integrity & AI Assistance Disclosure

* This system is developed as an assignment milestone for course **SECJ4383 Software Construction**.
* AI assistance was used only to support documentation refinement, debugging guidance, task planning, and explanation of implementation concepts.
* Any AI-assisted suggestions were reviewed, modified where necessary, tested, and understood by the group members before inclusion in the final submission.
* The group members take full responsibility for the submitted source code, system design, testing results, and final documentation.
* No AI-generated code is submitted without proper review, understanding, and disclosure.
