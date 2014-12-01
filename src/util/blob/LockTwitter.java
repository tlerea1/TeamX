package util.blob;

import java.util.concurrent.locks.ReentrantLock;

import twitter4j.Twitter;

public class LockTwitter implements Lockable {

	private ReentrantLock lock;
	private Twitter tw;
	
	public LockTwitter(Twitter t) {
		this.tw = t;
		this.lock = new ReentrantLock();
	}
	
	@Override
	public boolean isLocked() {
		return this.lock.isLocked();
	}

	@Override
	public void lock() {
		this.lock.lock();
	}

	@Override
	public void unlock() {
		this.lock.unlock();
	}

	public Twitter getTwitter() {
		this.lock.lock();
		return this.tw;
	}
}
