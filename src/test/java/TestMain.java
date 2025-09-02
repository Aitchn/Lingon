import io.aitchn.lingon.Lingon;
import io.aitchn.lingon.LingonLang;
import io.aitchn.lingon.LocalizedString;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

public class TestMain {
    public static void main(String[] args) {
        final String TEST_HOME = System.getenv("TEST_HOME");
        final Path lingonPath = Path.of(TEST_HOME).resolve("lingon");


        Lingon lingon = Lingon.getInstance(TestMain.class, lingonPath, Locale.TAIWAN);
        LingonLang lang = lingon.get(Locale.TAIWAN, "b.test");

        var text = lang.get("a.chat[0]").substitute(Map.of("name", "Jerry"));
        System.out.println(text);
    }
}
