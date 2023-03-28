package sd2223.trab1.clients;

import java.net.URI;
import java.io.IOException;
import sd2223.trab1.api.User;
import sd2223.trab1.server.Discovery;

import java.util.logging.Logger;

public class CreateUserClient {

	private static Logger Log = Logger.getLogger(CreateUserClient.class.getName());
	public static final String SERVICE = "users";

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 4) {
			System.err
					.println(
							"Use: java -cp sd2223.jar sd2223.trab1.clients.CreateUserClient name pwd domain displayName");
			return;
		}

		String name = args[0];
		String pwd = args[1];
		String domain = args[2];
		String displayName = args[3];

		var u = new User(name, pwd, domain, displayName);

		Log.info("Sending request to server.");
		// domain-1:users<tab>http://users.domain-1/rest
		URI serverUrl = Discovery.getInstance().knownUrisOf(domain, SERVICE);

		var result = new RestUsersClient(serverUrl).createUser(u);
		System.out.println("Result: " + result);

	}

}
