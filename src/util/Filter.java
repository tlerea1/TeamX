package util;

import java.sql.SQLException;

import net.sf.javaml.utils.ArrayUtils;
import util.blob.FacebookAccount;
import util.blob.InstagramAccount;
import util.blob.SocialMediaAccount;
import util.blob.TwitterAccount;
import util.db.TwitterDB;
import util.writer.FacebookDatabaseWriter;
import util.writer.InstagramDatabaseWriter;
import util.writer.TwitterDatabaseWriter;

public class Filter {

    public boolean isInitialized_ = false;
    private double[] TwitterBotMean_;
    private double[] TwitterHumanMean_;

    public static Filter FILTER_SINGLETON = new Filter();

    // TODO someone has to refresh the filter
    // We can refresh the weights of the different metrics or we can threshold
    // the different metrics and then change the thresholds
    public static void updateSingletonFilter(Filter f) {
        f.isInitialized_ = true;

    }

    public Filter() {
        try {
            TwitterBotMean_ = TwitterDB.getBotMean();
            TwitterHumanMean_ = TwitterDB.getHumanMean();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks to see if the given account is a BOT according to its internal
     * criteria. If we decide it is a bot, we write to the database and then
     * return true. else, return false
     * 
     * @param account
     * @return true iff the filter thinks the account is a bot
     */
    public boolean isBot(SocialMediaAccount account) {

        if (isInitialized_ == false) {
            updateSingletonFilter(this);
        }

        boolean evaluationResult = false;
        if (account instanceof TwitterAccount) {
            evaluationResult = evaluateAccount((TwitterAccount) account);
        } else if (account instanceof FacebookAccount) {
            evaluationResult = evaluateAccount((FacebookAccount) account);
        } else if (account instanceof InstagramAccount) {
            evaluationResult = evaluateAccount((InstagramAccount) account);
        }

        return evaluationResult;
    }

    /**
     * Evaluates the account to check if it is a bot.
     * 
     * @param account
     *            Account to be checked
     * @return true iff the filter thinks the account is a bot
     */
    private boolean evaluateAccount(TwitterAccount account) {
        try {
            TwitterBotMean_ = TwitterDB.getBotMean();
            TwitterHumanMean_ = TwitterDB.getHumanMean();
        } catch (SQLException e) {
            // If we fail to update, its no big deal
            e.printStackTrace();
        }

        double BotDistsance = ArrayUtils.norm(ArrayUtils.substract(
                account.getMetircs(), TwitterBotMean_));
        double HumanDistsance = ArrayUtils.norm(ArrayUtils.substract(
                account.getMetircs(), TwitterHumanMean_));

        if (BotDistsance > HumanDistsance) {
            account.setIsBot(true);
            executeWrite(account);
            return true;
        } else {
            executeWrite(account); // Prevents Feedback loop from going unstable
                                   // Makes sure we have plenty of datapoints
                                   // for the human dataset
            return false;
        }
    }

    /**
     * Evaluates the account to check if it is a bot.
     * 
     * @param account
     *            Account to be checked
     * @return true iff the filter thinks the account is a bot
     */
    private boolean evaluateAccount(FacebookAccount account) {
        executeWrite(account);
        return true;
    }

    /**
     * Evaluates the account to check if it is a bot.
     * 
     * @param account
     *            Account to be checked
     * @return true iff the filter thinks the account is a bot
     */
    private boolean evaluateAccount(InstagramAccount account) {
        executeWrite(account);
        return true;
    }

    /**
     * Generates a writer thread to write data to the database. The write
     * happens asynchronously
     * 
     * @param account
     *            data blob that will be writen to the database
     */
    private void executeWrite(TwitterAccount account) {
        TwitterDatabaseWriter wr = new TwitterDatabaseWriter(
                (TwitterAccount) account);
        wr.run();
    }

    /**
     * Generates a writer thread to write data to the database. The write
     * happens asynchronously
     * 
     * @param account
     *            data blob that will be writen to the database
     */
    private void executeWrite(FacebookAccount account) {
        FacebookDatabaseWriter wr = new FacebookDatabaseWriter(
                (FacebookAccount) account);
        wr.run();
    }

    /**
     * Generates a writer thread to write data to the database. The write
     * happens asynchronously
     * 
     * @param account
     *            data blob that will be writen to the database
     */
    private void executeWrite(InstagramAccount account) {
        InstagramDatabaseWriter wr = new InstagramDatabaseWriter(
                (InstagramAccount) account);
        wr.run();
    }
}
