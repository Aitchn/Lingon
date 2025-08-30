import io.aitchn.lingon.Lingon;
import io.aitchn.lingon.LingonLang;

import java.nio.file.Path;
import java.util.Locale;

public class TestMain {
    public static void main(String[] args) {
        final String TEST_HOME = System.getenv("TEST_HOME");
        final Path lingonPath = Path.of(TEST_HOME).resolve("lingon");


        Lingon lingon = new Lingon(TestMain.class, lingonPath, Locale.TAIWAN);
        LingonLang lang = lingon.get(Locale.TAIWAN, "b.test");

        String text = lang.get("a.chat[1]");
        System.out.println(text);
    }
}
