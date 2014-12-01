package util.blob;

public class Tweet {

	private String tweet;
	private int numRetweets;
	private long date;
	private long id;
	
	public Tweet(String tweet, int retweets, long date, long id) {
		this.tweet = tweet;
		this.numRetweets = retweets;
		this.date = date;
		this.id = id;
	}

	public String getTweet() {
		return tweet;
	}

	public void setTweet(String tweet) {
		this.tweet = tweet;
	}

	public int getNumRetweets() {
		return numRetweets;
	}

	public void setNumRetweets(int numRetweets) {
		this.numRetweets = numRetweets;
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
    @Override
    public boolean equals(Object o) {
        if (o instanceof Tweet) {
            Tweet t = ((Tweet) o);
            return this.getTweet().equals(t.getTweet());
        }
        return false;
    }

    @Override
    public String toString() {
        return tweet;
    }
}
