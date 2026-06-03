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

The application is structured following a strict **Layered Architecture Pattern** to ensure a clear separation of concerns among HTTP handling, business validation rules, data persistence, and data transport layers.


```

src/main/java/com/example/hospitalqueue
│
├── controller      # REST Controllers handling incoming HTTP requests
│   ├── QueueTicketController.java
│   └── QueueManagementController.java
│
├── service         # Service Layer encapsulating core business logic
│   ├── QueueTicketService.java
│   └── QueueManagementService.java
│
├── repository      # Data Access Objects (DAO) interfacing with the database
│   ├── QueueTicketRepository.java
│   ├── DepartmentRepository.java
│   └── CounterRepository.java
│
├── model           # Domain Entities representing database schemas
│   ├── QueueTicket.java
│   ├── Department.java
│   ├── Counter.java
│   └── QueueStatus.java
│
├── dto             # Data Transfer Objects wrapping request/response payloads
│   ├── QueueTicketRequest.java
│   └── QueueTicketResponse.java
│
└── exception       # Centralized Exception Handling definitions
├── ResourceNotFoundException.java
└── QueueClosedException.java

```

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

The system exposes the core functionalities as RESTful API web-services returning JSON payloads:

### 1. Take Queue Number Online (Required)
* **Endpoint:** `POST /api/queue-tickets`
* **Sample Request Body (`JSON`):**
```json
{
  "patientName": "Nur Aisyah",
  "icNumber": "010203011234",
  "phoneNumber": "01123456789",
  "department": "General Consultation",
  "visitReason": "Fever and cough"
}

```

* **Sample Response Body (`JSON`):**

```json
{
  "ticketId": 1,
  "queueNumber": "GEN001",
  "patientName": "Nur Aisyah",
  "department": "General Consultation",
  "status": "WAITING",
  "message": "Queue number successfully generated."
}

```

### 2. View Current Queue Status (Required)

* **Endpoint:** `GET /api/queues/current?department=General Consultation`
* **Purpose:** Allows patients to view the current queue number being served for a department.

### 3. Additional Endpoints for Full System Demo

* `GET /api/queue-tickets/{queueNumber}` - Search ticket by queue number.
* `PUT /api/queue-tickets/{queueNumber}/call` - Call next or selected queue ticket.
* `PUT /api/queue-tickets/{queueNumber}/status` - Update ticket status.

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

Note: the application currently keeps queue tickets in memory inside `QueueService`. The Neon connection is configured, but ticket data will only be saved in Neon after adding JPA entities and repositories for the queue tickets.

### Step 4: Build and Launch the Server inside IDE

1. Right-click on the root project folder named `hospital-queue-system`.

2. Select **Run As** > **Run Configurations...** (or **Maven build...**).

3. In the **Goals** text box, fill in exactly: `spring-boot:run`.

4. Click **Apply** and click **Run** to launch the backend server instance.



---

## 🧪 Postman API Validation & Web Access

### Web Interface Access

Once logs track a successful startup sequence, access the system's live front-end portal layout by opening your browser and heading to:

```
http://localhost:8081
```

### Postman Testing

1. Open your **Postman Client**.
2. Set up a **POST** request targeting `http://localhost:8081/api/queue-tickets`.
3. Under the **Body** tab, select **raw** and set the format dropdown to **JSON**.
4. Paste the example request payload detailed in the REST API section above and hit **Send**.

---

## 📝 Academic Integrity & Disclaimer

* This system is developed as an assignment milestone for course **SECJ4383 Software Construction**.
* All code contained in this repository has been independently developed and thoroughly understood by our group members in complete compliance with academic regulations.
