package sd2223.trab1.server.mastodon.msgs;

public record MastodonAccount(String id, String username) {

    public String getId() {
        return id;
    }

    public String getUserName() {
        return username;
    }

}
