package sd2223.trab1.server;

import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.xml.ws.Endpoint;
import sd2223.trab1.server.soap.SoapFeedsWebService;

public class SoapFeedsServer {

    public static final int PORT = 8081;
    public static final String SERVICE_NAME = "feeds";
    public static String SERVER_BASE_URI = "http://%s:%s/soap";
    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    private static Logger Log = Logger.getLogger(SoapFeedsServer.class.getName());

    public static void main(String[] args) throws Exception {
        String domain = args[0];
        Domain.setDomain(domain);
        Domain.setSeq(Integer.parseInt(args[1]));
        Log.setLevel(Level.INFO);

        String ip = InetAddress.getLocalHost().getHostAddress();
        String serverURI = String.format(SERVER_BASE_URI, ip, PORT);

        Endpoint.publish(serverURI.replace(ip, "0.0.0.0"), new SoapFeedsWebService());
        Log.info(String.format("%s Soap Server ready @ %s\n", SERVICE_NAME, serverURI));

        Discovery d = Discovery.getInstance();
        d.announce(domain, SERVICE_NAME, serverURI);
    }
}
