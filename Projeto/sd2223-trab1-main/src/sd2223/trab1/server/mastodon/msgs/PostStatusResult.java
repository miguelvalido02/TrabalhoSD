package sd2223.trab1.server.mastodon.msgs;

import java.time.Instant;
import sd2223.trab1.api.Message;
import sd2223.trab1.server.Domain;

public record PostStatusResult(String id, String content, String created_at, MastodonAccount account) {

	public long getId() {
		return Long.valueOf(id);
	}

	long getCreationTime() {
		Instant instant = Instant.parse(created_at);
		return instant.toEpochMilli();
	}

	public String getText() {
		return content.split("<p>")[1].split("</p>")[0];
	}

	public Message toMessage() {
		var m = new Message(getId(), account.username(), Domain.getDomain(), getText());
		m.setCreationTime(getCreationTime());
		return m;
	}
}