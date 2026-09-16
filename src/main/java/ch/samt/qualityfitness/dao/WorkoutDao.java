package ch.samt.qualityfitness.dao;

import ch.samt.qualityfitness.db.Database;
import ch.samt.qualityfitness.model.Workout;
import ch.samt.qualityfitness.model.WorkoutExerciseData;
import ch.samt.qualityfitness.model.WorkoutExerciseData.SetData;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkoutDao {

    public WorkoutDao() {
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        String workout = """
            CREATE TABLE IF NOT EXISTS workout (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL UNIQUE,
                start_datetime DATETIME NOT NULL,
                end_datetime DATETIME NOT NULL
            )
        """;
        String workoutExercise = """
            CREATE TABLE IF NOT EXISTS workout_exercise (
                id INT AUTO_INCREMENT PRIMARY KEY,
                workout_id INT NOT NULL,
                exercise_id INT NOT NULL,
                position INT NOT NULL,
                FOREIGN KEY (workout_id) REFERENCES workout(id) ON DELETE CASCADE,
                FOREIGN KEY (exercise_id) REFERENCES exercies(id)
            )
        """;
        String workoutSet = """
            CREATE TABLE IF NOT EXISTS workout_set (
                id INT AUTO_INCREMENT PRIMARY KEY,
                workout_exercise_id INT NOT NULL,
                set_number INT NOT NULL,
                weight INT NOT NULL,
                reps INT NOT NULL,
                FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercise(id) ON DELETE CASCADE
            )
        """;
        try (Connection conn = Database.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(workout);
            stmt.execute(workoutExercise);
            stmt.execute(workoutSet);
        } catch (SQLException e) {
            throw new RuntimeException("Impossibile inizializzare le tabelle dei workout", e);
        }
    }

    public List<Workout> getAllWorkouts() {
        List<Workout> workouts = new ArrayList<>();
        String sql = "SELECT id, name, start_datetime, end_datetime FROM workout ORDER BY start_datetime DESC";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                workouts.add(new Workout(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getTimestamp("start_datetime").toLocalDateTime(),
                        rs.getTimestamp("end_datetime").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel caricamento dei workout", e);
        }
        return workouts;
    }

    public List<WorkoutExerciseData> getWorkoutExercises(int workoutId) {
        List<WorkoutExerciseData> result = new ArrayList<>();
        String sql = """
            SELECT we.id AS workout_exercise_id, we.exercise_id, e.name AS exercise_name
            FROM workout_exercise we
            JOIN exercies e ON e.id = we.exercise_id
            WHERE we.workout_id = ?
            ORDER BY we.position
        """;
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workoutId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int workoutExerciseId = rs.getInt("workout_exercise_id");
                    result.add(new WorkoutExerciseData(
                            rs.getInt("exercise_id"),
                            rs.getString("exercise_name"),
                            getSets(conn, workoutExerciseId)
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel caricamento degli esercizi del workout", e);
        }
        return result;
    }

    private List<SetData> getSets(Connection conn, int workoutExerciseId) throws SQLException {
        List<SetData> sets = new ArrayList<>();
        String sql = "SELECT weight, reps FROM workout_set WHERE workout_exercise_id = ? ORDER BY set_number";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, workoutExerciseId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) sets.add(new SetData(rs.getInt("weight"), rs.getInt("reps")));
            }
        }
        return sets;
    }

    public boolean isNameTaken(String name, Integer excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM workout WHERE name = ?"
                : "SELECT COUNT(*) FROM workout WHERE name = ? AND id <> ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            if (excludeId != null) stmt.setInt(2, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nella verifica del nome workout", e);
        }
    }

    /** True se il periodo [start, end) si sovrappone a un altro workout esistente. */
    public boolean hasOverlap(LocalDateTime start, LocalDateTime end, Integer excludeId) {
        String sql = excludeId == null
                ? "SELECT COUNT(*) FROM workout WHERE start_datetime < ? AND end_datetime > ?"
                : "SELECT COUNT(*) FROM workout WHERE start_datetime < ? AND end_datetime > ? AND id <> ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(end));
            stmt.setTimestamp(2, Timestamp.valueOf(start));
            if (excludeId != null) stmt.setInt(3, excludeId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel controllo di sovrapposizione", e);
        }
    }

    public int insertWorkout(String name, LocalDateTime start, LocalDateTime end, List<WorkoutExerciseData> exercises) {
        String insertWorkout = "INSERT INTO workout (name, start_datetime, end_datetime) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(insertWorkout, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, name);
                stmt.setTimestamp(2, Timestamp.valueOf(start));
                stmt.setTimestamp(3, Timestamp.valueOf(end));
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    int workoutId = keys.getInt(1);
                    saveExercises(conn, workoutId, exercises);
                    conn.commit();
                    return workoutId;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore nel salvataggio del workout", e);
        }
    }

    public void updateWorkout(int workoutId, String name, LocalDateTime start, LocalDateTime end, List<WorkoutExerciseData> exercises) {
        String updateWorkout = "UPDATE workout SET name = ?, start_datetime = ?, end_datetime = ? WHERE id = ?";
        String deleteExercises = "DELETE FROM workout_exercise WHERE workout_id = ?";
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(updateWorkout)) {
                stmt.setString(1, name);
                stmt.setTimestamp(2, Timestamp.valueOf(start));
                stmt.setTimestamp(3, Timestamp.valueOf(end));
                stmt.setInt(4, workoutId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(deleteExercises)) {
                stmt.setInt(1, workoutId);
                stmt.executeUpdate(); // CASCADE elimina anche i set collegati
            }
            saveExercises(conn, workoutId, exercises);
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'aggiornamento del workout", e);
        }
    }

    private void saveExercises(Connection conn, int workoutId, List<WorkoutExerciseData> exercises) throws SQLException {
        String insertExercise = "INSERT INTO workout_exercise (workout_id, exercise_id, position) VALUES (?, ?, ?)";
        String insertSet = "INSERT INTO workout_set (workout_exercise_id, set_number, weight, reps) VALUES (?, ?, ?, ?)";

        int position = 0;
        for (WorkoutExerciseData exercise : exercises) {
            int workoutExerciseId;
            try (PreparedStatement stmt = conn.prepareStatement(insertExercise, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, workoutId);
                stmt.setInt(2, exercise.getExerciseId());
                stmt.setInt(3, position++);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    keys.next();
                    workoutExerciseId = keys.getInt(1);
                }
            }

            int setNumber = 1;
            for (SetData set : exercise.getSets()) {
                try (PreparedStatement stmt = conn.prepareStatement(insertSet)) {
                    stmt.setInt(1, workoutExerciseId);
                    stmt.setInt(2, setNumber++);
                    stmt.setInt(3, set.weight());
                    stmt.setInt(4, set.reps());
                    stmt.executeUpdate();
                }
            }
        }
    }

    public void deleteWorkout(int id) {
        String sql = "DELETE FROM workout WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Errore nell'eliminazione del workout", e);
        }
    }
}