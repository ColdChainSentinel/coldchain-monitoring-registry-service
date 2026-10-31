package pe.edu.upc.coldchain.integration;

import java.io.File;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Owns only processes and a disposable database created by this test run. */
final class ServiceStack implements AutoCloseable {
    static final ObjectMapper JSON = new ObjectMapper();
    private final Path root = Path.of(System.getProperty("repository.root")).toAbsolutePath().normalize();
    private final Path output = root.resolve("tests/integration/target/stack");
    private final String mode = System.getProperty("integration.database", "postgres");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final String password = UUID.randomUUID().toString();
    private PostgreSQLContainer postgres;
    private Process registry;
    private Process telemetry;
    private Process alerts;
    private int registryPort;
    private int telemetryPort;
    private int alertsPort;
    String point;
    String noRulePoint;

    void start() throws Exception {
        if (!mode.equals("h2") && !mode.equals("postgres")) {
            throw new IllegalArgumentException("integration.database debe ser h2 o postgres");
        }
        Files.createDirectories(output);
        validateBuild("monitoring-registry", "monitoring-management", "pe.edu.upc.coldchain.MonitoringApplication");
        validateBuild("telemetry-ingestion", "telemetry-ingestion", "pe.edu.upc.coldchain.telemetry.TelemetryApplication");
        validateBuild("alert-management", "alert-management", "pe.edu.upc.coldchain.alerts.AlertsApplication");
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "acceptance-stack-cleanup"));
        try {
            if (mode.equals("postgres")) {
                postgres = new PostgreSQLContainer("postgres:16.6-alpine")
                        .withDatabaseName("control").withUsername("test_admin").withPassword(password);
                postgres.start();
                try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), "test_admin", password);
                        var statement = connection.createStatement()) {
                    for (String name : List.of("registry", "telemetry", "alerts")) {
                        statement.execute("CREATE ROLE " + name + " LOGIN PASSWORD '" + password + "'");
                        statement.execute("CREATE DATABASE " + name + " OWNER " + name);
                        statement.execute("REVOKE CONNECT ON DATABASE " + name + " FROM PUBLIC");
                    }
                }
            }
            registryPort = freePort();
            registry = launch("monitoring-registry", "monitoring-management", "registry", registryPort,
                    "pe.edu.upc.coldchain.MonitoringApplication");
            awaitHealthy(registry, registryPort, "registry");
            point = createPoint("IT-RULE");
            noRulePoint = createPoint("IT-NO-RULE");
            Files.writeString(output.resolve("rules.yml"), "coldchain:\n  alerts:\n    rules:\n      \""
                    + point + "\":\n        threshold-min: 2\n        threshold-max: 8\n");
            alertsPort = freePort();
            startAlerts();
            telemetryPort = freePort();
            telemetry = launch("telemetry-ingestion", "telemetry-ingestion", "telemetry", telemetryPort,
                    "pe.edu.upc.coldchain.telemetry.TelemetryApplication");
            awaitHealthy(telemetry, telemetryPort, "telemetry");
            Files.writeString(output.resolve("environment.txt"), "database=" + mode
                    + "\nregistry=" + registryPort + "\ntelemetry=" + telemetryPort + "\nalerts=" + alertsPort + "\n");
        } catch (Exception | Error failure) {
            close();
            throw failure;
        }
    }

    private void validateBuild(String folder, String jar, String mainClass) {
        Path target = root.resolve("services/" + folder + "/target");
        Path required = mode.equals("postgres") ? target.resolve(jar + "-0.1.0.jar")
                : target.resolve("integration-classpath.txt");
        if (!Files.isRegularFile(required)
                || (mode.equals("h2") && !Files.isRegularFile(target.resolve("classes/" + mainClass.replace('.', '/') + ".class")))) {
            throw new IllegalStateException("Falta preparar " + folder + ": consulte tests/integration/README.md (" + required + ")");
        }
    }

    private Process launch(String folder, String jar, String database, int port, String mainClass) throws Exception {
        Path target = root.resolve("services/" + folder + "/target");
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        List<String> command = new ArrayList<>(List.of(Path.of(System.getProperty("java.home"), "bin", executable).toString(), "-Xmx256m"));
        if (mode.equals("postgres")) {
            command.addAll(List.of("-jar", target.resolve(jar + "-0.1.0.jar").toString()));
        } else {
            String classpath = target.resolve("classes") + File.pathSeparator + target.resolve("test-classes")
                    + File.pathSeparator + Files.readString(target.resolve("integration-classpath.txt")).strip();
            command.addAll(List.of("-cp", classpath, mainClass));
        }
        ProcessBuilder builder = new ProcessBuilder(command).directory(root.toFile()).redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(output.resolve(database + ".log").toFile()));
        Map<String, String> env = builder.environment();
        // Test child processes must not inherit a user's external Spring configuration.
        env.keySet().removeIf(key -> key.startsWith("SPRING_") || key.startsWith("TEST_DB_"));
        env.put("SPRING_PROFILES_ACTIVE", "integration");
        env.put("SERVER_ADDRESS", "127.0.0.1");
        env.put("PORT", Integer.toString(port));
        String jdbc = mode.equals("postgres") ? "jdbc:postgresql://" + postgres.getHost() + ":"
                + postgres.getMappedPort(5432) + "/" + database : "jdbc:h2:mem:flow_" + database + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        env.put("DB_URL", jdbc);
        env.put("DB_USER", mode.equals("postgres") ? database : "sa");
        env.put("DB_PASSWORD", mode.equals("postgres") ? password : "");
        env.put("REGISTRY_BASE_URL", "http://127.0.0.1:" + registryPort);
        env.put("ALERTS_BASE_URL", "http://127.0.0.1:" + alertsPort);
        if (database.equals("alerts")) {
            env.put("SPRING_CONFIG_ADDITIONAL_LOCATION", output.resolve("rules.yml").toUri().toString());
        }
        return builder.start();
    }

    private String createPoint(String code) throws Exception {
        Reply reply = request(registryPort, "POST", "/api/v1/monitoring-points",
                Map.of("code", code, "name", "Punto integración", "type", "ALMACENAMIENTO"));
        if (reply.status() != 201) {
            throw new IllegalStateException("No se creó fixture de punto: " + reply);
        }
        return reply.body().path("id").asText();
    }

    Reply telemetry(String method, String path, Object body) throws Exception {
        return request(telemetryPort, method, path, body);
    }

    Reply alerts(String method, String path, Object body) throws Exception {
        return request(alertsPort, method, path, body);
    }

    private Reply request(int port, String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        }
        var reply = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new Reply(reply.statusCode(), JSON.readTree(reply.body()));
    }

    void stopAlerts() {
        stop(alerts);
        alerts = null;
    }

    void startAlerts() throws Exception {
        if (alerts != null && alerts.isAlive()) {
            return;
        }
        alerts = launch("alert-management", "alert-management", "alerts", alertsPort,
                "pe.edu.upc.coldchain.alerts.AlertsApplication");
        awaitHealthy(alerts, alertsPort, "alerts");
    }

    private void awaitHealthy(Process process, int port, String name) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (process.isAlive() && System.nanoTime() < deadline) {
            try {
                Reply reply = request(port, "GET", "/actuator/health", null);
                if (reply.status() == 200 && reply.body().path("status").asText().equals("UP")) {
                    return;
                }
            } catch (java.io.IOException ignored) {
                // The owned process is still starting; readiness, not a fixed delay, gates scenarios.
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("Servicio " + name + " no saludable; revisar " + output.resolve(name + ".log"));
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
    }

    private static void stop(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void close() {
        stop(telemetry);
        stop(alerts);
        stop(registry);
        if (postgres != null && postgres.isRunning()) {
            postgres.close();
        }
    }

    record Reply(int status, JsonNode body) { }
}
