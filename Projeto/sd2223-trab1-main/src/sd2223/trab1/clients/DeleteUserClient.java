package sd2223.trab1.clients;

import java.io.IOException;
import java.net.URI;

import sd2223.trab1.server.Discovery;

public class DeleteUserClient {

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 4) {
			System.err.println(
					"Use: java -cp sd2223.jar sd2223.trab1.clients.DeleteUserClient domain serviceName name pwd");
			return;
		}
		String domain = args[0];
		String serviceName = args[1];
		String name = args[2];
		String pwd = args[3];

		System.out.println("Sending request to server.");

		String serverUrl = Discovery.getInstance().knownUrisOf(domain, serviceName, 1)[0].toString();

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
