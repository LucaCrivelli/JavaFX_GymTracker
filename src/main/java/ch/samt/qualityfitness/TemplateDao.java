package ch.samt.qualityfitness;

import ch.samt.qualityfitness.Database;
import ch.samt.qualityfitness.Template;
import ch.samt.qualityfitness.TemplateExerciseEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TemplateDao {

    public TemplateDao() {
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        String template = """
            CREATE TABLE IF NOT EXISTS template (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE
            )
        """;
        String templateExercise = """
            CREATE TABLE IF NOT EXISTS template_exercise (
                id INT AUTO_INCREMENT PRIMARY KEY,
                template_id INT NOT NULL,
                exercise_id INT NOT NULL,
                position INT NOT NULL,
                FOREIGN KEY (template_id) REFERENCES template(id) ON DELETE CASCADE,
                FOREIGN KEY (exercise_id) REFERENCES exercies(id)
            )
        """;
        String templateExerciseSet = """
            CREATE TABLE IF NOT EXISTS template_exercise_set (
                id INT AUTO_INCREMENT PRIMARY KEY,
                template_exercise_id INT NOT NULL,
                set_number INT NOT NULL,
                reps INT NOT NULL,
                FOREIGN KEY (template_exercise_id) REFERENCES template_exercise(id) ON DELETE CASCADE
            )
        """;
        try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(template);
            stmt.execute(templateExercise);
            stmt.execute(templateExerciseSet);
        } catch (SQLException e) {
            throw new RuntimeException("Impossibile inizializzare le tabelle dei template", e);
        }
    }

    public List<Template> getAllTemplates() {
        List<Template> templates = new ArrayList<>();
        String sql = "SELECT id, name FROM template ORDER BY name";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                templates.add(new Template(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel caricamento dei template", e);
        }
        return templates;
    }

    public List<TemplateExerciseEntry> getTemplateExercises(int templateId) {
        List<TemplateExerciseEntry> entries = new ArrayList<>();
        String sql = """
            SELECT te.id AS template_exercise_id, te.exercise_id, e.name AS exercise_name
            FROM template_exercise te
            JOIN exercies e ON e.id = te.exercise_id
            WHERE te.template_id = ?
            ORDER BY te.position
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, templateId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int templateExerciseId = rs.getInt("template_exercise_id");
                    entries.add(new TemplateExerciseEntry(
                            rs.getInt("exercise_id"),
                            rs.getString("exercise_name"),
                            getRepsForTemplateExercise(conn, templateExerciseId)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel caricamento degli esercizi del template", e);
        }
        return entries;
    }

    private List<Integer> getRepsForTemplateExercise(Connection conn, int templateExerciseId) throws SQLException {
        List<Integer> reps = new ArrayList<>();
        String sql = "SELECT reps FROM template_exercise_set WHERE template_exercise_id = ? ORDER BY set_number";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, templateExerciseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) reps.add(rs.getInt("reps"));
            }
        }
        return reps;
    }

    public boolean isNameTaken(String name, Integer excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM template WHERE name = ?"
                : "SELECT COUNT(*) FROM template WHERE name = ? AND id <> ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            if (excludeId != null) stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nella verifica del nome template", e);
        }
    }

    public int insertTemplate(String name, List<TemplateExerciseEntry> entries) {
        String insertTemplate = "INSERT INTO template (name) VALUES (?)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(insertTemplate, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    int templateId = keys.getInt(1);
                    saveExercises(conn, templateId, entries);
                    conn.commit();
                    return templateId;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel salvataggio del template", e);
        }
    }

    public void updateTemplate(int templateId, String name, List<TemplateExerciseEntry> entries) {
        String updateTemplate = "UPDATE template SET name = ? WHERE id = ?";
        String deleteExercises = "DELETE FROM template_exercise WHERE template_id = ?";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement updateStmt = conn.prepareStatement(updateTemplate)) {
                updateStmt.setString(1, name);
                updateStmt.setInt(2, templateId);
                updateStmt.executeUpdate();
            }
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteExercises)) {
                deleteStmt.setInt(1, templateId);
                deleteStmt.executeUpdate(); // CASCADE elimina anche i set collegati
            }
            saveExercises(conn, templateId, entries);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'aggiornamento del template", e);
        }
    }

    private void saveExercises(Connection conn, int templateId, List<TemplateExerciseEntry> entries) throws SQLException {
        String insertExercise = "INSERT INTO template_exercise (template_id, exercise_id, position) VALUES (?, ?, ?)";
        String insertSet = "INSERT INTO template_exercise_set (template_exercise_id, set_number, reps) VALUES (?, ?, ?)";

        int position = 0;
        for (TemplateExerciseEntry entry : entries) {
            int templateExerciseId;
            try (PreparedStatement stmt = conn.prepareStatement(insertExercise, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, templateId);
                stmt.setInt(2, entry.getExerciseId());
                stmt.setInt(3, position++);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    templateExerciseId = keys.getInt(1);
                }
            }

            int setNumber = 1;
            for (int reps : entry.getRepsPerSet()) {
                try (PreparedStatement stmt = conn.prepareStatement(insertSet)) {
                    stmt.setInt(1, templateExerciseId);
                    stmt.setInt(2, setNumber++);
                    stmt.setInt(3, reps);
                    stmt.executeUpdate();
                }
            }
        }
    }

    public void deleteTemplate(int id) {
        String sql = "DELETE FROM template WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'eliminazione del template", e);
        }
    }
}