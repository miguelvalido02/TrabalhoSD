package sd2223.trab1.clients;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import sd2223.trab1.api.java.Users;
import sd2223.trab1.clients.rest.RestUsersClient;
import sd2223.trab1.clients.soap.SoapUsersClient;

public class UsersClientFactory {

    private static final String REST = "/rest";
    private static final String SOAP = "/soap";
    private static Map<String, Users> clients = new ConcurrentHashMap<String, Users>();

    public static Users get(URI serverURI) {
        var uriString = serverURI.toString();
        if (clients.containsKey(uriString))
            return clients.get(uriString);
        Users newUserClient = null;
        if (uriString.endsWith(REST)) {
            clients.put(uriString, newUserClient = new RestUsersClient(serverURI));
            return newUserClient;
        } else if (uriString.endsWith(SOAP)) {
            clients.put(uriString, newUserClient = new SoapUsersClient(serverURI));
            return newUserClient;
        } else
            throw new RuntimeException("Unknown service type..." + uriString);
    }
}
