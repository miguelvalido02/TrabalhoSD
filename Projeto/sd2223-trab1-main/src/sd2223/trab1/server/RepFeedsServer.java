package sd2223.trab1.server;

import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import sd2223.trab1.server.kafka.KafkaRepFeeds;
import sd2223.trab1.server.kafka.RepFeedsInterface;
import sd2223.trab1.server.kafka.RepFeedsResource;
import sd2223.trab1.server.kafka.sync.SyncPoint;

public class RepFeedsServer {

    private static Logger Log = Logger.getLogger(RestFeedsServer.class.getName());

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
    }

    public static final int PORT = 9092;
    public static final String SERVICE = "feeds";
    private static final String SERVER_URI_FMT = "https://%s:%s/rest";

    public static void main(String[] args) {
        try {
            String domain = args[0];
            int seq = Integer.parseInt(args[1]);
            Domain.setDomain(domain);
            Domain.setSeq(seq);
            SyncPoint<Object> sync = new SyncPoint<>();
            ResourceConfig config = new ResourceConfig();
            RepFeedsInterface rep = new KafkaRepFeeds(sync);
            config.register(new RepFeedsResource(rep));
            config.register(new VersionFilter(sync));

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
