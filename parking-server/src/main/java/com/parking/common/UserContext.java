package com.parking.common;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_PHONE = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();

    public static void setUserId(Long id) { USER_ID.set(id); }
    public static Long getUserId() { return USER_ID.get(); }
    public static void setPhone(String phone) { USER_PHONE.set(phone); }
    public static String getPhone() { return USER_PHONE.get(); }
    public static void setRole(String role) { USER_ROLE.set(role); }
    public static String getRole() { return USER_ROLE.get(); }
    public static void clear() { USER_ID.remove(); USER_PHONE.remove(); USER_ROLE.remove(); }
}
