# PULSE — Priority-driven Unified Logistics & Service Engine

A **Core Java 17+ & MySQL 8.0** University Campus Incident Management System built without heavy frameworks (no Spring Boot) to demonstrate Object-Oriented Programming, Multithreading, Java Collections Framework, and JDBC.

---

## 1. Project Introduction

**PULSE (Priority-driven Unified Logistics & Service Engine)** centralizes the reporting, prioritization, technician allocation, and resolution tracking of campus infrastructure incidents across four categories:

- **Laboratory Equipment** (`LabIncident`)
- **Electrical Failures & Power Disruptions** (`ElectricalIncident`)
- **Classroom Projectors & AV** (`ProjectorIncident`)
- **Campus Network & Wi-Fi** (`NetworkIncident`)

---

## 2. Problem Statement

Universities often rely on fragmented phone calls, emails, or paper registers to report broken lab equipment, power outages, classroom projector faults, and Wi-Fi failures. Without centralized priority queuing and specialization-based technician dispatch, critical emergencies (such as a 440V electrical outage or hazardous lab failure) can be delayed behind routine tasks.

---

## 3. Objectives

1. Provide a unified console interface for reporting and tracking campus incidents.
2. Automatically prioritize incidents (`CRITICAL > HIGH > MEDIUM > LOW`) with FIFO tie-breaking by creation timestamp.
3. Match incidents to available technicians based on specialization (`LAB`, `ELECTRICAL`, `PROJECTOR`, `NETWORK`).
4. Demonstrate concurrent task execution using Java worker threads (`TechnicianWorker`), `synchronized`, `wait()`, and `notifyAll()`.
5. Persist all incidents, technicians, and status transition audit logs in MySQL 8.0 via JDBC.

---

## 4. Features

