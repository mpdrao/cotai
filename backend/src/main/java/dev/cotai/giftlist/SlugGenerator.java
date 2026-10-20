package dev.cotai.giftlist;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Builds public slugs like {@code churrasco-do-erick-7f3k}: a readable prefix plus 4 random
 * base32 chars ({@code [a-z2-7]}). Pattern: {@code ^[a-z0-9-]+-[a-z2-7]{4}$}.
 */
@Component
public class SlugGenerator {

    private static final char[] BASE32 = "abcdefghijklmnopqrstuvwxyz234567".toCharArray();
    private static final int MAX_PREFIX = 60;

    private final SecureRandom random = new SecureRandom();

    public String generate(String title) {
        return prefix(title) + "-" + suffix();
    }

    static String prefix(String title) {
        String ascii = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.length() > MAX_PREFIX) {
            slug = slug.substring(0, MAX_PREFIX).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "lista" : slug;
    }

    private String suffix() {
        char[] chars = new char[4];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = BASE32[random.nextInt(BASE32.length)];
        }
        return new String(chars);
    }
}
