package com.eaglepoint.workforce.masking;

public final class MaskingUtil {

    private MaskingUtil() {}

    public static String maskToLastFour(String input) {
        if (input == null) return null;
        if (input.length() <= 4) return input;
        return "*".repeat(input.length() - 4) + input.substring(input.length() - 4);
    }

    public static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 1) return email;
        return email.charAt(0) + "*".repeat(at - 1) + email.substring(at);
    }

    public static String maskPhone(String phone) {
        if (phone == null) return null;
        if (phone.length() <= 4) return phone;
        return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
    }

    /** Admin sees full value; non-admin sees masked. */
    public static String mask(String value, boolean isAdmin) {
        return isAdmin ? value : maskToLastFour(value);
    }

    public static String maskEmailByRole(String email, boolean isAdmin) {
        return isAdmin ? email : maskEmail(email);
    }

    public static String maskPhoneByRole(String phone, boolean isAdmin) {
        return isAdmin ? phone : maskPhone(phone);
    }
}
