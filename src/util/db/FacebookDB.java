package util.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import util.blob.FacebookAccount;
import facebook4j.Post;

/**
 * Wrapper around the MySQL interaction for the Facebook Database
 * 
 * @author Gabe
 * 
 */
public class FacebookDB {

    private static class FacebookDBException extends RuntimeException {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public FacebookDBException(String msg) {
            super(msg);
        }
    }

    private FacebookDB() {
    }

    private static boolean inited_ = false;

    public static void init() throws SQLException {
        if (inited_) {
            return;
        }
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        // Check if users table exists
        ResultSet result = conn.getMetaData().getTables(null, null, null,
                new String[] { "TABLE" });
        while (result.next()) {
            if (result.getString("TABLE_NAME").equals("users")) {
                return;
            }
        }
        // if not create it
        stmt.executeUpdate("CREATE TABLE users (id INT(16) UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
                + "username TEXT, dateCreated BIGINT(64), numFriends INT(32), "
                + "facebookID TEXT, bot BOOLEAN DEFAULT FALSE)");
        // stmt.executeUpdate("CREATE TABLE means (id INT(16) UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
        // + "name TEXT)");
        result.close();
        stmt.close();
        conn.close();
        inited_ = true;

    }

    /**
     * Function to make a connection to the database
     * 
     * @return the connection object
     * @throws SQLException
     *             if a connection fails
     */
    private static Connection makeConnection() throws SQLException {
        if (!inited_) {
            throw new FacebookDBException("Must init DB module");
        }
        Connection dbConn = DriverManager
                .getConnection("jdbc:mysql://localhost/facebook?"
                        + "user=root&password=teamx4lyfe");
        return dbConn;
    }

    /**
     * Checks to see if the given user already exists in the database
     * 
     * @param username
     *            username of the account in question
     * @return true iff there is an entry in the users table with the given
     *         account name
     * @throws SQLException
     */
    public static boolean hasUser(String username) throws SQLException {
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery("SELECT username FROM users");
        while (result.next()) {
            if (result.getString("username").equals(username)) {
                result.close();
                stmt.close();
                conn.close();
                return true;
            }
        }
        result.close();
        stmt.close();
        conn.close();
        return false;
    }

    /**
     * Adds a new user to the database. Creates the tables for accompanying data
     * 
     * @param account
     *            facebookAccount blob to generate the tables
     * @throws SQLException
     */
    public static void addUser(FacebookAccount account) throws SQLException {
        String username = account.getName();
        if (account.getName().equals("users")) {
            throw new FacebookDBException(
                    "Cannont add user with username users");
        }
        if (hasUser(username)) {
            return;
        }
        Connection conn = makeConnection();
        // Insert into users table
        PreparedStatement insertStmt = conn
                .prepareStatement("INSERT INTO users "
                        + "(username,dateCreated,numFriends,facebookID) "
                        + "VALUES(?,?,?,?,?)");
        insertStmt.setString(1, username);
        insertStmt.setLong(2, System.nanoTime());
        insertStmt.setInt(3, account.getNumFriends());
        insertStmt.setString(4, account.getId());
        insertStmt.executeUpdate();
        Statement stmt = conn.createStatement();
        // Create sub tables
        stmt.executeUpdate("CREATE TABLE " + username + "_statuses "
                + "(id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
                + " status TEXT, likes INT(16), date DATE, statusID TEXT)");
        stmt.executeUpdate("CREATE TABLE " + username + "_links "
                + "(id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
                + " link TEXT, date DATE)");
        stmt.executeUpdate("CREATE TABLE " + username + "_friends "
                + "(id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
                + " username TEXT, userID BIGINT(64), date DATE)");
        insertStmt.close();
        stmt.close();
        conn.close();
    }

    public static void addStatus(String username, Post status)
            throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO "
                + username
                + "_statuses (status, likes, date, statusId) VALUES(?,?,?,?");
        stmt.setString(1, status.getMessage());
        stmt.setInt(2, status.getLikes().size());
        // stmt.setDate(3, new java.sql.Date(status.getCreatedTime()));
        stmt.setString(4, status.getId());
    }
}
