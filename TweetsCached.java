
public class TweetsCached {
	private int version;
	private String content;
	
	public TweetsCached(int version, String content) {
		this.version = version;
		this.content = content;
	}
	
	public int getVersion() {
		return version;
	}
	
	public String getContent() {
		return content;
	}
}
