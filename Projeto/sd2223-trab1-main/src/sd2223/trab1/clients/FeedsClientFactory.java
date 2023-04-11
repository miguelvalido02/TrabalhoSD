package sd2223.trab1.clients;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.clients.rest.RestFeedsClient;
import sd2223.trab1.clients.soap.SoapFeedsClient;

public class FeedsClientFactory {

    private static final String REST = "/rest";
    private static final String SOAP = "/soap";
    private static Map<String, Feeds> clients = new ConcurrentHashMap<String, Feeds>();

    public static Feeds get(URI serverURI) {
        var uriString = serverURI.toString();
        if (clients.containsKey(uriString))
            return clients.get(uriString);
        Feeds newFeedClient = null;
        if (uriString.endsWith(REST)) {
            clients.put(uriString, newFeedClient = new RestFeedsClient(serverURI));
            return newFeedClient;
        } else if (uriString.endsWith(SOAP)) {
            clients.put(uriString, newFeedClient = new SoapFeedsClient(serverURI));
            return newFeedClient;
        } else
            throw new RuntimeException("Unknown service type..." + uriString);
    }
}
