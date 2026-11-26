package pulse;

import pulse.database.DatabaseConnection;
import pulse.database.DatabaseInitializer;
import pulse.model.ElectricalIncident;
import pulse.model.Incident;
import pulse.model.IncidentHistory;
import pulse.model.IncidentStatus;
import pulse.model.LabIncident;
import pulse.model.NetworkIncident;
import pulse.model.Priority;
import pulse.model.ProjectorIncident;
import pulse.model.Technician;
import pulse.repository.HistoryRepository;
import pulse.repository.IncidentRepository;
import pulse.repository.TechnicianRepository;
import pulse.service.DispatchManager;
import pulse.service.IncidentManager;
import pulse.service.TechnicianManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Entry point and console UI for PULSE (Priority-driven Unified Logistics & Service Engine).
 */
public class PulseMain {
    private static final String LINE = "============================================================";
    private static final String THIN_LINE = "------------------------------------------------------------";

    private final Scanner scanner = new Scanner(System.in);
    private final IncidentManager incidentManager;
    private final TechnicianManager technicianManager;
    private final DispatchManager dispatchManager;

    public PulseMain(IncidentManager incidentManager,
                     TechnicianManager technicianManager,
                     DispatchManager dispatchManager) {
        this.incidentManager = incidentManager;
        this.technicianManager = technicianManager;
        this.dispatchManager = dispatchManager;
    }

    public static void main(String[] args) {
        System.out.println(LINE);
        System.out.println("       PULSE - CAMPUS INCIDENT MANAGEMENT SYSTEM");
        System.out.println("  Priority-driven Unified Logistics & Service Engine");
        System.out.println(LINE);

        if (!DatabaseConnection.isConfigured()) {
            System.err.println("[ERROR] MySQL configuration file not found.");
            System.err.println("Please copy 'src/main/resources/application.properties.example' to");
            System.err.println("'src/main/resources/application.properties' and set your MySQL credentials.");
            return;
        }

        IncidentRepository incidentRepo = new IncidentRepository();
        TechnicianRepository techRepo = new TechnicianRepository();
        HistoryRepository historyRepo = new HistoryRepository();

        IncidentManager incidentManager = new IncidentManager(incidentRepo, historyRepo);
        TechnicianManager technicianManager = new TechnicianManager(techRepo);
        DispatchManager dispatchManager = new DispatchManager(incidentManager, technicianManager);

        try {
            new DatabaseInitializer().initialize();
            incidentManager.loadFromDatabase();
            technicianManager.loadFromDatabase();
            dispatchManager.initializeWorkersAndQueues();
            System.out.println("[OK] Connected to MySQL (pulse_db) and initialized worker threads.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to connect or initialize database: " + e.getMessage());
            System.err.println("Verify MySQL 8.0 is running, database 'pulse_db' exists, and credentials are valid.");
            return;
        }

        PulseMain app = new PulseMain(incidentManager, technicianManager, dispatchManager);
        app.runMenuLoop();
    }

