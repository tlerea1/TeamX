package util.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import util.blob.Link;
import util.blob.Tweet;
import util.blob.TwitterAccount;

/**
 * Wrapper around the MySQL JAVA JDBC for interaction with the Twitter DB
 * 
 * @author tuvialerea
 * 
 */
public class TwitterDB {

    /**
     * TwitterDB Exception class
     * 
     * @author tuvialerea
     * 
     */
    private static class TwitterDBException extends RuntimeException {
        /**
		 * 
		 */
        private static final long serialVersionUID = 1L;

        public TwitterDBException(String msg) {
            super(msg);
        }
    }

    private TwitterDB() {
    }

    private static boolean inited = false;

    /**
     * Init function, must be run before all other functions in the class.
     * Creates users table if it does not exist
     * 
     * @throws SQLException
     *             when MySQL connection fails
     */
    public static void init() throws SQLException {
        if (inited) {
            return;
        }
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        inited = true;
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
                + "username TEXT, dateCreated BIGINT(64), numFollowers INT(32), numFollowing INT(32), "
                + "modified BIGINT(64), twitterID BIGINT(64), bot BOOLEAN DEFAULT FALSE)");
        stmt.executeUpdate("CREATE TABLE means (id INT(16) UNSIGNED AUTO_INCREMENT PRIMARY KEY, "
                + "name TEXT, internalSimilarity FLOAT, linkAverage FLOAT)");
        result.close();
        stmt.close();
        conn.close();
    }

    /**
     * Function to make a connection to the database
     * 
     * @return the connection object
     * @throws SQLException
     *             if a connection fails
     */
    private static Connection makeConnection() throws SQLException {
        if (!inited) {
            throw new TwitterDBException("Must init DB module");
        }
        Connection dbConn = DriverManager
                .getConnection("jdbc:mysql://localhost/twitter?"
                        + "user=root&password=teamx4lyfe");
        return dbConn;
    }

    /**
     * Adds user to the database. This addition process involves adding to the
     * users table, and making tables for tweets, links, followers, and
     * followings. If user already exists, nothing is added.
     * 
     * @param account
     *            the account to add.
     * @throws SQLException
     *             if connection fails.
     */
    public static void addUser(TwitterAccount account) throws SQLException {
        String username = account.getName();
        if (account.getName().equals("users")) {
            throw new TwitterDBException("Cannont add user with username users");
        }
        if (hasUser(username)) {
            return;
        }
        Connection conn = makeConnection();
        // Insert into users table
        PreparedStatement insertStmt = conn
                .prepareStatement("INSERT INTO users "
                        + "(username,dateCreated,numFollowers,numFollowing,modified,twitterID) "
                        + "VALUES(?,?,?,?,?,?)");
        insertStmt.setString(1, username);
        insertStmt.setLong(2, System.nanoTime());
        insertStmt.setInt(3, account.getNumFollowers());
        insertStmt.setInt(4, account.getNumFollowing());
        insertStmt.setLong(5, System.nanoTime());
        insertStmt.setLong(6, account.getTwitterId());
        insertStmt.executeUpdate();
        Statement stmt = conn.createStatement();
        // Create sub tables
        stmt.executeUpdate("CREATE TABLE "
                + username
                + "_tweets "
                + "(id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
                + " tweet TEXT, numRetweets INT(16), date BIGINT(64), tweetId BIGINT(64))");
        stmt.executeUpdate("CREATE TABLE " + username + "_links "
                + "(id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
                + " link TEXT, date BIGINT(64))");
        stmt.executeUpdate("CREATE TABLE " + username + "_followers "
                + "(id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
                + " username TEXT, userID BIGINT(64), date BIGINT(64))");
        stmt.executeUpdate("CREATE TABLE " + username + "_following "
                + "(id INT(6) UNSIGNED AUTO_INCREMENT PRIMARY KEY,"
                + " username TEXT, userID BIGINT(64), date BIGINT(64))");
        insertStmt.close();
        stmt.close();
        conn.close();
    }

    /**
     * Update the given user, if the user doesnt yet exist nothing happens.
     * 
     * @param account
     *            the account ot update with this info.
     * @throws SQLException
     *             if connection fails.
     */
    public static void updateUser(TwitterAccount account) throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("UPDATE users SET numFollowers=?,numFollowing=?,modified=? WHERE username=?");
        stmt.setInt(1, account.getNumFollowers());
        stmt.setInt(2, account.getNumFollowing());
        stmt.setLong(3, System.nanoTime());
        stmt.setString(4, account.getName());
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Checks to see if the database contains the given user.
     * 
     * @param username
     *            the username to look for
     * @return true if the database contains the user
     * @throws SQLException
     *             if connection fails.
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
     * Deletes all information about the user.
     * 
     * @param username
     *            the username to delete
     * @throws SQLException
     *             if connection fails.
     */
    public static void dropUser(String username) throws SQLException {
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        PreparedStatement st = conn
                .prepareStatement("DELETE FROM users WHERE username=?");
        st.setString(1, username);
        st.executeUpdate();
        stmt.executeUpdate("DROP TABLE " + username + "_tweets");
        stmt.executeUpdate("DROP TABLE " + username + "_links");
        stmt.executeUpdate("DROP TABLE " + username + "_followers");
        stmt.executeUpdate("DROP TABLE " + username + "_following");
        stmt.close();
        st.close();
        conn.close();
    }

    /**
     * Marks the given username as a definite bot. If user is not in the
     * database nothing happens
     * 
     * @param username
     *            the username to make a bot.
     * @throws SQLException
     *             if connection fails.
     */
    public static void makeBot(String username) throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("UPDATE users SET bot=? WHERE username=?");
        stmt.setBoolean(1, true);
        stmt.setString(2, username);
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Function to tell if the given username is definitely a bot.
     * 
     * @param username
     *            the username to check
     * @return true if the username is marked as a bot
     * @throws SQLException
     *             if connection fails
     */
    public static boolean isBot(String username) throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("SELECT * FROM users WHERE username=?");
        stmt.setString(1, username);
        ResultSet result = stmt.executeQuery();
        boolean toReturn = false;
        while (result.next()) {
            toReturn = result.getBoolean("bot");
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }

    /**
     * Add the given link to the given user in the database.
     * 
     * @param username
     *            the username to add the link to.
     * @param link
     *            the link to add to the user
     * @throws SQLException
     *             fails to connect
     */
    public static void addLink(String username, Link link) throws SQLException {
        if (hasLink(username, link)) {
            return;
        }
        Connection conn = makeConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO "
                + username + "_links (link,date) VALUES(?,?)");
        stmt.setString(1, link.getLink());
        stmt.setLong(2, link.getTime());
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Add the given tweet to the given user in the database.
     * 
     * @param username
     *            the username to add the tweet to.
     * @param tweet
     *            the tweet to add to the user.
     * @throws SQLException
     *             if fails to connect
     */
    public static void addTweet(String username, Tweet tweet)
            throws SQLException {
        if (hasTweet(username, tweet)) {
            return;
        }
        Connection conn = makeConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO "
                + username
                + "_tweets (tweet,numRetweets,date,tweetId) VALUES(?,?,?,?)");
        stmt.setString(1, tweet.getTweet());
        stmt.setInt(2, tweet.getNumRetweets());
        stmt.setLong(3, tweet.getDate());
        stmt.setLong(4, tweet.getId());
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Add the given TwitterAccount as a follower of the given username.
     * 
     * @param username
     *            the username to add the follower to.
     * @param p
     *            the follower to add to the user.
     * @throws SQLException
     *             if fails to connect.
     */
    public static void addFollower(String username, TwitterAccount p)
            throws SQLException {
        if (hasFollower(username, p.getName())) {
            return;
        }
        Connection conn = makeConnection();
        PreparedStatement stmt = conn.prepareStatement("INSERT INTO "
                + username + "_followers (username,userID,date) VALUES(?,?,?)");
        stmt.setString(1, p.getName());
        stmt.setLong(2, p.getTwitterId());
        stmt.setLong(3, System.nanoTime());
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Add the given TwitterAccount as a friend of the given username.
     * 
     * @param username
     *            the username to add a friend to.
     * @param p
     *            the friend to add to the user.
     * @throws SQLException
     *             if fails to connect.
     */
    public static void addFollowing(String username, TwitterAccount p)
            throws SQLException {
        if (hasFollowing(username, p.getName())) {
            return;
        }
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("INSERT INTO " + username
                        + "_following (username,userID,date) VALUES (?,?,?)");
        stmt.setString(1, p.getName());
        stmt.setLong(2, p.getTwitterId());
        stmt.setLong(3, System.nanoTime());
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Checks if the given username is followed by the given follower
     * 
     * @param username
     *            the username
     * @param follower
     *            the follower
     * @return true of the follower follows the username
     * @throws SQLException
     *             if fails to connect
     */
    public static boolean hasFollower(String username, String follower)
            throws SQLException {
        Collection<String> followers = getFollowers(username);
        return followers.contains(follower);
    }

    /**
     * Checks if the given username is following the given following name.
     * 
     * @param username
     *            the username
     * @param following
     *            the friend
     * @return true if username is following following
     * @throws SQLException
     *             if fails to connect
     */
    public static boolean hasFollowing(String username, String following)
            throws SQLException {
        Collection<String> followings = getFollowing(username);
        return followings.contains(following);
    }

    /**
     * Checks if the given user has the given tweet.
     * 
     * @param username
     *            the username
     * @param t
     *            the tweet
     * @return true if the username has the given tweet.
     * @throws SQLException
     *             if fails to connect.
     */
    public static boolean hasTweet(String username, Tweet t)
            throws SQLException {
        Collection<Tweet> tweets = getTweets(username);
        return tweets.contains(t);
    }

    /**
     * Checks if the given user has the given link.
     * 
     * @param username
     *            the given user
     * @param l
     *            the given link
     * @return true if the username has the given link
     * @throws SQLException
     *             if fials to connect.
     */
    public static boolean hasLink(String username, Link l) throws SQLException {
        Collection<Link> links = getLinks(username);
        return links.contains(l);
    }

    /**
     * Gets the list of followers for the given username.
     * 
     * @param username
     *            the given username.
     * @return a collectino of username of followers
     * @throws SQLException
     *             if fails to connect.
     */
    public static Collection<String> getFollowers(String username)
            throws SQLException {
        Collection<String> toReturn = new ArrayList<String>();
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery("SELECT * FROM " + username
                + "_followers");
        while (result.next()) {
            toReturn.add(result.getString("username"));
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }

    /**
     * Gets the list of friends for the given username.
     * 
     * @param username
     *            the given username.
     * @return a collection of the usernames of the friends of username
     * @throws SQLException
     *             if fails to connect.
     */
    public static Collection<String> getFollowing(String username)
            throws SQLException {
        Collection<String> toReturn = new ArrayList<String>();
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery("SELECT * FROM " + username
                + "_following");
        while (result.next()) {
            toReturn.add(result.getString("username"));
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }

    /**
     * Gets all the tweets of the given username.
     * 
     * @param username
     *            the given username.
     * @return a collection of tweets.
     * @throws SQLException
     *             if fails to connect.
     */
    public static Collection<Tweet> getTweets(String username)
            throws SQLException {
        Collection<Tweet> toReturn = new ArrayList<Tweet>();
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery("SELECT * FROM " + username
                + "_tweets");
        while (result.next()) {
            toReturn.add(new Tweet(result.getString("tweet"), result
                    .getInt("numRetweets"), result.getLong("date"), result
                    .getLong("tweetId")));
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }

    /**
     * Gets all the links of the given username.
     * 
     * @param username
     *            the given username.
     * @return a collection of links.
     * @throws SQLException
     *             if fails to connect.
     */
    public static Collection<Link> getLinks(String username)
            throws SQLException {
        Collection<Link> toReturn = new ArrayList<Link>();
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery("SELECT * FROM " + username
                + "_links");
        while (result.next()) {
            toReturn.add(new Link(result.getString("link"), result
                    .getLong("date")));
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }

    /**
     * Gets all the users from the user table.
     * 
     * @return a collection of usernames in the users table.
     * @throws SQLException
     *             if fails to connect.
     */
    public static Collection<String> getUsers() throws SQLException {
        Collection<String> toReturn = new ArrayList<String>();
        Connection conn = makeConnection();
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery("SELECT username FROM users");
        while (result.next()) {
            toReturn.add(result.getString("username"));
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }

    /**
     * Updates the value for the bot mean in the table
     * 
     * @param mean
     */
    public static void updateBotMean(Map<String, Double> mean)
            throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("UPDATE means SET internalSimilarity=? linkAverage=? WHERE name=?");
        stmt.setDouble(1, mean.get("internalSimilarity"));
        stmt.setDouble(2, mean.get("linkAverage"));
        stmt.setString(3, "TWITTER_BOT");
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Gets the mean of the bot data from the database Mean is updated
     * dynamically by the DBCrawler
     * 
     * @return vector representation of the mean of the bot data
     */
    public static double[] getBotMean() throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("SELECT * FROM means WHERE name=?");
        stmt.setString(1, "TWITTER_BOT");
        ResultSet result = stmt.executeQuery();
        double[] toReturn = new double[2];
        if (result.next()) {
            toReturn[0] = result.getFloat("internalSimilarity");
            toReturn[1] = result.getFloat("linkAverage");
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }

    public static void updateHumanMean(Map<String, Double> mean)
            throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("UPDATE means SET internalSimilarity=? WHERE name=?");
        stmt.setDouble(1, mean.get("internalSimilarity"));
        stmt.setDouble(2, mean.get("linkAverage"));
        stmt.setString(3, "TWITTER_BOT");
        stmt.executeUpdate();
        stmt.close();
        conn.close();
    }

    /**
     * Gets the mean of the human data from the database Mean is updated
     * dynamically by the DBCrawler
     * 
     * @return vector representation of the mean of the human data
     * @throws SQLException
     */
    public static double[] getHumanMean() throws SQLException {
        Connection conn = makeConnection();
        PreparedStatement stmt = conn
                .prepareStatement("SELECT * FROM means WHERE name=?");
        stmt.setString(1, "TWITTER_HUMAN");
        ResultSet result = stmt.executeQuery();
        double[] toReturn = new double[2];
        if (result.next()) {
            toReturn[0] = result.getFloat("internalSimilarity");
            toReturn[1] = result.getFloat("linkAverage");
        }
        result.close();
        stmt.close();
        conn.close();
        return toReturn;
    }
}
