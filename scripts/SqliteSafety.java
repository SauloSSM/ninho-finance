import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.zip.CRC32;
import org.sqlite.SQLiteConnection;

/** Operational tool only; uses the application's existing SQLite driver. */
class SqliteSafety {
    public static void main(String[] args) {
        try {
            switch (args[0]) {
                case "backup" -> backup(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]));
                case "validate" -> validate(Path.of(args[1]), Path.of(args[2]));
                default -> throw new IllegalArgumentException("Comando desconhecido.");
            }
        } catch (Exception e) {
            System.err.println("ERRO SQLite: " + e.getMessage());
            System.exit(1);
        }
    }

    static Connection open(Path file, boolean readOnly) throws Exception {
        if (!Files.isRegularFile(file) || Files.size(file) == 0)
            throw new IllegalArgumentException("Banco ausente ou vazio: " + file);
        return DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath().toUri()
                + (readOnly ? "?mode=ro" : "?mode=rw"));
    }

    static void backup(Path source, Path target, Path migrations) throws Exception {
        // CREATE_NEW reserves the name. Never replace an existing backup.
        Files.createFile(target);
        boolean complete = false;
        try {
            try (var db = open(source, true)) {
                db.createStatement().execute("PRAGMA busy_timeout=5000");
                int result = ((SQLiteConnection) db).getDatabase()
                        .backup("main", target.toAbsolutePath().toString(), null, 100, 50, 100);
                if (result != 0) throw new SQLException("Backup online falhou, codigo " + result);
            }
            // Make the snapshot self-contained, including when the source uses WAL.
            try (var db = open(target, false); var stmt = db.createStatement()) {
                stmt.execute("PRAGMA journal_mode=DELETE");
            }
            validate(target, migrations);
            complete = true;
            System.out.println("Backup validado: " + target.toAbsolutePath());
        } finally {
            if (!complete) Files.deleteIfExists(target);
        }
    }

    static List<Path> migrations(Path directory) throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(p -> p.getFileName().toString().matches("V[0-9]+__.*\\.sql"))
                    .sorted(Comparator.comparingInt(p -> Integer.parseInt(
                            p.getFileName().toString().split("__")[0].substring(1)))) .toList();
        }
    }

    static int checksum(Path migration) throws Exception {
        CRC32 crc = new CRC32();
        boolean first = true;
        for (String line : Files.readAllLines(migration, StandardCharsets.UTF_8)) {
            if (first && line.startsWith("\uFEFF")) line = line.substring(1);
            first = false;
            crc.update(line.getBytes(StandardCharsets.UTF_8));
        }
        return (int) crc.getValue();
    }

    static Map<String, String> schema(Connection db) throws Exception {
        Map<String, String> result = new TreeMap<>();
        try (var stmt = db.createStatement(); var rows = stmt.executeQuery(
                "SELECT type, name, sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' "
                + "AND name NOT LIKE '%flyway_schema_history%' AND sql IS NOT NULL")) {
            while (rows.next()) result.put(rows.getString(1) + ":" + rows.getString(2),
                    rows.getString(3).replaceAll("\\s+", " ").trim());
        }
        return result;
    }

    static void validate(Path file, Path directory) throws Exception {
        for (String suffix : List.of("-wal", "-shm", "-journal"))
            if (Files.exists(Path.of(file + suffix)))
                throw new IllegalArgumentException("Backup deve ser independente, sem arquivo " + suffix);
        var migrations = migrations(directory);
        if (migrations.isEmpty()) throw new IllegalArgumentException("Migrations nao encontradas.");
        try (var db = open(file, true); var stmt = db.createStatement()) {
            try (var rows = stmt.executeQuery("PRAGMA integrity_check")) {
                if (!rows.next() || !"ok".equals(rows.getString(1)) || rows.next())
                    throw new SQLException("Integridade do SQLite invalida.");
            }
            try (var rows = stmt.executeQuery("PRAGMA foreign_key_check")) {
                if (rows.next()) throw new SQLException("Violacao de foreign key no backup.");
            }
            try (var rows = stmt.executeQuery("SELECT version, script, checksum, success "
                    + "FROM flyway_schema_history ORDER BY installed_rank")) {
                for (Path migration : migrations) {
                    String name = migration.getFileName().toString();
                    String version = name.split("__")[0].substring(1);
                    if (!rows.next() || !version.equals(rows.getString(1))
                            || !name.equals(rows.getString(2)) || rows.getInt(3) != checksum(migration)
                            || rows.getInt(4) != 1)
                        throw new SQLException("Historico Flyway incompativel com esta versao do Ninho.");
                }
                if (rows.next()) throw new SQLException("Backup pertence a outra versao do Ninho.");
            }
            try (var expected = DriverManager.getConnection("jdbc:sqlite::memory:")) {
                for (Path migration : migrations) {
                    int result = ((SQLiteConnection) expected).getDatabase()
                            ._exec(Files.readString(migration, StandardCharsets.UTF_8));
                    if (result != 0) throw new SQLException("Falha ao conferir schema de referencia.");
                }
                if (!schema(db).equals(schema(expected)))
                    throw new SQLException("Estrutura do backup incompativel com as migrations atuais.");
            }
        }
        System.out.println("Integridade, foreign keys e compatibilidade OK: " + file.toAbsolutePath());
    }
}
