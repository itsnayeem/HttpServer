import java.io.IOException;

import org.apache.log4j.Logger;


public class WebServer {
	private static Logger log = Logger.getLogger(WebServer.class);
	public static String DataServerAddress = null;
	public static int DataServerPort;

    public static void main(String[] args) {
    	String ServerType = args[0];
    	String MyPort = args[1];
    	if (ServerType.equals("-f")) {
    		DataServerAddress = args[2];
    		DataServerPort = Integer.parseInt(args[3]);
    	}
    	
    	log.info("Starting server type: " + ServerType + " port: " + MyPort);
    	
        Thread t = null;
		try {
			t = new RequestListener(ServerType, Integer.parseInt(MyPort));
		} catch (IOException e) {
		}
        t.setDaemon(false);
        t.start();
    }
}
