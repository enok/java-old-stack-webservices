package com.campusconnect.sis.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * SMELL #6: a hand-rolled connection factory.
 *
 * There is no pool. Every DAO call opens a brand new MySQL connection through
 * DriverManager and closes it in a finally block (sometimes). Under the
 * nightly roster export this opens tens of thousands of connections and is the
 * reason max_connections was raised to 500 on the database server.
 *
 * TODO: replace with a real pool (or, after modernization, a Spring Boot
 * DataSource + HikariCP). Do NOT do this before the characterization tests
 * exist: several DAO methods depend on each call seeing its own transaction.
 */
public final class ConnectionFactory {

    private static final Logger LOG = Logger.getLogger(ConnectionFactory.class);

    private static final String PROPS = "/db.properties";

    private static String url;
    private static String user;
    private static String password;
    private static boolean initialised = false;

    private ConnectionFactory() {
    }

    /**
     * XXX: lazy static init with no synchronisation. Two threads racing here on
     * a cold JVM can both run the initialiser. It is idempotent so nobody ever
     * fixed it.
     */
    private static void init() {
        if (initialised) {
            return;
        }
        InputStream in = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            in = ConnectionFactory.class.getResourceAsStream(PROPS);
            Properties p = new Properties();
            if (in != null) {
                p.load(in);
            }
            url = p.getProperty("sis.db.url", "jdbc:mysql://localhost:3306/campusconnect?useSSL=false");
            user = p.getProperty("sis.db.user", "sisapp");
            password = p.getProperty("sis.db.password", "CHANGEME-not-a-real-password");
            initialised = true;
            LOG.info("ConnectionFactory initialised for " + url);
        } catch (Exception e) {
            // SMELL #4: catch(Exception) + printStackTrace.
            e.printStackTrace();
            throw new DataAccessException("could not initialise ConnectionFactory", e);
        } finally {
            closeQuietly(in);
        }
    }

    public static Connection getConnection() {
        init();
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
            throw new DataAccessException("could not open a connection to " + url, sqle);
        }
    }

    public static void closeQuietly(Object closeable) {
        if (closeable == null) {
            return;
        }
        try {
            if (closeable instanceof java.sql.ResultSet) {
                ((java.sql.ResultSet) closeable).close();
            } else if (closeable instanceof java.sql.Statement) {
                ((java.sql.Statement) closeable).close();
            } else if (closeable instanceof Connection) {
                ((Connection) closeable).close();
            } else if (closeable instanceof java.io.Closeable) {
                ((java.io.Closeable) closeable).close();
            }
        } catch (Exception e) {
            // TODO: at least log it.
            e.printStackTrace();
        }
    }
}
