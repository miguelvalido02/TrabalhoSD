package aula9;

import aula9.api.java.Message;
import aula9.mastodon.Mastodon;

public class Test {
	public static void main(String[] args) {
		var res0 = Mastodon.getInstance().postMessage("", "", new Message(0, "", "", "test ;;;; " + System.currentTimeMillis()));
		System.out.println(res0);

		var res1 = Mastodon.getInstance().getMessages("", 0);
		System.out.println(res1);
	}
}
