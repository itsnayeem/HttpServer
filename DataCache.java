import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;


public class DataCache {
	private static Logger log = Logger.getLogger(DataCache.class);
	private static DataCache instance;
	private final ConcurrentHashMap<String, TweetsCached> index;

	public static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	private DataCache() {
		index = new ConcurrentHashMap<String,TweetsCached>();
	}
	
	public static DataCache getInstance() {
		if (instance == null) {
			synchronized (DataCache.class) {
				if (instance == null) {
					instance = new DataCache();
				}
			}
		}
		return instance;
	}
	
	public boolean containsHash (String hash) {
		return index.containsKey(hash);
	}
	
	public int getVersion (String hash) {
		TweetsCached tweet = null;
		if ((tweet = index.get(hash)) != null) {
			return tweet.getVersion();
		}
		return 0;
	}
	
	public String getTweet (String hash) {
		return index.get(hash).getContent();
	}
	
	public void addTweet(String hash, int version, String tweet) {
		log.info("Adding version "+version+" of '"+tweet+" to #"+hash);
		lock.writeLock().lock();
		index.put(hash, new TweetsCached(version, tweet));
		lock.writeLock().unlock();
	}
}
