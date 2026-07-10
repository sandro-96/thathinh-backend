package vn.thathinh.util;

public final class UserPairUtils {

    private UserPairUtils() {}

    public record UserPair(String lowId, String highId) {}

    public static UserPair sort(String userA, String userB) {
        if (userA.compareTo(userB) <= 0) {
            return new UserPair(userA, userB);
        }
        return new UserPair(userB, userA);
    }

    public static String partnerId(String userLowId, String userHighId, String currentUserId) {
        return userLowId.equals(currentUserId) ? userHighId : userLowId;
    }
}
