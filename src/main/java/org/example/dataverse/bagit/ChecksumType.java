package org.example.dataverse.bagit;

import java.util.Arrays;

public enum ChecksumType {

    MD5("MD5"),
    SHA1("SHA-1"),
    SHA256("SHA-256"),
    SHA512("SHA-512");

    private final String text;

    private ChecksumType(final String text) {
        this.text = text;
    }

    public static ChecksumType fromString(String text) {
        if (text != null) {
            for (ChecksumType checksumType : ChecksumType.values()) {
                if (text.equals(checksumType.text)) {
                    return checksumType;
                }
            }
        }
        throw new IllegalArgumentException("ChecksumType must be one of these values: " + Arrays.asList(ChecksumType.values()) + ".");
    }

    @Override
    public String toString() {
        return text;
    }
}
