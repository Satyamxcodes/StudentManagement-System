package org.example.repository;

import org.example.model.CampusStudent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CampusStudentRepository {
    private static final String URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:mysql://localhost:3306/CAMPUS_RECORDS_DB");
    private static final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "root");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new SQLException("DB_PASSWORD environment variable is not set.");
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public List<CampusStudent> getAllStudents() throws SQLException {
        List<CampusStudent> campus_students = new ArrayList<>();
        String sql = "SELECT id, name, email, age FROM campus_students ORDER BY id DESC";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) campus_students.add(mapRow(rs));
        }
        return campus_students;
    }

    public CampusStudent getStudentById(long id) throws SQLException {
        String sql = "SELECT id, name, email, age FROM campus_students WHERE id = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public boolean addStudent(CampusStudent student) throws SQLException {
        String sql = "INSERT INTO campus_students(name, email, age) VALUES (?, ?, ?)";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, student.getName());
            ps.setString(2, student.getEmail());
            ps.setInt(3, student.getAge());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean updateStudent(CampusStudent student) throws SQLException {
        String sql = "UPDATE campus_students SET name = ?, email = ?, age = ? WHERE id = ?";
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, student.getName());
            ps.setString(2, student.getEmail());
            ps.setInt(3, student.getAge());
            ps.setLong(4, student.getId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteStudent(long id) throws SQLException {
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM campus_students WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    private CampusStudent mapRow(ResultSet rs) throws SQLException {
        return new CampusStudent(rs.getLong("id"), rs.getString("name"),
                rs.getString("email"), rs.getInt("age"));
    }
}
