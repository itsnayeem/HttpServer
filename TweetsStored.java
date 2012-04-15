import static argo.jdom.JsonNodeBuilders.aStringBuilder;
import static argo.jdom.JsonNodeBuilders.anArrayBuilder;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import argo.jdom.JsonArrayNodeBuilder;

public class TweetsStored {
	AtomicInteger version;
	CopyOnWriteArrayList<String> tweets;

	public TweetsStored(String tweet) {
		version = new AtomicInteger();
		tweets = new CopyOnWriteArrayList<String>();

		version.incrementAndGet();
		tweets.add(tweet);
	}

	public int getVersion() {
		return version.intValue();
	}
	
	public String getTweets () {
		return tweets.toString();
	}

	public JsonArrayNodeBuilder getTweetsArrayBuilder() {
		JsonArrayNodeBuilder abuild = anArrayBuilder();

		for (String tweet : tweets) {
			abuild.withElement(aStringBuilder(tweet));
		}

		return abuild;
	}
	
	public void addTweet(String tweet) {
		tweets.add(tweet);
		version.incrementAndGet();
	}
}
