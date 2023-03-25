package sd2223.trab1.clients;

import java.io.IOException;
import java.net.URI;

import sd2223.trab1.server.Discovery;

public class DeleteUserClient {

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 3) {
			System.err.println("Use: java -cp sd2223.jar sd2223.trab1.clients.DeleteUserClient serviceName name pwd");
			return;
		}

		String serviceName = args[0];
		String name = args[1];
		String pwd = args[2];

		System.out.println("Sending request to server.");

		String serverUrl = Discovery.getInstance().knownUrisOf(serviceName, 1)[0].toString();

		var result = new RestUsersClient(URI.create(serverUrl)).deleteUser(name, pwd);
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
