package com.example.Smart_E_Commerce_System.config;

/**
 * All cache names in one place, so a typo is a compile error instead of a silent new cache.
 * The same names are listed in application.yml (spring.cache.cache-names).
 */
public final class CacheNames {

    /** ProductResponse by product id. */
    public static final String PRODUCTS = "products";

    /** CategoryResponse by category id. */
    public static final String CATEGORIES = "categories";

    /** Pages of the category list, keyed by the Pageable (page, size, sort). */
    public static final String CATEGORY_LISTS = "categoryLists";

    /** UserResponse (user profile) by user id. */
    public static final String USERS = "users";

    /** Report results, keyed by report name + parameters. */
    public static final String REPORTS = "reports";

    private CacheNames() {
    }
}