package sd2223.trab1.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import jakarta.xml.ws.Endpoint;
import sd2223.trab1.server.soap.SoapUsersWebService;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;

public class SoapUsersServer {

	public static final int PORT = 8081;
	public static final String SERVICE_NAME = "users";
	public static String SERVER_BASE_URI = "https://%s:%s/soap";
	private static Logger Log = Logger.getLogger(SoapUsersServer.class.getName());
	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static void main(String[] args) throws Exception {
		String domain = args[0];
		Domain.setDomain(domain);
		Log.setLevel(Level.INFO);

		String ip = InetAddress.getLocalHost().getHostName();
		var server = HttpsServer.create(new InetSocketAddress(ip, PORT), 0);
		server.setExecutor(Executors.newCachedThreadPool());
		server.setHttpsConfigurator(new HttpsConfigurator(SSLContext.getDefault()));
		var endpoint = Endpoint.create(new SoapUsersWebService());
		endpoint.publish(server.createContext("/soap"));
		server.start();

		String serverURI = String.format(SERVER_BASE_URI, ip, PORT);
		Log.info(String.format("%s Soap Server ready @ %s\n", SERVICE_NAME, serverURI));

		Discovery d = Discovery.getInstance();
		d.announce(domain, SERVICE_NAME, serverURI);
	}
}
