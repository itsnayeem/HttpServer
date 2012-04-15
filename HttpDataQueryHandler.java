import static argo.jdom.JsonNodeBuilders.aStringBuilder;
import static argo.jdom.JsonNodeBuilders.anObjectBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import argo.format.CompactJsonFormatter;
import argo.jdom.JsonObjectNodeBuilder;

public class HttpDataQueryHandler implements HttpRequestHandler {

	public void handle(final HttpRequest request, final HttpResponse response,
			final HttpContext context) throws HttpException, IOException {

		String method = request.getRequestLine().getMethod()
				.toUpperCase(Locale.ENGLISH);

		System.out.println("Handling Data Query; Line = " + request.getRequestLine());
		if (method.equals("GET")) {
			final String target = request.getRequestLine().getUri();

			Pattern p = Pattern.compile("/data/query\\?q=([^&]*)&v=([^&]*)$");
			Matcher m = p.matcher(target);
			if (m.find()) {
				System.out.println("'" + target + "' matches the pattern");
				System.out.flush();
				String hash = m.group(1);
				int version = Integer.parseInt(m.group(2));
				DataStorage ds = DataStorage.getInstance();
				
				JsonObjectNodeBuilder bbuild = anObjectBuilder();
				
				DataStorage.lock.readLock().lock();
				
				if (ds.containsHash(hash)) {
					System.out.println("'" + hash + "' is in datastorage");
					System.out.flush();
					if (ds.isLatest(hash, version)) {
						bbuild.withField("status", aStringBuilder("ok"));
					} else {
						bbuild.withField("status", aStringBuilder("updated"));
						bbuild.withField("version", aStringBuilder("" + ds.getVersion(hash)));
						bbuild.withField("tweets", ds.getTweetsArrayBuilder(hash));
					}
				} else {
					System.out.println("'" + hash + "' isn't in cache or datastorage");
					System.out.flush();
					bbuild.withField("status", aStringBuilder("error"));
				}
				DataStorage.lock.readLock().unlock();
				
				final String Content = new CompactJsonFormatter().format(bbuild.build());
				System.out.println("Backend JSON: " + Content);
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream)
							throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(
								outstream, "UTF-8");
						writer.write(Content);
						writer.write("\n");
						writer.flush();
					}
				});
				body.setContentType("application/json; charset=UTF-8");
				
				response.setStatusCode(HttpStatus.SC_OK);
				response.setEntity(body);
			} else {
				response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
			}
		} else {
			throw new MethodNotSupportedException(method
					+ " method not supported\n");
		}

	}
}