package sd2223.trab1.clients;

import java.net.URI;
import java.io.IOException;
import sd2223.trab1.api.User;
import sd2223.trab1.server.Discovery;

import java.util.logging.Logger;

public class CreateUserClient {

	private static Logger Log = Logger.getLogger(CreateUserClient.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 5) {
			System.err
					.println(
							"Use: java -cp sd2223.jar sd2223.trab1.clients.CreateUserClient serviceName name pwd domain displayName");
			return;
		}

		String serviceName = args[0];
		String name = args[1];
		String pwd = args[2];
		String domain = args[3];
		String displayName = args[4];

		var u = new User(name, pwd, domain, displayName);

		Log.info("Sending request to server.");
		// domain-1:users<tab>http://users.domain-1/rest
		String serverUrl = Discovery.getInstance().knownUrisOf(serviceName, 1)[0].toString();

		var result = new RestUsersClient(URI.create(serverUrl)).createUser(u);
		System.out.println("Result: " + result);

	}

}
