package sd2223.trab1.clients;

import java.net.URI;
import java.io.IOException;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.User;
import sd2223.trab1.server.Discovery;

import java.util.logging.Logger;

public class PostMessageClient {
    private static Logger Log = Logger.getLogger(CreateUserClient.class.getName());
    public static final String SERVICE = "feeds";

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 4) {
            System.err
                    .println(
                            "Use: java -cp sd2223.jar sd2223.trab1.clients.PostMessage" +
                                    "name@domain pwd Message(text)");
            return;
        }

        String nameAndDomain = args[0];
        String[] nameDomain = args[0].split("@");
        String name = nameDomain[0];
        String domain = nameDomain[1];

        String pwd = args[1];
        String textM = args[2];

        var m = new Message(0, name, pwd, textM);

        Log.info("Sending request to server.");
        String serverUrl = Discovery.getInstance().knownUrisOf(domain, SERVICE, 1)[0].toString();

        var result = new RestFeedsClient(URI.create(serverUrl)).postMessage(nameAndDomain, pwd, m);
        System.out.println("Result: " + result);

    }
}
