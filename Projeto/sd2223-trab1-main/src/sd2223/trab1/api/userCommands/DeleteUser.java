package sd2223.trab1.api.userCommands;

import java.io.IOException;

import org.glassfish.jersey.client.ClientConfig;

import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.UsersService;
import sd2223.trab1.api.services.Discovery;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class DeleteUser {

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 3) {
			System.err.println("Use: java trab1.api.userCommands.DeleteUser serviceName userId password");
			return;
		}

		String serverUrl = args[0];
		String userId = args[1];
		String password = args[2];

		System.out.println("Sending request to server.");

		String serviceName = Discovery.getInstance().knownUrisOf(serverUrl, 1)[0].toString();

		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target(serviceName).path(UsersService.PATH);

		Response r = target.path(userId)
				.queryParam(UsersService.PWD, password).request()
				.accept(MediaType.APPLICATION_JSON).delete();

		if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
			System.out.println("Success:");
			var u = r.readEntity(User.class);
			System.out.println("User Deleted: " + u);
		} else
			System.out.println("Error, HTTP error status: " + r.getStatus());

	}

}
