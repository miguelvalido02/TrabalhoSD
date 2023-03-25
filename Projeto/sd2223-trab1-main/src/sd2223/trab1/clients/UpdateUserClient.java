package sd2223.trab1.clients;

import java.net.URI;
import java.io.IOException;
import sd2223.trab1.api.User;

public class UpdateUserClient {

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 6) {
			System.err.println(
					"Use: java trab1.api.clients.UpdateUserClient serviceName name oldpwd pwd displayName domain");
			return;
		}

		String serverUrl = args[0];
		String name = args[1];
		String oldpwd = args[2];
		String pwd = args[3];
		String domain = args[4];
		String displayName = args[5];

		var u = new User(name, pwd, domain, displayName);

		System.out.println("Sending request to server.");

		var result = new RestUsersClient(URI.create(serverUrl)).updateUser(name, oldpwd, u);
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
