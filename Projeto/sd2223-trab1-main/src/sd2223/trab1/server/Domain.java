package sd2223.trab1.server;

public class Domain {
    private static String domain;
    private static int seq;

    public static void setDomain(String domain) {
        Domain.domain = domain;
    }

    public static String getDomain() {
        return domain;
    }

    public static void setSeq(int seq) {
        Domain.seq = seq;
    }

    public static int getSeq() {
        return seq;
    }
}
