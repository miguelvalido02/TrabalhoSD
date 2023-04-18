package sd2223.trab1.server;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import sd2223.trab1.server.soap.SoapUsersWebService;

public class SoapUsersServer {

	public static final int PORT = 8081;
	public static final String SERVICE_NAME = "users";
	public static String SERVER_BASE_URI = "http://%s:%s/soap";
	private static Logger Log = Logger.getLogger(SoapUsersServer.class.getName());
	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}

	public static void main(String[] args) throws Exception {
		String domain = args[0];
		Domain.setDomain(domain);
		Log.setLevel(Level.INFO);

		String ip = InetAddress.getLocalHost().getHostAddress();
		String serverURI = String.format(SERVER_BASE_URI, ip, PORT);

		Endpoint.publish(serverURI, new SoapUsersWebService());
		Log.info(String.format("%s Soap Server ready @ %s\n", SERVICE_NAME, serverURI));

		Discovery d = Discovery.getInstance();
		d.announce(domain, SERVICE_NAME, serverURI);
	}
}
