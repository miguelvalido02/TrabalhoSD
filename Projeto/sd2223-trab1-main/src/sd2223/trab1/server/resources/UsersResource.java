package sd2223.trab1.server.resources;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.function.Supplier;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;

import java.net.URI;
import sd2223.trab1.api.User;
import sd2223.trab1.server.Domain;
import sd2223.trab1.server.Discovery;
import sd2223.trab1.api.rest.FeedsService;
import sd2223.trab1.api.rest.UsersService;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.WebApplicationException;

@Singleton
public class UsersResource implements UsersService {
	private static final int MAX_RETRIES = 10;
	private static final int RETRY_SLEEP = 500;

	private static final String FEEDS_SERVICE = "feeds";

	private final Map<String, User> users = new ConcurrentHashMap<String, User>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	private Client client;
	private ClientConfig config;
	private ExecutorService executor;

	public UsersResource() {
		config = new ClientConfig();
		config.property(ClientProperties.READ_TIMEOUT, 5000);
		config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
		client = ClientBuilder.newClient(config);
		executor = Executors.newFixedThreadPool(50);
	}

	@Override
	public String createUser(User user) {
		// Log.info("createUser : " + user);

		// Check if user data is valid
		if (user == null || user.getDisplayName() == null || user.getPwd() == null || user.getName() == null ||
				user.getDomain() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		synchronized (users) {
			// Insert new user, checking if name already exists
			if (users.putIfAbsent(user.getName(), user) != null) {
				Log.info("User already exists.");
				throw new WebApplicationException(Status.CONFLICT);
			}
		}
		return user.getName() + "@" + user.getDomain();
	}

	@Override
	public User getUser(String name, String pwd) {
		// Log.info("getUser : name = " + name + " ; pwd = " + pwd);
		synchronized (users) {
			return checkUser(name, pwd);
		}
	}

	@Override
	public User userExists(String name) {
		User u = users.get(name);
		if (u == null)
			throw new WebApplicationException(Status.NOT_FOUND);
		return u;
	}

	@Override
	public User updateUser(String name, String oldPwd, User user) {
		// Log.info("updateUser : name = " + name + "; pwd = " + oldPwd + " ; user = " +
		// user);
		synchronized (users) {
			User u = checkUser(name, oldPwd);

			if (u != null) {
				String pwd = user.getPwd() == null ? oldPwd : user.getPwd();
				u.setPwd(pwd);
				String displayName = user.getDisplayName() == null ? u.getDisplayName() : user.getDisplayName();
				u.setDisplayName(displayName);
			}

			return u;
		}
	}

	@Override
	public User deleteUser(String name, String pwd) {
		// Log.info("deleteUser : user = " + name + "; pwd = " + pwd);
		synchronized (users) {
			checkUser(name, pwd);

			executor.submit(() -> {
				reTry(() -> {
					deleteFeed(name, pwd);
					return null;
				});
			});
			return users.remove(name);
		}
	}

	private void deleteFeed(String name, String pwd) {
		try {
			Discovery d = Discovery.getInstance();
			URI userURI = d.knownUrisOf(Domain.getDomain(), FEEDS_SERVICE);
			WebTarget target = client.target(userURI).path(FeedsService.PATH);
			target.path("delete").path(name).path(Domain.getDomain()).queryParam(FeedsService.PWD, pwd).request()
					.delete();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public List<User> searchUsers(String pattern) {
		// Log.info("searchUsers : pattern = " + pattern);
		List<User> l = new ArrayList<User>();
		synchronized (users) {
			for (User user : users.values()) {
				if (user.getName().contains(pattern))
					l.add(new User(user.getName(), "", user.getDomain(), user.getDisplayName()));
			}
		}
		return l;
	}

	private User checkUser(String name, String pwd) {
		// Check if user is valid
		if (name == null || pwd == null) {
			Log.info("name or pwd null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		User user = users.get(name);
		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		// Check if the pwd is correct
		if (!user.getPwd().equals(pwd)) {
			Log.info("pwd is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		return user;
	}

	protected <T> T reTry(Supplier<T> func) {
		// método generico recebe uma funçao que quando invocada devolve T.
		// createUser, etc...
		for (int i = 0; i < MAX_RETRIES; i++)
			try {
				return func.get();
			} catch (ProcessingException x) {
				System.err.println(x.getMessage());
				Log.fine("ProcessingException: " + x.getMessage());
				sleep(RETRY_SLEEP);
			}
		return null;
	}

	private void sleep(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException x) { // nothing to do...
		}
	}
}
