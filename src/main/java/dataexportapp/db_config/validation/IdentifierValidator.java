package dataexportapp.db_config.validation;


import java.util.regex.Pattern;


public final class IdentifierValidator {

    // Oracle unquoted identifiers: start with a letter, then letters/digits/_/$/#.
    // This intentionally rejects quoted identifiers, spaces, and anything
    // containing SQL syntax characters (;, ', --, etc.).
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[A-Za-z][A-Za-z0-9_$#]*$");
    private static final int MAX_LENGTH = 128; // Oracle 12.2+ identifier length limit

    private IdentifierValidator() {
    }

    public static void validate(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Identifier cannot be empty.");
        }
        if (identifier.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Identifier exceeds maximum length of " + MAX_LENGTH + ": " + identifier);
        }
        if (!VALID_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid identifier (letters, digits, _, $, # only, starting with a letter): " + identifier);
        }
    }
}