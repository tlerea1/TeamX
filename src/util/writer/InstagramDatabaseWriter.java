package util.writer;

import util.blob.InstagramAccount;

public class InstagramDatabaseWriter extends Thread {

    private InstagramAccount account_;

    public InstagramDatabaseWriter(InstagramAccount account) {
        account_ = account;

    }

    /**
     * executes the write to the database
     */
    public void run() {

    }

}
