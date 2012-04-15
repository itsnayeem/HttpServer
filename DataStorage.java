import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import argo.jdom.JsonArrayNodeBuilder;


public class DataStorage {
	private static Logger log = Logger.getLogger(DataStorage.class);
	private static DataStorage instance;
	private final ConcurrentHashMap<String, TweetsStored> index;
	
	public static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	
	private DataStorage() {
		index = new ConcurrentHashMap<String, TweetsStored>();
	}
	
	public static DataStorage getInstance() {
		if (instance == null) {
			synchronized (DataStorage.class) {
				if (instance == null) {
					instance = new DataStorage();
				}
			}
		}
		return instance;
	}
	
	public boolean containsHash (String hash) {
		return index.containsKey(hash);
	}
	
	public int getVersion (String hash) {
		int retval = 0;
		TweetsStored tweet = null;
		if ((tweet = index.get(hash)) != null) {
			return tweet.getVersion();
		}
		return retval;
	}
	
	public boolean isLatest (String hash, int version) {
		return (getVersion(hash) == version);
	}

	public JsonArrayNodeBuilder getTweetsArrayBuilder(String hash) {
		JsonArrayNodeBuilder retval = index.get(hash).getTweetsArrayBuilder();
		return retval;
	}
	
	public void addTweet(String hash, String tweet) {
		log.info("Adding '"+tweet+"' to #"+hash);
		lock.writeLock().lock();
		TweetsStored tweetList = null;
		
		if ((tweetList = index.get(hash)) == null) {
			index.put(hash, new TweetsStored(tweet));
		} else {
			tweetList.addTweet(tweet);
		}
		lock.writeLock().unlock();
	}
	
	public void print() {
		for (String i : index.keySet()) {
			System.out.println(index.get(i).getTweets());
		}
	}
}
