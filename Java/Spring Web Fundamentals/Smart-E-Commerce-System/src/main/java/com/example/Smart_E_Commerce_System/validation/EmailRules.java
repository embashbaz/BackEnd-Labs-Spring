package com.example.Smart_E_Commerce_System.validation;

import java.util.regex.Pattern;

/** Single source of truth for what counts as a valid email in this application. */
public final class EmailRules {

    private EmailRules() {}

    /**
     * local-part@domain.tld
     *  - no consecutive dots anywhere
     *  - local part: letters, digits and . _ % + -
     *  - domain: one or more labels followed by a TLD of at least 2 letters
     */
    public static final String REGEX =
            "^(?!.*\\.\\.)[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$";

    public static final Pattern PATTERN = Pattern.compile(REGEX);
}