    private void runMenuLoop() {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter your choice (1-13): ", 1, 13);
            System.out.println();

            try {
                switch (choice) {
                    case 1 -> handleReportIncident();
                    case 2 -> handleViewAllIncidents();
                    case 3 -> handleViewByPriority();
                    case 4 -> handleViewByStatus();
                    case 5 -> handleViewIncidentDetails();
                    case 6 -> handleViewAvailableTechnicians();
                    case 7 -> handleAddTechnician();
                    case 8 -> handleDispatchNextIncident();
                    case 9 -> handleViewTechnicianWorkload();
                    case 10 -> handleResolveIncident();
                    case 11 -> handleViewHistory();
                    case 12 -> handleViewDashboard();
                    case 13 -> {
                        dispatchManager.shutdownAllWorkers();
                        System.out.println("Shutting down technician worker threads... Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("[ERROR] Invalid choice.");
                }
            } catch (SQLException e) {
                System.err.println("[DB ERROR] Database operation failed: " + e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.err.println("[ERROR] " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("       PULSE - CAMPUS INCIDENT SYSTEM   ");
        System.out.println("========================================");
        System.out.println("1.  Report New Incident");
        System.out.println("2.  View All Incidents");
        System.out.println("3.  View Incidents by Priority");
        System.out.println("4.  View Incidents by Status");
        System.out.println("5.  View Incident Details");
        System.out.println("6.  View Available Technicians");
        System.out.println("7.  Add New Technician");
        System.out.println("8.  Dispatch Next Incident");
        System.out.println("9.  View Technician Workload");
        System.out.println("10. Mark Incident as Resolved");
        System.out.println("11. View Incident Resolution History");
        System.out.println("12. View Dashboard / Statistics");
        System.out.println("13. Exit");
        System.out.println(THIN_LINE);
    }

    private void handleReportIncident() throws SQLException {
        System.out.println("--- Report New Campus Incident ---");
        System.out.println("Select Incident Category:");
        System.out.println("1. Laboratory Equipment (LAB)");
        System.out.println("2. Electrical Failure (ELECTRICAL)");
        System.out.println("3. Classroom Projector (PROJECTOR)");
        System.out.println("4. Network / Wi-Fi (NETWORK)");
        int typeChoice = readInt("Category (1-4): ", 1, 4);

        String title = readNonEmptyString("Incident Title: ");
        String description = readNonEmptyString("Description: ");
        String location = readNonEmptyString("Location (Building/Room): ");
        Priority priority = readPriority();
        String reportedBy = readNonEmptyString("Reported By (Name/ID): ");

        Incident incident = switch (typeChoice) {
            case 1 -> {
                String labName = readNonEmptyString("Lab Name: ");
                String equipId = readNonEmptyString("Equipment ID: ");
                boolean hazard = readBoolean("Is there a safety hazard? (y/n): ");
                yield new LabIncident(title, description, location, priority, reportedBy, labName, equipId, hazard);
            }
            case 2 -> {
                String panel = readNonEmptyString("Panel / Circuit ID: ");
                String voltage = readNonEmptyString("Voltage Level (e.g., 230V / 440V): ");
                boolean outage = readBoolean("Is there a power outage? (y/n): ");
                yield new ElectricalIncident(title, description, location, priority, reportedBy, panel, voltage, outage);
            }
            case 3 -> {
                String room = readNonEmptyString("Classroom Number: ");
                String model = readNonEmptyString("Projector Model: ");
                String issue = readNonEmptyString("Issue Category (Lamp/HDMI/Power/Focus): ");
                yield new ProjectorIncident(title, description, location, priority, reportedBy, room, model, issue);
            }
            case 4 -> {
                String zone = readNonEmptyString("Building Zone: ");
                String ssid = readNonEmptyString("Network SSID: ");
                boolean multiUser = readBoolean("Affects multiple users? (y/n): ");
                yield new NetworkIncident(title, description, location, priority, reportedBy, zone, ssid, multiUser);
            }
            default -> throw new IllegalStateException("Unexpected category: " + typeChoice);
        };

        incidentManager.reportIncident(incident);
        dispatchManager.enqueueIncident(incident);

        System.out.println("\n[SUCCESS] Incident #" + incident.getIncidentId() + " reported with status OPEN.");
        System.out.println("Added to priority dispatch queue (" + incident.getPriority() + ").");
    }

    private void handleViewAllIncidents() {
        System.out.println("--- All Reported Incidents ---");
        printIncidentList(incidentManager.getAllIncidents());
    }

    private void handleViewByPriority() {
        Priority priority = readPriority();
        System.out.println("--- Incidents with Priority: " + priority + " ---");
        printIncidentList(incidentManager.getIncidentsByPriority(priority));
    }

    private void handleViewByStatus() {
        IncidentStatus status = readStatus();
        System.out.println("--- Incidents with Status: " + status + " ---");
        printIncidentList(incidentManager.getIncidentsByStatus(status));
    }

    private void handleViewIncidentDetails() throws SQLException {
        int id = readInt("Enter Incident ID: ", 1, Integer.MAX_VALUE);
        Optional<Incident> incidentOpt = incidentManager.findById(id);
        if (incidentOpt.isEmpty()) {
            System.out.println("[NOT FOUND] No incident found with ID #" + id);
            return;
        }

        System.out.println(LINE);
        System.out.println(incidentOpt.get().getDetailedReport());
        System.out.println(THIN_LINE);
        System.out.println("Status History:");
        List<IncidentHistory> history = incidentManager.getHistoryForIncident(id);
        if (history.isEmpty()) {
            System.out.println("  No history entries recorded.");
        } else {
            history.forEach(h -> System.out.println("  " + h));
        }
        System.out.println(LINE);
    }

    private void handleViewAvailableTechnicians() {
        System.out.println("--- Available Technicians ---");
        List<Technician> available = technicianManager.getAvailableTechnicians();
        if (available.isEmpty()) {
            System.out.println("No technicians are currently available.");
            return;
        }
        available.forEach(System.out::println);
    }

    private void handleAddTechnician() throws SQLException {
        System.out.println("--- Add New Technician ---");
        String name = readNonEmptyString("Technician Name: ");
        System.out.println("Specializations: LAB, ELECTRICAL, PROJECTOR, NETWORK");
        String spec = readNonEmptyString("Specialization: ").toUpperCase();

        Technician tech = technicianManager.addTechnician(name, spec);
        dispatchManager.startWorkerForTechnician(tech);
        System.out.println("[SUCCESS] Added " + tech + " and started worker thread.");
    }

    private void handleDispatchNextIncident() throws SQLException {
        System.out.println("--- Dispatching Next Priority Incident ---");
        Optional<Incident> dispatched = dispatchManager.dispatchNextIncident();
        if (dispatched.isPresent()) {
            Incident inc = dispatched.get();
            System.out.println("[DISPATCHED] Incident #" + inc.getIncidentId() + " (" + inc.getPriority()
                    + ", " + inc.getIncidentType() + ") assigned to Tech #" + inc.getAssignedTechnicianId());
            System.out.println("Status changed: OPEN -> IN_PROGRESS. Worker thread notified.");
        } else {
            System.out.println("[INFO] No dispatchable incident right now.");
            System.out.println("Pending/Waiting incidents: " + dispatchManager.getPendingQueueSize()
                    + " (Waiting for matching technician: " + dispatchManager.getWaitingQueueSize() + ")");
        }
    }

    private void handleViewTechnicianWorkload() {
        System.out.println("--- Technician Workload & Thread States ---");
        List<Technician> all = technicianManager.getAllTechnicians();
        Map<Integer, Thread.State> threadStates = dispatchManager.getWorkerThreadStates();
        if (all.isEmpty()) {
            System.out.println("No technicians registered.");
            return;
        }
        for (Technician tech : all) {
            Thread.State state = threadStates.getOrDefault(tech.getTechnicianId(), Thread.State.NEW);
            System.out.printf("%s | Thread: %s%n", tech, state);
        }
    }

    private void handleResolveIncident() throws SQLException {
        int id = readInt("Enter Incident ID to mark as RESOLVED: ", 1, Integer.MAX_VALUE);
        Optional<Incident> incidentOpt = incidentManager.findById(id);
        if (incidentOpt.isEmpty()) {
            System.out.println("[NOT FOUND] Incident #" + id + " does not exist.");
            return;
        }
        if (incidentOpt.get().getStatus() == IncidentStatus.RESOLVED) {
            System.out.println("[INFO] Incident #" + id + " is already RESOLVED.");
            return;
        }

        System.out.print("Resolution remarks (press Enter for default): ");
        String remarks = scanner.nextLine().trim();
        if (remarks.isEmpty()) {
            remarks = "Manually verified and marked resolved.";
        }

        boolean resolved = dispatchManager.resolveIncident(id, remarks);
        if (resolved) {
            System.out.println("[SUCCESS] Incident #" + id + " marked RESOLVED. Technician released and dispatchers notified.");
        } else {
            System.out.println("[ERROR] Could not resolve Incident #" + id);
        }
    }

    private void handleViewHistory() throws SQLException {
        System.out.println("--- Incident Resolution & Status History ---");
        List<IncidentHistory> history = incidentManager.getAllHistory();
        if (history.isEmpty()) {
            System.out.println("No history records found.");
            return;
        }
        history.forEach(System.out::println);
    }

    private void handleViewDashboard() {
        List<Incident> allIncidents = incidentManager.getAllIncidents();
        List<Technician> allTechs = technicianManager.getAllTechnicians();

        long openCount = allIncidents.stream().filter(i -> i.getStatus() == IncidentStatus.OPEN).count();
        long inProgressCount = allIncidents.stream().filter(i -> i.getStatus() == IncidentStatus.IN_PROGRESS).count();
        long resolvedCount = allIncidents.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED).count();

        long criticalCount = allIncidents.stream().filter(i -> i.getPriority() == Priority.CRITICAL).count();
        long highCount = allIncidents.stream().filter(i -> i.getPriority() == Priority.HIGH).count();
        long mediumCount = allIncidents.stream().filter(i -> i.getPriority() == Priority.MEDIUM).count();
        long lowCount = allIncidents.stream().filter(i -> i.getPriority() == Priority.LOW).count();

        long availableTechs = allTechs.stream().filter(Technician::isAvailable).count();

        System.out.println(LINE);
        System.out.println("               PULSE SYSTEM DASHBOARD");
        System.out.println(LINE);
        System.out.printf("Total Incidents       : %d%n", allIncidents.size());
        System.out.printf("  - OPEN              : %d%n", openCount);
        System.out.printf("  - IN_PROGRESS       : %d%n", inProgressCount);
        System.out.printf("  - RESOLVED          : %d%n", resolvedCount);
        System.out.println(THIN_LINE);
        System.out.println("By Priority:");
        System.out.printf("  CRITICAL: %d | HIGH: %d | MEDIUM: %d | LOW: %d%n",
                criticalCount, highCount, mediumCount, lowCount);
        System.out.println(THIN_LINE);
        System.out.printf("Total Technicians     : %d (Available: %d, Busy: %d)%n",
                allTechs.size(), availableTechs, allTechs.size() - availableTechs);
        System.out.printf("Pending Dispatch Queue: %d (Waiting Queue: %d)%n",
                dispatchManager.getPendingQueueSize(), dispatchManager.getWaitingQueueSize());
        System.out.println(LINE);
    }

    private void printIncidentList(List<Incident> list) {
        if (list.isEmpty()) {
            System.out.println("No incidents found matching the criteria.");
            return;
        }
        System.out.println(THIN_LINE);
        for (Incident incident : list) {
            System.out.println(incident);
        }
        System.out.println(THIN_LINE);
    }

    private int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("[INVALID] Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("[INVALID] Please enter a valid integer.");
            }
        }
    }

    private String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[INVALID] Input cannot be empty.");
        }
    }

    private boolean readBoolean(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes") || input.equals("true")) {
                return true;
            }
            if (input.equals("n") || input.equals("no") || input.equals("false")) {
                return false;
            }
            System.out.println("[INVALID] Please enter 'y' or 'n'.");
        }
    }

    private Priority readPriority() {
        System.out.println("Select Priority: 1. CRITICAL  2. HIGH  3. MEDIUM  4. LOW");
        int choice = readInt("Priority (1-4): ", 1, 4);
        return switch (choice) {
            case 1 -> Priority.CRITICAL;
            case 2 -> Priority.HIGH;
            case 3 -> Priority.MEDIUM;
            case 4 -> Priority.LOW;
            default -> throw new IllegalStateException("Unexpected priority: " + choice);
        };
    }

    private IncidentStatus readStatus() {
        System.out.println("Select Status: 1. OPEN  2. IN_PROGRESS  3. RESOLVED");
        int choice = readInt("Status (1-3): ", 1, 3);
        return switch (choice) {
            case 1 -> IncidentStatus.OPEN;
            case 2 -> IncidentStatus.IN_PROGRESS;
            case 3 -> IncidentStatus.RESOLVED;
            default -> throw new IllegalStateException("Unexpected status: " + choice);
        };
    }
}
