package sd2223.trab1.clients;

import java.io.IOException;
import java.net.URI;

import sd2223.trab1.server.Discovery;

public class SearchUserClient {

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 2) {
			System.err.println("Use: java -cp sd2223.jar sd2223.trab1.clients.SearchUserClient serviceName query");
			return;
		}

		String serviceName = args[0];
		String query = args[1];

		System.out.println("Sending request to server.");

		String serverUrl = Discovery.getInstance().knownUrisOf(serviceName, 1)[0].toString();

		var result = new RestUsersClient(URI.create(serverUrl)).searchUsers(query);
		System.out.println("Result: " + result);

		/*
		 * String serviceName = Discovery.getInstance().knownUrisOf(serverUrl,
		 * 1)[0].toString();
		 * 
		 * ClientConfig config = new ClientConfig();
		 * Client client = ClientBuilder.newClient(config);
		 * 
		 * WebTarget target = client.target(serviceName).path(UsersService.PATH);
		 */
	}

}
