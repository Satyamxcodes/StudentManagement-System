# Student Management System (Java + JDBC + MySQL + HTML/CSS/JS)

A simple web-based student CRUD project. It uses Java's built-in HTTP server (no Spring Boot or Servlet dependency), JDBC for database access, and plain HTML/CSS/JavaScript for the frontend.

## Requirements
- JDK 25
- Maven
- MySQL Server

## 1. Create the database
Run `database.sql` in MySQL Workbench.

## 2. Set database environment variables
The app defaults to:
- URL: `jdbc:mysql://localhost:3306/CAMPUS_RECORDS_DB`
- Username: `root`

Set `DB_PASSWORD` to your local MySQL root password before starting the app.
Optional: set `DB_USERNAME` and `DB_URL` if your settings differ.

In IntelliJ IDEA: Run > Edit Configurations > Environment variables:
`DB_PASSWORD=your_actual_mysql_password`

Do not put your real password in source code or upload it to GitHub.

## 3. Run the project
Open this folder as a Maven project in IntelliJ IDEA, reload Maven, then run `org.example.Main`.

Open this address in your browser:
http://localhost:8080

Keep the Java program running while using the website.

## Features
- Add student
- View all students
- Search by ID, name or email
- Edit student
- Delete student
- MySQL persistence through JDBC

## Note
This is a small learning/demo project using Java's built-in `HttpServer`. For a larger production application, use a structured web framework and add authentication, authorization, CSRF protection, and stronger validation.
