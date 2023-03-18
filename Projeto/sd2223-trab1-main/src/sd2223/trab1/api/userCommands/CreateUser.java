package sd2223.trab1.api.userCommands;

import java.io.IOException;

import org.glassfish.jersey.client.ClientConfig;

import sd2223.trab1.api.User;
import sd2223.trab1.api.rest.UsersService;
import sd2223.trab1.api.services.Discovery;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class CreateUser {

	public static void main(String[] args) throws IOException, InterruptedException {

		if (args.length != 5) {
			System.err
					.println("Use: java trab1.api.userCommands.CreateUser serviceName name pwd domain displayName");
			return;
		}

		String serverUrl = args[0]; // O que entra aqui?
		String name = args[1];
		String pwd = args[2];
		String domain = args[3];
		String displayName = args[4];

		var u = new User(name, pwd, domain, displayName);

		System.out.println("Sending request to server.");

		String serviceName = Discovery.getInstance().knownUrisOf(serverUrl, 1)[0].toString();

		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);

		WebTarget target = client.target(serviceName).path(UsersService.PATH);

		Response r = target.request()
				.accept(MediaType.APPLICATION_JSON)
				.post(Entity.entity(u, MediaType.APPLICATION_JSON));

		if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
			System.out.println("Success, created user with id: " + r.readEntity(String.class));
		else
			System.out.println("Error, HTTP error status: " + r.getStatus());

	}

}
