package com.ibm.capstone.bookstore.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * Who is looking? Guests browse the public catalogue, members (signed-in customers) also see
 * members-only categories, and admins manage the store. Used wherever visibility depends on the caller.
 */
public final class Entitlements {

    public static final List<String> GUEST_VISIBILITY = List.of("PUBLIC");
    public static final List<String> MEMBER_VISIBILITY = List.of("PUBLIC", "MEMBERS");

    private Entitlements() {}

    /** True for any signed-in user. Spring's anonymous token carries a String principal, so it is excluded. */
    public static boolean isMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal;
    }

    public static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return isMember() && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /** Category visibility values the current caller may see. */
    public static List<String> visibleCategories() {
        return isMember() ? MEMBER_VISIBILITY : GUEST_VISIBILITY;
    }
}
