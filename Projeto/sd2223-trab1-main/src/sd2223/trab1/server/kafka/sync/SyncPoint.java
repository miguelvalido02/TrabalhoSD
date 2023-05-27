package sd2223.trab1.server.kafka.sync;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint<T> {
	private static SyncPoint<?> instance;

	@SuppressWarnings("unchecked")
	synchronized public static <T> SyncPoint<T> getInstance() {
		if (instance == null)
			instance = new SyncPoint<>();
		return (SyncPoint<T>) instance;
	}

	private long version = -1L;
	private Map<Long, T> results;

	public SyncPoint() {
		results = new ConcurrentHashMap<>();
	}

	/**
	 * Waits for version to be at least equals to n
	 */
	public void waitForVersion(long n, int waitPeriod) {
		while (version < n) {
			try {
				this.wait(waitPeriod);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * Assuming that results are added sequentially, returns null if the result is
	 * not available.
	 */
	public T waitForResult(long n) {
		waitForVersion(n, Integer.MAX_VALUE);
		return results.remove(n);
	}

	/**
	 * Updates the version and stores the associated result
	 */
	public synchronized void setResult(long n, T result) {
		results.put(n, result);
		version = n;
		System.out.println("set result: " + results.get(n));
		this.notifyAll();
	}

	/**
	 * Updates the version
	 */
	public synchronized void setVersion(long n) {
		version = n;
		this.notifyAll();
	}

	public synchronized long getVersion() {
		return this.version;
	}

	public String toString() {
		return results.keySet().toString();
	}
}
