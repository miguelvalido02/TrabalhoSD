package sd2223.trab1.server.java;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import sd2223.trab1.api.User;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.api.java.Result.ErrorCode;
import sd2223.trab1.api.java.Users;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;

import java.net.URI;
import sd2223.trab1.server.Domain;
import sd2223.trab1.server.Discovery;
import sd2223.trab1.clients.FeedsClientFactory;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.WebApplicationException;

@Singleton
public class JavaUsers implements Users {
	private static final String FEEDS_SERVICE = "feeds";

	private final Map<String, User> users = new ConcurrentHashMap<String, User>();

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private ClientConfig config;
	private ExecutorService executor;

	public JavaUsers() {
		config = new ClientConfig();
		config.property(ClientProperties.READ_TIMEOUT, 5000);
		config.property(ClientProperties.CONNECT_TIMEOUT, 5000);
		executor = Executors.newFixedThreadPool(50);
	}

	@Override
	public Result<String> createUser(User user) {
		if (user == null || user.getDisplayName() == null || user.checkPasswordNull() || user.getName() == null ||
				user.getDomain() == null) {
			Log.info("User object invalid.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}
		synchronized (users) {
			if (users.putIfAbsent(user.getName(), user) != null) {
				Log.info("User already exists.");
				return Result.error(ErrorCode.CONFLICT);
			}
		}
		return Result.ok(user.getName() + "@" + user.getDomain());
	}

	@Override
	public Result<User> getUser(String name, String pwd) {
		return checkUser(name, pwd);
	}

	@Override
	public Result<User> userExists(String name) {
		User u = null;
		u = users.get(name);
		if (u == null)
			throw new WebApplicationException(Status.NOT_FOUND);
		return Result.ok(u);
	}

	@Override
	public Result<User> updateUser(String name, String oldPwd, User user) {
		Result<User> u = checkUser(name, oldPwd);
		if (!name.equals(user.getName()))
			return Result.error(ErrorCode.BAD_REQUEST);
		if (u.isOK()) {
			User us = u.value();
			synchronized (users) {
				if (us != null) {
					String displayName = user.getDisplayName() == null ? us.getDisplayName() : user.getDisplayName();
					if (user.checkPasswordNull())
						us.setDisplayName(displayName);
					else {
						user.setDisplayName(displayName);
						user.setDomain(Domain.getDomain());
						users.put(name, user);
						return Result.ok(user);
					}
				}
			}
			return Result.ok(us);
		}
		return u;
	}

	@Override
	public Result<User> deleteUser(String name, String pwd) {
		Result<User> us = checkUser(name, pwd);
		if (us.isOK()) {
			executor.submit(() -> deleteFeed(name, pwd));
			synchronized (users) {
				User u = users.remove(name);
				return Result.ok(u);
			}
		}
		return us;
	}

	private void deleteFeed(String name, String pwd) {
		try {
			Discovery d = Discovery.getInstance();
			URI userURI = d.knownUrisOf(Domain.getDomain(), FEEDS_SERVICE);
			FeedsClientFactory.get(userURI).deleteFeed(name, Domain.getDomain(), pwd);
		} catch (InterruptedException e) {
		}
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		List<User> l = new ArrayList<User>();
		synchronized (users) {
			for (User user : users.values()) {
				if (user.getName().contains(pattern))
					l.add(new User(user.getName(), "", user.getDomain(), user.getDisplayName()));
			}
		}
		return Result.ok(l);
	}

	private Result<User> checkUser(String name, String pwd) {
		// Check if user is valid
		if (name == null || pwd == null) {
			Log.info("name or pwd null.");
			return Result.error(ErrorCode.BAD_REQUEST);
		}
		synchronized (users) {
			User user = users.get(name);
			// Check if user exists
			if (user == null) {
				Log.info("User does not exist.");
				return Result.error(ErrorCode.NOT_FOUND);
			}
			// Check if the pwd is correct
			if (!user.checkPassword(pwd)) {
				Log.info("pwd is incorrect.");
				return Result.error(ErrorCode.FORBIDDEN);
			}
			return Result.ok(user);
		}
	}
}
