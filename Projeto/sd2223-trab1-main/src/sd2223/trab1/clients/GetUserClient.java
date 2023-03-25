package sd2223.trab1.clients;

import java.io.IOException;
import java.net.URI;

public class GetUserClient {

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 3) {
			System.err.println("Use: java trab1.api.userCommands.GetUser serviceName name pwd");
			return;
		}

		String serverUrl = args[0];
		String name = args[1];
		String pwd = args[2];

		System.out.println("Sending request to server.");

		var result = new RestUsersClient(URI.create(serverUrl)).getUser(name, pwd);
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
