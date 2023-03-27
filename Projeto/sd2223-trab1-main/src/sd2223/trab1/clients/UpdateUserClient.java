package sd2223.trab1.clients;

import java.net.URI;
import java.io.IOException;
import sd2223.trab1.api.User;
import sd2223.trab1.server.Discovery;

public class UpdateUserClient {

	public static final String SERVICE = "users";

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 5) {
			System.err.println(
					"Use: java -cp sd2223.jar sd2223.trab1.clients.UpdateUserClient domain name oldpwd pwd displayName");
			return;
		}

		String domain = args[0];
		String name = args[1];
		String oldpwd = args[2];
		String pwd = args[3];
		String displayName = args[4];

		var u = new User(null, pwd, null, displayName);

		System.out.println("Sending request to server.");

		String serverUrl = Discovery.getInstance().knownUrisOf(domain, SERVICE, 1)[0].toString();

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
