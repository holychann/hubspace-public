package com.example.backend.domain.response.helper;

public class StringNomalize {
    static private String normalizeKey(String key) {
        if (key == null) return null;
        return key.replace("\uFEFF", "").trim();
    }
}
