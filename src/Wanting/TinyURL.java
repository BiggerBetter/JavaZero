package Wanting;

import java.util.HashMap;

public class TinyURL {

    static String string="0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    HashMap<String,String> hashSet = new HashMap<>();
    // Encodes a URL to a shortened URL.
    public String encode(String longUrl) {
        while (true) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                int index = (int) (Math.random() * 62);
                stringBuilder.append(string.charAt(index));
            }
            String ss = stringBuilder.toString();
            if (!hashSet.containsKey(ss)) {
                hashSet.put(ss, longUrl);
                return ss;
            }
        }
    }

    // Decodes a shortened URL to its original URL.
    public String decode(String shortUrl) {
        return hashSet.get(shortUrl);
    }
}
