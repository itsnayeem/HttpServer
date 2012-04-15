import static argo.jdom.JsonNodeFactories.aJsonArray;
import static argo.jdom.JsonNodeFactories.aJsonField;
import static argo.jdom.JsonNodeFactories.aJsonObject;
import static argo.jdom.JsonNodeFactories.aJsonString;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import argo.format.CompactJsonFormatter;
import argo.jdom.JdomParser;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;

public class TweetsLib {
	private static Logger log = Logger.getLogger(TweetsLib.class);
	
	public static void pushTweetToServer(String tweet) {
		log.info("Pushing tweet to server: " + tweet);
		HttpClient httpclient = new DefaultHttpClient();
		List<NameValuePair> formparams = new ArrayList<NameValuePair>();
		formparams.add(new BasicNameValuePair("t", tweet));
		UrlEncodedFormEntity entity = null;
		try {
			entity = new UrlEncodedFormEntity(formparams, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		HttpPost httppost = new HttpPost("http://"+WebServer.DataServerAddress+":"
				+WebServer.DataServerPort+"/data/update");
		httppost.setEntity(entity);
		try {
			httpclient.execute(httppost);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
	}

	public static void processTweet(String tweet) {
		log.info("Processing '" + tweet + "'");
		DataStorage ds = DataStorage.getInstance();
		Pattern p = Pattern.compile("#(\\S*)");
		Matcher m = p.matcher(tweet);

		synchronized(ds) {
			while (m.find()) {
				String hash = m.group(1);
				ds.addTweet(hash, tweet);
				log.info("Added '" + tweet + "' to storage");
			}
		}
	}

	public static String getContent(String hash) {
		DataCache dc = DataCache.getInstance();
		int currentVersion = dc.getVersion(hash);

		HttpClient httpclient = new DefaultHttpClient();
		log.info("Connecting to backend to search for #" + hash);
		HttpGet httpget = new HttpGet("http://" + WebServer.DataServerAddress
				+ ":" + WebServer.DataServerPort + "/data/query?q=" + hash
				+ "&v=" + currentVersion);
		HttpResponse response = null;
		try {
			response = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		}
		HttpEntity entity = response.getEntity();
		String entityContent = null;
		try {
			entityContent = EntityUtils.toString(entity);
		} catch (ParseException e) {
		} catch (IOException e) {
		}
		JdomParser parser = new JdomParser();

		JsonRootNode json = null;
		JsonRootNode retval = null;

		try {
			json = parser.parse(entityContent);
		} catch (InvalidSyntaxException e) {
		}

		String status = json.getStringValue("status");

		if (status.equals("ok")) {
			DataCache.lock.readLock().lock();
			System.out.println("Using Cache + " + dc.getTweet(hash));
			try {
				retval = aJsonObject(
						aJsonField("query", aJsonString(hash)),
						aJsonField("cached", aJsonString("yes")),
						aJsonField("tweets",
								aJsonArray(parser.parse(dc.getTweet(hash))
										.getArrayNode())));
			} catch (InvalidSyntaxException e) {
			}
			DataCache.lock.readLock().unlock();

		} else if (status.equals("updated")) {
			retval = aJsonObject(
					aJsonField("query", aJsonString(hash)),
					aJsonField("cached", aJsonString("no")),
					aJsonField("tweets",
							aJsonArray(json.getArrayNode("tweets"))));
			try {
				dc.addTweet(hash, Integer.parseInt(json
						.getStringValue("version")), new CompactJsonFormatter()
						.format(aJsonArray(json.getArrayNode("tweets"))));
			} catch (NumberFormatException e) {
			}
		} else {
			retval = json;
		}
		
		log.info("Backend returns: " + new CompactJsonFormatter().format(retval));
		return new CompactJsonFormatter().format(retval);
	}
}
