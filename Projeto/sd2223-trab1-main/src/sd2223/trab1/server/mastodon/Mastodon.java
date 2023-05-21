package sd2223.trab1.server.mastodon;

import static sd2223.trab1.api.java.Result.error;
import static sd2223.trab1.api.java.Result.ok;
import static sd2223.trab1.api.java.Result.ErrorCode.*;

import java.util.List;

import com.google.gson.reflect.TypeToken;

import sd2223.trab1.api.Message;
import sd2223.trab1.api.java.Feeds;
import sd2223.trab1.api.java.Result;
import sd2223.trab1.server.mastodon.msgs.MastodonAccount;
import sd2223.trab1.server.mastodon.msgs.PostStatusArgs;
import sd2223.trab1.server.mastodon.msgs.PostStatusResult;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import sd2223.trab1.server.util.JSON;

public class Mastodon implements Feeds {

	static String MASTODON_NOVA_SERVER_URI = "http://10.170.138.52:3000";
	static String MASTODON_SOCIAL_SERVER_URI = "https://mastodon.social";

	static String MASTODON_SERVER_URI = MASTODON_NOVA_SERVER_URI;

	private static final String clientKey = "KXkdmRm9CC8oE2GI6AJaeyLk6r_hns18ILErvMufjlU";
	private static final String clientSecret = "uz5BWuywl5mrw7nLQppidnUDbHVhu6AX72uX5jC9848";
	private static final String accessTokenStr = "k_KOCnJWOwpqTLvWYVIoOWUSGUiJNwdTYfGE0nc9edo";

	static final String STATUSES_PATH = "/api/v1/statuses";
	static final String TIMELINES_PATH = "/api/v1/timelines/home";
	static final String ACCOUNT_FOLLOWING_PATH = "/api/v1/accounts/%s/following";
	static final String VERIFY_CREDENTIALS_PATH = "/api/v1/accounts/verify_credentials";
	static final String SEARCH_ACCOUNTS_PATH = "/api/v1/accounts/search";
	static final String ACCOUNT_FOLLOW_PATH = "/api/v1/accounts/%s/follow";
	static final String ACCOUNT_UNFOLLOW_PATH = "/api/v1/accounts/%s/unfollow";

	private static final int HTTP_OK = 200;

	protected OAuth20Service service;
	protected OAuth2AccessToken accessToken;

	public Mastodon() {
		try {
			service = new ServiceBuilder(clientKey).apiSecret(clientSecret).build(MastodonApi.instance());
			accessToken = new OAuth2AccessToken(accessTokenStr);
		} catch (Exception x) {
			x.printStackTrace();
			System.exit(0);
		}
	}

	private String getEndpoint(String path, Object... args) {
		var fmt = MASTODON_SERVER_URI + path;
		return String.format(fmt, args);
	}

	@Override
	public Result<Long> postMessage(String user, String pwd, Message msg) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.POST, getEndpoint(STATUSES_PATH));
			JSON.toMap(new PostStatusArgs(msg.getText())).forEach((k, v) -> {
				request.addBodyParameter(k, v.toString());
			});
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), PostStatusResult.class);
				return ok(res.getId());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<List<Message>> getMessages(String user, long time) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(TIMELINES_PATH));
			request.addQuerystringParameter("min_id", String.valueOf(time << 16));
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				List<PostStatusResult> res = JSON.decode(response.getBody(), new TypeToken<List<PostStatusResult>>() {
				});
				return ok(
						res.stream().map(PostStatusResult::toMessage).toList());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(INTERNAL_ERROR);
	}

	@Override
	public Result<Message> getMessage(String user, long mid) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(STATUSES_PATH) + "/" + mid);
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				var res = JSON.decode(response.getBody(), PostStatusResult.class);
				return ok(res.toMessage());
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(NOT_FOUND);
	}

	@Override
	public Result<Void> removeFromPersonalFeed(String user, long mid, String pwd) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.DELETE, getEndpoint(STATUSES_PATH) + "/" + mid);
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK)
				return ok();
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(NOT_FOUND);
	}

	@Override
	public Result<Void> subUser(String user, String userSub, String pwd) {
		try {
			String id = getId(userSub);
			if (id == null)
				return error(NOT_FOUND);
			final OAuthRequest request = new OAuthRequest(Verb.POST, getEndpoint(ACCOUNT_FOLLOW_PATH, id));
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK)
				return ok();
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(NOT_FOUND);
	}

	@Override
	public Result<Void> unsubscribeUser(String user, String userSub, String pwd) {
		try {
			String id = getId(userSub);
			if (id == null)
				return error(NOT_FOUND);
			final OAuthRequest request = new OAuthRequest(Verb.POST, getEndpoint(ACCOUNT_UNFOLLOW_PATH, id));
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK)
				return ok();
		} catch (Exception x) {
			x.printStackTrace();
		}
		return error(NOT_FOUND);
	}

	private String getId(String user) {
		try {
			final OAuthRequest request = new OAuthRequest(Verb.GET, getEndpoint(SEARCH_ACCOUNTS_PATH));
			request.addQuerystringParameter("q", user);
			service.signRequest(accessToken, request);
			Response response = service.execute(request);
			if (response.getCode() == HTTP_OK) {
				List<MastodonAccount> res = JSON.decode(response.getBody(), new TypeToken<List<MastodonAccount>>() {
				});
				MastodonAccount account = res.get(0);
				return account.getId();
			}
		} catch (Exception x) {
			x.printStackTrace();
		}
		return null;
	}

	@Override
	public Result<Void> postOutside(String user, Message msg) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<List<String>> listSubs(String user) {
		return error(NOT_IMPLEMENTED);
	}

	@Override
	public Result<Void> deleteFeed(String user, String domain, String pwd) {
		return error(NOT_IMPLEMENTED);
	}

}
