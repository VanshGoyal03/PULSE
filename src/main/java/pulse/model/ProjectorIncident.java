package pulse.model;

import java.time.LocalDateTime;

/**
 * Represents a classroom projector or AV malfunction incident.
 */
public class ProjectorIncident extends Incident {
    public static final String TYPE = "PROJECTOR";

    private String classroomNumber;
    private String projectorModel;
    private String issueCategory;

    public ProjectorIncident(String title, String description, String location,
                             Priority priority, String reportedBy,
                             String classroomNumber, String projectorModel, String issueCategory) {
        super(title, description, location, priority, reportedBy);
        this.classroomNumber = classroomNumber == null ? "Unknown Room" : classroomNumber.trim();
        this.projectorModel = projectorModel == null ? "Standard AV" : projectorModel.trim();
        this.issueCategory = issueCategory == null ? "Display" : issueCategory.trim();
    }

    public ProjectorIncident(int incidentId, String title, String description, String location,
                             Priority priority, IncidentStatus status, String reportedBy,
                             Integer assignedTechnicianId, LocalDateTime createdAt,
                             LocalDateTime updatedAt, LocalDateTime resolvedAt,
                             String classroomNumber, String projectorModel, String issueCategory) {
        super(incidentId, title, description, location, priority, status, reportedBy,
                assignedTechnicianId, createdAt, updatedAt, resolvedAt);
        this.classroomNumber = classroomNumber;
        this.projectorModel = projectorModel;
        this.issueCategory = issueCategory;
    }

    @Override
    public String getIncidentType() {
        return TYPE;
    }

    @Override
    public String getRequiredSpecialization() {
        return TYPE;
    }

    @Override
    public int getEstimatedResolutionMinutes() {
        return 20;
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Classroom: %s | Model: %s | Issue: %s",
                classroomNumber, projectorModel, issueCategory);
    }

    @Override
    public String serializeSpecificData() {
        return classroomNumber + ";;" + projectorModel + ";;" + issueCategory;
    }

    public String getClassroomNumber() {
        return classroomNumber;
    }

    public void setClassroomNumber(String classroomNumber) {
        this.classroomNumber = classroomNumber;
    }

    public String getProjectorModel() {
        return projectorModel;
    }

    public void setProjectorModel(String projectorModel) {
        this.projectorModel = projectorModel;
    }

    public String getIssueCategory() {
        return issueCategory;
    }

    public void setIssueCategory(String issueCategory) {
        this.issueCategory = issueCategory;
    }
}
