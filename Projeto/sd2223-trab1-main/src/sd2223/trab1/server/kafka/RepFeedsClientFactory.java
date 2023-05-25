package sd2223.trab1.server.kafka;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RepFeedsClientFactory {

    private static final String REST = "/rest";
    private static Map<String, KafkaRepFeedsInterface> clients = new ConcurrentHashMap<String, KafkaRepFeedsInterface>();

    public static KafkaRepFeedsInterface get(URI serverURI) {
        var uriString = serverURI.toString();
        if (clients.containsKey(uriString))
            return clients.get(uriString);
        KafkaRepFeedsInterface newFeedClient = null;
        if (uriString.endsWith(REST)) {
            clients.put(uriString, newFeedClient = new RepFeedsClient(serverURI));
            return newFeedClient;
        } else
            throw new RuntimeException("Unknown service type..." + uriString);
    }
}
