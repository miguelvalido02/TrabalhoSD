package sd2223.trab1.api;

import java.util.Map;
import java.util.HashMap;

/**
 * Represents a user in the system. Note: the password of a user should not be
 * returned in any method.
 */
public class User {
	private String name;
	private String pwd;
	private String displayName;
	private String domain;
	private Map<String, Map<String, User>> followers;// <domain,<name,user>> TODO nao devia ser so
														// <domain,List<name>>????
	private Map<String, Map<String, User>> following;// <domain,<name,user>> TODO sera que precisamos?

	public User() {
		this.pwd = null;
		this.name = null;
		this.domain = null;
		this.displayName = null;
	}

	public User(String name, String pwd, String domain, String displayName) {
		this.pwd = pwd;
		this.name = name;
		this.domain = domain;
		this.displayName = displayName;
		followers = new HashMap<String, Map<String, User>>();
		following = new HashMap<String, Map<String, User>>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void addFollower(User newFollower) {
		Map<String, User> domainMap = followers.get(newFollower.getDomain());
		if (domainMap == null)
			followers.put(newFollower.getDomain(), domainMap = new HashMap<String, User>());

		domainMap.put(newFollower.getName(), newFollower);
	}

	public void removeFollower(User oldFollower) {
		Map<String, User> domainMap = followers.get(oldFollower.getDomain());
		if (domainMap == null)
			return;

		domainMap.remove(oldFollower.getName());
	}

	public void addFollowing(User newFollowing) {
		Map<String, User> domainMap = following.get(newFollowing.getDomain());
		if (domainMap == null)
			following.put(newFollowing.getDomain(), domainMap = new HashMap<String, User>());

		domainMap.put(newFollowing.getName(), newFollowing);
	}

	public void removeFollowing(User oldFollowing) {
		Map<String, User> domainMap = following.get(oldFollowing.getDomain());
		if (domainMap == null)
			return;

		domainMap.remove(oldFollowing.getName());
	}

	public Map<String, Map<String, User>> getFollowers() {
		return followers;
	}

	@Override
	public String toString() {
		return "User [name=" + name + ", pwd=" + pwd + ", displayName=" + displayName + ", domain=" + domain + "]";
	}
}
