package aula9;

import aula9.api.java.Message;
import aula9.mastodon.Mastodon;

public class Test {
	public static void main(String[] args) {
		var res0 = Mastodon.getInstance().postMessage("59744", "b37c6b8be0ea87541a3ed6c068f36669",
				new Message(0, "59744", "artur", "test ;;;; " + System.currentTimeMillis()));
		System.out.println(res0);

		var res1 = Mastodon.getInstance().getMessages("59744", 0);
		System.out.println(res1);

		var res2 = Mastodon.getInstance().removeFromPersonalFeed("59744", res0.value(),
				"b37c6b8be0ea87541a3ed6c068f36669");
		System.out.println(res2);
	}
}
