package sd2223.trab1.clients;

import java.net.URI;
import java.io.IOException;
import sd2223.trab1.api.User;
import java.util.logging.Logger;

public class CreateUserClient {

	private static Logger Log = Logger.getLogger(CreateUserClient.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 5) {
			System.err
					.println("Use: java trab1.api.clients.CreateUserClient serviceName name pwd domain displayName");
			return;
		}

		String serverUrl = args[0]; // O que entra aqui?
		String name = args[1];
		String pwd = args[2];
		String domain = args[3];
		String displayName = args[4];

		var u = new User(name, pwd, domain, displayName);

		Log.info("Sending request to server.");

		var result = new RestUsersClient(URI.create(serverUrl)).createUser(u);
		System.out.println("Result: " + result);

		/*
		 * String serviceName = Discovery.getInstance().knownUrisOf(serverUrl,
		 * 1)[0].toString();
		 * 
		 * ClientConfig config = new ClientConfig();
		 * Client client = ClientBuilder.newClient(config);
		 * 
		 * WebTarget target = client.target(serviceName).path(UsersService.PATH);
		 * 
		 * Response r = target.request()
		 * .accept(MediaType.APPLICATION_JSON)
		 * .post(Entity.entity(u, MediaType.APPLICATION_JSON));
		 * 
		 * if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
		 * System.out.println("Success, created user with id: " +
		 * r.readEntity(String.class));
		 * else
		 * System.out.println("Error, HTTP error status: " + r.getStatus());
		 */

	}

}
