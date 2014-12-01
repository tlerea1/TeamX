package util.writer;

import util.blob.FacebookAccount;

public class FacebookDatabaseWriter extends Thread {

    private FacebookAccount account_;

    public FacebookDatabaseWriter(FacebookAccount account) {
        account_ = account;
    }

    /**
     * executes the write to the database
     */
    public void run() {

    }

}
