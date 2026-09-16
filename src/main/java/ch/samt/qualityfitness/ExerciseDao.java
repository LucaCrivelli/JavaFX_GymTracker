package ch.samt.qualityfitness;

import ch.samt.qualityfitness.Database;
import ch.samt.qualityfitness.Exercise;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExerciseDao {

    public List<Exercise> getAllExercises() {
        List<Exercise> exercises = new ArrayList<>();
        String sql = "SELECT id, name, description, type FROM exercies ORDER BY name";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                exercises.add(new Exercise(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("type")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel caricamento degli esercizi", e);
        }
        return exercises;
    }

    public void insertExercise(String name, String description, String type) {
        String sql = "INSERT INTO exercies (name, description, type) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setString(3, type);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel salvataggio dell'esercizio", e);
        }
    }

    public void updateExercise(int id, String name, String description, String type) {
        String sql = "UPDATE exercies SET name = ?, description = ?, type = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setString(3, type);
            stmt.setInt(4, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'aggiornamento dell'esercizio", e);
        }
    }

    public void deleteExercise(int id) {
        String sql = "DELETE FROM exercies WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'eliminazione dell'esercizio", e);
        }
    }
}