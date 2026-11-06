package com.ttcsn.common.security;

/**
 * Vai trò dùng chung trong JWT claim {@code role} và các quyết định phân quyền.
 * Một chỗ duy nhất để thêm vai trò mới thay vì rải chuỗi "ADMIN"/"CUSTOMER".
 */
public final class Roles {

    public static final String ADMIN = "ADMIN";
    public static final String CUSTOMER = "CUSTOMER";

    private Roles() {
    }

    public static boolean isAdmin(String role) {
        return ADMIN.equals(role);
    }

    public static boolean isCustomer(String role) {
        return CUSTOMER.equals(role);
    }
}
