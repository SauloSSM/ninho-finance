import java.nio.file.*;
import java.sql.*;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import br.com.saulossm.ninho.NinhoApplication;

/** Test-only entry point. Every path is supplied by the temporary test harness. */
class OperationsFixture {
    public static void main(String[] args) throws Exception {
        Path db = Path.of(args[1]);
        if (args[0].equals("assert-startup")) {
            if (!Files.isRegularFile(db)) throw new AssertionError("Banco esperado nao foi criado: " + db);
            try (var connection = DriverManager.getConnection("jdbc:sqlite:file:" + db.toUri().getRawPath() + "?mode=ro");
                 var stmt = connection.createStatement()) {
                try (var rows = stmt.executeQuery("PRAGMA database_list")) {
                    if (!rows.next() || !Files.isSameFile(db, Path.of(rows.getString("file"))))
                        throw new AssertionError("PRAGMA database_list diverge do arquivo esperado");
                    System.out.println("PRAGMA database_list: " + rows.getString("file"));
                }
                try (var query = connection.prepareStatement("SELECT count(*) FROM people WHERE name = ?")) {
                    query.setString(1, args[2]);
                    try (var rows = query.executeQuery()) {
                        if (!rows.next() || rows.getInt(1) != 1) throw new AssertionError("Marcador escrito pela API ausente");
                    }
                }
                try (var rows = stmt.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success = 1")) {
                    if (!rows.next() || rows.getInt(1) < 1) throw new AssertionError("Migrations ausentes");
                }
            }
            return;
        }
        if (args[0].equals("boot")) {
            System.setProperty("spring.devtools.restart.enabled", "false");
            try (var context = SpringApplication.run(NinhoApplication.class,
                    "--spring.datasource.url=jdbc:sqlite:" + db.toAbsolutePath(),
                    "--server.address=127.0.0.1", "--server.port=0")) {
                int port = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context)
                        .getWebServer().getPort();
                var uri = java.net.URI.create("http://127.0.0.1:" + port + "/api/people");
                try (var client = java.net.http.HttpClient.newHttpClient()) {
                    var get = java.net.http.HttpRequest.newBuilder(uri)
                            .header("Origin", "http://localhost:5173").GET().build();
                    var response = client.send(get, java.net.http.HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() != 200 || !response.headers()
                            .firstValue("Access-Control-Allow-Origin").orElse("").equals("http://localhost:5173"))
                        throw new AssertionError("GET da API ou CORS invalido");
                    var options = java.net.http.HttpRequest.newBuilder(uri)
                            .header("Origin", "http://localhost:5173")
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "content-type")
                            .method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody()).build();
                    var preflight = client.send(options, java.net.http.HttpResponse.BodyHandlers.discarding());
                    if (preflight.statusCode() != 200 || !preflight.headers()
                            .firstValue("Access-Control-Allow-Methods").orElse("").contains("POST"))
                        throw new AssertionError("Preflight CORS invalido");
                }
                Files.writeString(Path.of(args[2]), "ready");
                await(Path.of(args[3]));
            }
            return;
        }
        if (args[0].equals("init")) {
            Flyway.configure().dataSource("jdbc:sqlite:" + db, null, null)
                    .locations("filesystem:" + args[2]).load().migrate();
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + db);
             var stmt = connection.createStatement()) {
            switch (args[0]) {
                case "init" -> stmt.executeUpdate("INSERT INTO people(name,type) VALUES('before','USER')");
                case "change" -> stmt.executeUpdate("UPDATE people SET name='after'");
                case "assert" -> {
                    try (var rows = stmt.executeQuery("SELECT name FROM people ORDER BY id")) {
                        if (!rows.next() || !rows.getString(1).equals(args[2]) || rows.next())
                            throw new AssertionError("Dados restaurados incorretos");
                    }
                }
                case "incompatible" -> stmt.executeUpdate("ALTER TABLE people ADD COLUMN unexpected TEXT");
                case "foreign-key" -> {
                    stmt.executeUpdate("PRAGMA foreign_keys=OFF");
                    stmt.executeUpdate("INSERT INTO bank_accounts(owner_id,name,institution) "
                            + "VALUES(99999,'invalid','test')");
                }
                case "idle" -> {
                    stmt.executeQuery("SELECT count(*) FROM people").close();
                    Files.writeString(Path.of(args[2]), "ready");
                    await(Path.of(args[3]));
                }
                case "wal" -> {
                    stmt.execute("PRAGMA journal_mode=WAL");
                    stmt.execute("UPDATE people SET name='wal-committed'");
                    stmt.execute("BEGIN IMMEDIATE");
                    stmt.execute("UPDATE people SET name='uncommitted'");
                    Files.writeString(Path.of(args[2]), "ready");
                    await(Path.of(args[3]));
                    stmt.execute("ROLLBACK");
                }
                default -> throw new IllegalArgumentException(args[0]);
            }
        }
    }

    static void await(Path stop) throws Exception {
        long deadline = System.nanoTime() + 180_000_000_000L;
        while (!Files.exists(stop)) {
            if (System.nanoTime() > deadline) throw new IllegalStateException("Timeout aguardando teste");
            Thread.sleep(100);
        }
    }
}
