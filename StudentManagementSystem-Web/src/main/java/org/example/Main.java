package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.example.model.CampusStudent;
import org.example.repository.CampusStudentRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final CampusStudentRepository repository = new CampusStudentRepository();
    private static final Path WEB_ROOT = Path.of("src/main/resources/static");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/students", Main::handleStudents);
        server.createContext("/", Main::serveStatic);
        server.setExecutor(null);
        server.start();
        System.out.println("Student Management System running at http://localhost:8080");
    }

    private static void handleStudents(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && "/api/students".equals(path)) {
                sendJson(exchange, 200, studentsToJson(repository.getAllStudents()));
                return;
            }

            if (path.matches("/api/students/\\d+")) {
                long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));

                if ("GET".equals(method)) {
                    CampusStudent student = repository.getStudentById(id);
                    if (student == null) sendJson(exchange, 404, "{\"message\":\"Student not found\"}");
                    else sendJson(exchange, 200, studentToJson(student));
                    return;
                }

                if ("DELETE".equals(method)) {
                    boolean deleted = repository.deleteStudent(id);
                    sendJson(exchange, deleted ? 200 : 404,
                            deleted ? "{\"message\":\"Student deleted\"}" : "{\"message\":\"Student not found\"}");
                    return;
                }

                if ("PUT".equals(method)) {
                    Map<String, String> data = readForm(exchange);
                    CampusStudent student = studentFrom(data, id);
                    boolean updated = repository.updateStudent(student);
                    sendJson(exchange, updated ? 200 : 404,
                            updated ? "{\"message\":\"Student updated\"}" : "{\"message\":\"Student not found\"}");
                    return;
                }
            }

            if ("POST".equals(method) && "/api/students".equals(path)) {
                CampusStudent student = studentFrom(readForm(exchange), 0);
                boolean added = repository.addStudent(student);
                sendJson(exchange, added ? 201 : 400,
                        added ? "{\"message\":\"Student added\"}" : "{\"message\":\"Could not add student\"}");
                return;
            }

            sendJson(exchange, 405, "{\"message\":\"Method not allowed\"}");
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"message\":\"" + jsonEscape(e.getMessage()) + "\"}");
        } catch (SQLException e) {
            sendJson(exchange, 500, "{\"message\":\"Database error. Check DB settings and table.\"}");
            e.printStackTrace();
        } catch (Exception e) {
            sendJson(exchange, 500, "{\"message\":\"Something went wrong\"}");
            e.printStackTrace();
        }
    }

    private static CampusStudent studentFrom(Map<String, String> data, long id) {
        String name = data.getOrDefault("name", "").trim();
        String email = data.getOrDefault("email", "").trim();
        int age;
        try {
            age = Integer.parseInt(data.getOrDefault("age", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Age must be a number.");
        }
        if (name.isEmpty() || email.isEmpty() || age < 1 || age > 120)
            throw new IllegalArgumentException("Enter a name, email and valid age (1-120).");
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw new IllegalArgumentException("Enter a valid email address.");
        return new CampusStudent(id, name, email, age);
    }

    private static Map<String, String> readForm(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> values = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String studentsToJson(java.util.List<CampusStudent> students) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < students.size(); i++) {
            if (i > 0) json.append(",");
            json.append(studentToJson(students.get(i)));
        }
        return json.append("]").toString();
    }

    private static String studentToJson(CampusStudent s) {
        return "{\"id\":" + s.getId() + ",\"name\":\"" + jsonEscape(s.getName()) +
                "\",\"email\":\"" + jsonEscape(s.getEmail()) +
                "\",\"age\":" + s.getAge() + "}";
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void serveStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        String requested = exchange.getRequestURI().getPath();
        if (requested.equals("/")) requested = "/index.html";
        Path file = WEB_ROOT.resolve(requested.substring(1)).normalize();

        if (!file.startsWith(WEB_ROOT) || !Files.exists(file) || Files.isDirectory(file)) {
            byte[] notFound = "404 - File not found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, notFound.length);
            exchange.getResponseBody().write(notFound);
            exchange.close();
            return;
        }

        String type = file.toString().endsWith(".css") ? "text/css"
                : file.toString().endsWith(".js") ? "application/javascript" : "text/html";
        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
