package ez.minar.system.managers;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class FriendManager {
    private static final Map<String, String> FRIENDS = new LinkedHashMap<>();

    private FriendManager() {
    }

    public static boolean add(String name) {
        return FRIENDS.putIfAbsent(normalize(name), name) == null;
    }

    public static boolean remove(String name) {
        return FRIENDS.remove(normalize(name)) != null;
    }

    public static int clear() {
        int size = FRIENDS.size();
        FRIENDS.clear();
        return size;
    }

    public static Map<String, String> getFriends() {
        return new LinkedHashMap<>(FRIENDS);
    }

    public static void setFriends(Map<String, String> friends) {
        FRIENDS.clear();
        FRIENDS.putAll(friends);
    }

    public static boolean isFriend(String name) {
        return name != null && FRIENDS.containsKey(normalize(name));
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
