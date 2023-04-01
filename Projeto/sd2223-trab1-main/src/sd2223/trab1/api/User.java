package sd2223.trab1.api;

import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Represents a user in the system. Note: the password of a user should not be
 * returned in any method.
 */
public class User {
	private String name;
	private String pwd;
	private String displayName;
	private String domain;
	private Map<String, List<String>> followers;// <domain,List<name@domain>>
	private Set<String> following;// <name@domain,name@domain>

	public User() {
		this.pwd = null;
		this.name = null;
		this.domain = null;
		this.displayName = null;
		this.followers = new HashMap<String, List<String>>();
		this.following = new HashSet<String>();
	}

	public User(String name, String pwd, String displayName, String domain) {
		this.pwd = pwd;
		this.name = name;
		this.domain = domain;
		this.displayName = displayName;
		this.followers = new HashMap<String, List<String>>();
		this.following = new HashSet<String>();
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

	public void addFollower(String userDomain) {
		String followerDomain = userDomain.split("@")[1];
		List<String> domainList = followers.get(followerDomain);
		if (domainList == null)
			followers.put(followerDomain, domainList = new ArrayList<String>());

		domainList.add(userDomain);
	}

	public void removeFollower(String userDomain) {
		String[] nameDomain = userDomain.split("@");
		String followerName = nameDomain[0];
		String followerDomain = nameDomain[1];
		List<String> domainList = followers.get(followerDomain);
		if (domainList == null)
			return;

		domainList.remove(followerName);
	}

	public Map<String, List<String>> obtainFollowers() {
		return this.followers;
	}

	public void addFollowing(String userDomain) {
		following.add(userDomain);
	}

	public void removeFollowing(String userDomain) {
		following.remove(userDomain);
	}

	public List<String> obtainFollowing() {
		return new ArrayList<String>(following);
	}

	@Override
	public String toString() {
		return "User [name=" + name + ", pwd=" + pwd + ", displayName=" + displayName + ", domain=" + domain + "]";
	}
}
