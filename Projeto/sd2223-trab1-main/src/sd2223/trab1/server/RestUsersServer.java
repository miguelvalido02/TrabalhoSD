package sd2223.trab1.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2223.trab1.server.rest.RestUsersResource;

public class RestUsersServer {

	private static Logger Log = Logger.getLogger(RestUsersServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static final int PORT = 8080;
	public static final String SERVICE = "users";
	private static final String SERVER_URI_FMT = "https://%s:%s/rest";

	public static void main(String[] args) {
		try {
			String domain = args[0];
			Domain.setDomain(domain);
			ResourceConfig config = new ResourceConfig();
			config.register(RestUsersResource.class);

			// String ip = InetAddress.getLocalHost().getHostAddress();
			String ip = InetAddress.getLocalHost().getHostName();
			String serverURI = String.format(SERVER_URI_FMT, ip, PORT);
			URI uri = URI.create(serverURI);
			JdkHttpServerFactory.createHttpServer(uri, config, SSLContext.getDefault());
			Log.info(String.format("%s Server ready @ %s\n", SERVICE, serverURI));

			Discovery d = Discovery.getInstance();
			d.announce(domain, SERVICE, serverURI);

		} catch (Exception e) {
			Log.severe(e.getMessage());
		}
	}
}
