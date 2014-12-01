package util.writer;

import java.sql.SQLException;
import java.util.Collection;

import util.blob.Link;
import util.blob.Tweet;
import util.blob.TwitterAccount;
import util.db.TwitterDB;

public class TwitterDatabaseWriter extends Thread {

    private TwitterAccount account_;

    public TwitterDatabaseWriter(TwitterAccount account) {
        account_ = account;
    }

    /**
     * executes the write to the database
     */
    public void run() {
        try {
            TwitterDB.addUser(account_);
            writeTweets();
            writeLinks();
            writeFollowers();
            writeFollowings();
            writeIsBot();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void writeFollowings() throws SQLException {
        Collection<TwitterAccount> tas = account_.getFollowing();
        for (TwitterAccount ta : tas) {
            TwitterDB.addFollowing(account_.getName(), ta);
        }
    }

    private void writeFollowers() throws SQLException {
        Collection<TwitterAccount> tas = account_.getFollowers();
        for (TwitterAccount ta : tas) {
            TwitterDB.addFollower(account_.getName(), ta);
        }
    }

    private void writeLinks() throws SQLException {
        Collection<Link> links = account_.extractLinks();
        for (Link l : links) {
            TwitterDB.addLink(account_.getName(), l);
        }
    }

    private void writeTweets() throws SQLException {
        Collection<Tweet> tweets = account_.getTweets();
        for (Tweet t : tweets) {
            TwitterDB.addTweet(account_.getName(), t);
        }
    }

    private void writeIsBot() throws SQLException {
        if (account_.getIsBot()) {
            TwitterDB.makeBot(account_.getName());
        }
    }

}
