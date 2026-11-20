// Java source-file launcher: java -cp <postgresql JDBC jar> deploy/DatabaseEvidence.java init|snapshot
// Connection settings come from the environment and are never printed.
import java.sql.*;
import java.util.*;

class DatabaseEvidence {
    public static void main(String[] args) throws Exception {
        String mode = args.length == 1 ? args[0] : "snapshot";
        String password = Objects.requireNonNull(System.getenv("EVIDENCE_DB_PASSWORD"));
        try (Connection c = DriverManager.getConnection(System.getenv("EVIDENCE_DB_URL"),
                System.getenv("EVIDENCE_DB_USER"), password); Statement s = c.createStatement()) {
            if (mode.equals("init")) {
                s.execute("CREATE ROLE bookstore_user LOGIN PASSWORD '" + password.replace("'", "''") + "'");
                s.execute("CREATE DATABASE bookstoredb OWNER bookstore_user");
                System.out.println("Created an isolated database and a non-superuser application role.");
            } else if (mode.equals("snapshot")) {
                try (ResultSet r = s.executeQuery("SELECT current_setting('server_version')")) {
                    r.next(); System.out.println("postgres_version=" + r.getString(1));
                }
                List<String> tables = new ArrayList<>();
                try (ResultSet r = s.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename")) {
                    while (r.next()) tables.add(r.getString(1));
                }
                if (tables.size() != 22) throw new IllegalStateException("Expected 22 application tables, got " + tables.size());
                for (String table : tables) {
                    String quoted = "\"" + table.replace("\"", "\"\"") + "\"";
                    String sql = "SELECT count(*), md5(coalesce(string_agg(row_to_json(t)::text, E'\\n' ORDER BY row_to_json(t)::text), '')) FROM public." + quoted + " t";
                    try (ResultSet r = s.executeQuery(sql)) {
                        r.next(); System.out.printf("%s rows=%d md5=%s%n", table, r.getLong(1), r.getString(2));
                    }
                }
                try (ResultSet r = s.executeQuery("SELECT NOT rolsuper FROM pg_roles WHERE rolname=current_user")) {
                    r.next(); if (!r.getBoolean(1)) throw new IllegalStateException("Application role must not be a superuser");
                }
                System.out.println("application_role_is_not_superuser=true");
                try (ResultSet r = s.executeQuery("SELECT count(*) > 0 AND bool_and(password_hash LIKE '$2%') FROM users")) {
                    r.next(); if (!r.getBoolean(1)) throw new IllegalStateException("Stored passwords must be BCrypt hashes");
                }
                System.out.println("stored_passwords_are_bcrypt=true");
            } else {
                throw new IllegalArgumentException("Use init or snapshot");
            }
        }
    }
}
