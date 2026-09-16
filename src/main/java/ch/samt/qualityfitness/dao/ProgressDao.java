package ch.samt.qualityfitness.dao;

import ch.samt.qualityfitness.db.Database;
import ch.samt.qualityfitness.model.ProgressEntry;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgressDao {

    /** Tutte le esecuzioni dell'esercizio, una per workout, ordinate cronologicamente (tutto lo storico). */
    public List<ProgressEntry> getExecutions(int exerciseId) {
        List<ProgressEntry> entries = new ArrayList<>();
        String sql = """
            SELECT w.start_datetime AS execution_datetime,
                   SUM(ws.weight * ws.reps) AS total_volume
            FROM workout_exercise we
            JOIN workout w ON w.id = we.workout_id
            JOIN workout_set ws ON ws.workout_exercise_id = we.id
            WHERE we.exercise_id = ?
            GROUP BY we.id, w.start_datetime
            ORDER BY w.start_datetime ASC
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, exerciseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(new ProgressEntry(
                            rs.getTimestamp("execution_datetime").toLocalDateTime(),
                            rs.getInt("total_volume")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel caricamento del progresso", e);
        }
        return entries;
    }
}