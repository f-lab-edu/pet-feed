package com.petfeed.contracts.http;

/** Versioned HTTP prefixes shared by providers, clients, and gateway security. */
public final class ApiPaths {

    public static final String API_ROOT = "/api";
    public static final String API_V1 = API_ROOT + "/v1";
    public static final String INTERNAL_V1 = "/internal/v1";

    private ApiPaths() {
    }
}
