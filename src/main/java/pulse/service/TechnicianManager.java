package pulse.service;

import pulse.model.Technician;
import pulse.repository.TechnicianRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Manages campus technicians using a HashMap for fast O(1) lookup by ID.
 */
public class TechnicianManager {
    private final TechnicianRepository technicianRepository;
    private final Map<Integer, Technician> technicianMap = new HashMap<>();

    public TechnicianManager(TechnicianRepository technicianRepository) {
        this.technicianRepository = technicianRepository;
    }

    public synchronized void loadFromDatabase() throws SQLException {
        technicianMap.clear();
        List<Technician> all = technicianRepository.findAll();
        for (Technician tech : all) {
            technicianMap.put(tech.getTechnicianId(), tech);
        }
    }

    public synchronized Technician addTechnician(String name, String specialization) throws SQLException {
        String normalizedSpec = specialization == null ? "" : specialization.trim().toUpperCase();
        if (!normalizedSpec.equals("LAB") && !normalizedSpec.equals("ELECTRICAL")
                && !normalizedSpec.equals("PROJECTOR") && !normalizedSpec.equals("NETWORK")) {
            throw new IllegalArgumentException(
                    "Invalid specialization. Must be LAB, ELECTRICAL, PROJECTOR, or NETWORK.");
        }

        Technician tech = new Technician(name, normalizedSpec);
        technicianRepository.save(tech);
        technicianMap.put(tech.getTechnicianId(), tech);
        return tech;
    }

    public synchronized Optional<Technician> findById(int technicianId) {
        return Optional.ofNullable(technicianMap.get(technicianId));
    }

    public synchronized List<Technician> getAllTechnicians() {
        return new ArrayList<>(technicianMap.values());
    }

    public synchronized List<Technician> getAvailableTechnicians() {
        List<Technician> available = new ArrayList<>();
        for (Technician tech : technicianMap.values()) {
            if (tech.isAvailable()) {
                available.add(tech);
            }
        }
        return available;
    }

    public synchronized Optional<Technician> findAvailableBySpecialization(String specialization) {
        for (Technician tech : technicianMap.values()) {
            if (tech.isAvailable() && tech.getSpecialization().equalsIgnoreCase(specialization)) {
                return Optional.of(tech);
            }
        }
        return Optional.empty();
    }

    public synchronized void updateTechnician(Technician technician) throws SQLException {
        technicianRepository.updateAssignment(technician);
        technicianMap.put(technician.getTechnicianId(), technician);
    }
}
