package com.example.treasurehuntapplication;

import android.util.Patterns;

/**
 * Normalizes and validates login and signup fields.
 * <p>
 * Firebase email flows require Nottingham Trent University student addresses: the substring after
 * the final {@code @} must equal {@link #REQUIRED_EMAIL_DOMAIN} (personal domains such as
 * {@code gmail.com} or {@code outlook.com}, staff domains like {@code ntu.ac.uk} without {@code my.},
 * and subdomains such as {@code mail.my.ntu.ac.uk} are rejected).
 */
public final class AuthInputValidator {

    public static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * Host part (domain) that {@link #isAllowedNtuLoginEmail} requires after the last {@code @}.
     * Compares against {@link #normalizeEmail(String)} output, so matching is case-insensitive.
     */
    public static final String REQUIRED_EMAIL_DOMAIN = "my.ntu.ac.uk";

    private AuthInputValidator() {
    }

    public static String normalizeEmail(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase();
    }

    public static boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Whether {@code email} may be used for Firebase sign-in, password reset, or registration.
     * <ul>
     *   <li>Must pass {@link #isValidEmail(String)} (Android {@link android.util.Patterns#EMAIL_ADDRESS}).</li>
     *   <li>After {@link #normalizeEmail(String)}, the substring after the <em>last</em> {@code @} must equal
     *       {@link #REQUIRED_EMAIL_DOMAIN} exactly (case-insensitive because of normalization).</li>
     * </ul>
     * Examples: {@code alice@my.ntu.ac.uk} and {@code Bob@MY.NTU.AC.UK} allowed;
     * {@code x@gmail.com}, {@code x@outlook.com}, {@code x@ntu.ac.uk} (no {@code my.}), and
     * {@code x@mail.my.ntu.ac.uk} (subdomain, not exact host) are not allowed.
     * {@code lastIndexOf('@')} handles local parts that themselves contain {@code @}.
     */
    public static boolean isAllowedNtuLoginEmail(String email) {
        if (!isValidEmail(email)) {
            return false;
        }
        String e = normalizeEmail(email);
        int at = e.lastIndexOf('@');
        if (at < 1) {
            return false;
        }
        return REQUIRED_EMAIL_DOMAIN.equals(e.substring(at + 1));
    }

    public static boolean isValidPasswordLength(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}