1. **Report New Incident** (4 specialized incident types with category-specific fields)
2. **View All Incidents**
3. **View Incidents by Priority** (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`)
4. **View Incidents by Status** (`OPEN`, `IN_PROGRESS`, `RESOLVED`)
5. **View Incident Details** (polymorphic report + per-incident audit history)
6. **View Available Technicians**
7. **Add New Technician** (dynamically starts a new `TechnicianWorker` thread)
8. **Dispatch Next Incident** (`PriorityQueue` + `LinkedList` waiting queue)
9. **View Technician Workload** (shows assigned incident, resolved count, and live `Thread.State`)
10. **Mark Incident as Resolved** (updates DB, releases technician, notifies waiting queue)
11. **View Incident Resolution History**
12. **View Dashboard / Statistics**
13. **Exit Application** (gracefully shuts down worker threads)

---

## 5. Technologies Used

- **Language:** Java 17+ (compatible with Java 22)
- **Database:** MySQL 8.0
- **Database Connectivity:** JDBC (`PreparedStatement`, `ResultSet`, `try-with-resources`, `wasNull()`)
- **Driver:** MySQL Connector/J (`8.4.0`)
- **Build Tool:** Maven (`pom.xml`) — also compiles/runs directly via `javac`/`java`
- **IDE Compatibility:** Visual Studio Code (`.vscode/settings.json` included)

---

## 6. Project Structure

```text
PULSE/
├── .vscode/settings.json
├── database/
│   └── pulse.sql
├── src/main/java/pulse/
│   ├── PulseMain.java
│   ├── model/
│   │   ├── Incident.java
│   │   ├── LabIncident.java
│   │   ├── ElectricalIncident.java
│   │   ├── ProjectorIncident.java
│   │   ├── NetworkIncident.java
│   │   ├── Technician.java
│   │   ├── IncidentHistory.java
│   │   ├── Priority.java
│   │   └── IncidentStatus.java
│   ├── database/
│   │   ├── DatabaseConnection.java
│   │   └── DatabaseInitializer.java
│   ├── repository/
│   │   ├── IncidentRepository.java
│   │   ├── TechnicianRepository.java
│   │   └── HistoryRepository.java
│   ├── service/
│   │   ├── IncidentManager.java
│   │   ├── TechnicianManager.java
│   │   └── DispatchManager.java
│   └── threads/
│       └── TechnicianWorker.java
├── src/main/resources/
│   └── application.properties.example
├── pom.xml
└── README.md
```

---

## 7. MySQL Database Schema

Database name: `pulse_db`

1. **`technicians`**: `technician_id` (PK), `name`, `specialization` (`ENUM`), `available` (`BOOLEAN`), `current_incident_id`, `resolved_count`, `created_at`, `UNIQUE(name, specialization)`.
2. **`incidents`**: `incident_id` (PK), `incident_type` (`ENUM`), `title`, `description`, `location`, `priority` (`ENUM`), `status` (`ENUM`), `reported_by`, `assigned_technician_id` (FK → `technicians`), `specific_data`, `created_at`, `updated_at`, `resolved_at`.
3. **`incident_history`**: `history_id` (PK), `incident_id` (FK → `incidents`), `previous_status` (`ENUM NULL`), `new_status` (`ENUM`), `technician_id` (FK → `technicians`), `remarks`, `changed_at`.

---

## 8. Installation & MySQL Configuration

### Step 1: Initialize MySQL Database
Open MySQL Workbench or MySQL CLI and run:
```sql
SOURCE database/pulse.sql;
```

### Step 2: Configure Credentials (Never Hardcode Passwords)
Copy `src/main/resources/application.properties.example` to `src/main/resources/application.properties` and replace the placeholder credentials:
```properties
db.url=jdbc:mysql://localhost:3306/pulse_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=YOUR_MYSQL_USERNAME
db.password=YOUR_MYSQL_PASSWORD
```

---

## 9. How to Compile and Run

### Option A: Using Maven (Recommended if Maven is installed)
```bash
mvn clean compile
mvn exec:java
```

### Option B: Without Maven (Using `javac` and `java` directly)
1. Download `mysql-connector-j-8.4.0.jar` and place it in a `lib/` folder inside `PULSE/`.
2. Compile:
   ```powershell
   mkdir target\classes -Force
   dir /s /b src\main\java\*.java > sources.txt
   javac --release 17 -d target\classes @sources.txt
   del sources.txt
   ```
3. Run:
   ```powershell
   java -cp "target\classes;lib\mysql-connector-j-8.4.0.jar" pulse.PulseMain
   ```

---

## 10. Sample Console Output

```text
========================================
       PULSE - CAMPUS INCIDENT SYSTEM
========================================
1.  Report New Incident
2.  View All Incidents
3.  View Incidents by Priority
4.  View Incidents by Status
5.  View Incident Details
6.  View Available Technicians
7.  Add New Technician
8.  Dispatch Next Incident
9.  View Technician Workload
10. Mark Incident as Resolved
11. View Incident Resolution History
12. View Dashboard / Statistics
13. Exit
------------------------------------------------------------
Enter your choice (1-13): 8

--- Dispatching Next Priority Incident ---
[DISPATCHED] Incident #2 (CRITICAL, ELECTRICAL) assigned to Tech #2
Status changed: OPEN -> IN_PROGRESS. Worker thread notified.
```

---

## 11. Class-by-Class Explanation

- **`Incident` (abstract):** Encapsulates common fields (`incidentId`, `title`, `priority`, `status`, timestamps) and declares abstract methods (`getIncidentType()`, `getRequiredSpecialization()`, `getEstimatedResolutionMinutes()`, `getSpecificDetails()`, `serializeSpecificData()`).
- **`LabIncident`, `ElectricalIncident`, `ProjectorIncident`, `NetworkIncident`:** Concrete subclasses implementing category-specific fields and polymorphic behavior.
- **`Priority` & `IncidentStatus`:** Type-safe enums enforcing priority ranks (`1..4`) and lifecycle states (`OPEN -> IN_PROGRESS -> RESOLVED`).
- **`Technician`:** Models technician identity, specialization, availability, and synchronized `assignIncident()` / `releaseIncident()` methods.
- **`IncidentHistory`:** Immutable-style audit record of every status transition.
- **`DatabaseConnection` & `DatabaseInitializer`:** Centralized JDBC connection loader (from `application.properties`) and schema/seed initializer.
- **`IncidentRepository`, `TechnicianRepository`, `HistoryRepository`:** Data Access Objects (DAOs) using `PreparedStatement`, `ResultSet`, `try-with-resources`, and `wasNull()`.
- **`IncidentManager`:** Maintains `ArrayList<Incident>` and `LinkedList<IncidentHistory>` alongside repository persistence.
- **`TechnicianManager`:** Maintains `HashMap<Integer, Technician>` for $O(1)$ ID lookups.
- **`DispatchManager`:** Coordinates `PriorityQueue<Incident>`, `LinkedList<Incident>` waiting queue, and worker thread notifications.
- **`TechnicianWorker`:** `Runnable` worker thread per technician demonstrating thread lifecycle states and `wait()` / `notifyAll()`.
- **`PulseMain`:** Menu-driven console interface with input validation.

---

## 12. Key Concepts Explained for Viva

### A. Multithreading & Synchronization
- Each technician has a dedicated `TechnicianWorker` daemon thread (`NEW` → `RUNNABLE`).
- When idle, the worker calls `wait()` inside a `synchronized (this)` block, entering the `WAITING` state without consuming CPU.
- When `DispatchManager` assigns an incident, it calls `worker.assignWork(incident)`, which invokes `notifyAll()` to wake the worker thread.
- During simulated repair (`Thread.sleep(12000)`), the thread enters `TIMED_WAITING`.
- `DispatchManager` methods (`dispatchNextIncident`, `resolveIncident`) are `synchronized` so two technicians can never be assigned to the same incident simultaneously.
- On exit or manual resolution, `interrupt()` is handled cleanly without swallowing shutdown signals.

### B. `PriorityQueue` & Custom `Comparator`
- `DispatchManager.INCIDENT_COMPARATOR` compares `a.getPriority().getRank()` vs `b.getPriority().getRank()` (`CRITICAL=1` smallest rank = highest priority).
- If two incidents have the same priority, `a.getCreatedAt().compareTo(b.getCreatedAt())` ensures the older incident is dispatched first.
- If no technician with the required specialization is available, the incident is placed in a `LinkedList` `waitingQueue` so other incidents aren't blocked.

### C. JDBC Connectivity
- `try-with-resources` automatically closes `Connection`, `PreparedStatement`, and `ResultSet` to prevent resource leaks.
- `PreparedStatement` prevents SQL injection and handles parameterized queries.
- `ResultSet.wasNull()` checks whether nullable SQL columns (`assigned_technician_id`, `current_incident_id`, `previous_status`, `resolved_at`) were `NULL` in MySQL after calling `getInt()` or `getString()`.

---

## 13. Important Viva Questions & Answers

1. **Why is `Incident` an `abstract` class instead of an interface?**  
   Because all incidents share common state (ID, title, priority, status, timestamps) and concrete methods (`getDetailedReport()`, getters/setters), while forcing subclasses to override category-specific behavior (`getEstimatedResolutionMinutes()`, `getSpecificDetails()`).
2. **How do you prevent race conditions when assigning incidents?**  
   `dispatchNextIncident()` and `resolveIncident()` in `DispatchManager` are `synchronized`, and `Technician.assignIncident()` is `synchronized`, ensuring atomic check-and-assign.
3. **Why use `while` instead of `if` around `wait()`?**  
   To guard against spurious wakeups and verify the condition (`running && assignedIncident == null`) still holds when the thread wakes up.
4. **Why use `ResultSet.wasNull()` in JDBC?**  
   `rs.getInt("assigned_technician_id")` returns `0` when the database column is `NULL`. Calling `rs.wasNull()` distinguishes an actual `0` from SQL `NULL`.
5. **What is the time complexity of `PriorityQueue` and `HashMap` operations?**  
   `PriorityQueue` `offer()`/`poll()` is $O(\log n)$; `HashMap` `get()`/`put()` in `TechnicianManager` is $O(1)$ average case.
6. **Why avoid hardcoding database passwords?**  
   Hardcoding credentials exposes secrets in source control. Reading from `application.properties` (excluded via `.gitignore`) keeps credentials externalized.

---

## 14. Future Scope

- JavaFX or Swing desktop GUI / REST API layer.
- Automated email/SMS alerts for `CRITICAL` incidents.
- SLA breach timers and technician shift scheduling.
