package com.promptvault.api.support;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Optional;

/**
 * Http client wrapper for integration tests that respects the SPA CSRF contract:
 * any response (even an unauthenticated 401 GET) renders the {@code XSRF-TOKEN} cookie,
 * and state-changing requests must send it back in the {@code X-XSRF-TOKEN} header.
 *
 * Business integration tests suppress CSRF via
 * {@code @SpringBootTest(properties = "prompt-vault.security.csrf-enabled=false")}; only
 * the dedicated security tests exercise the full contract through this client.
 */
public final class CsrfAwareTestClient {

    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private final HttpClient httpClient;
    private final CookieManager cookieManager;
    private final URI baseUri;

    private CsrfAwareTestClient(URI baseUri) {
        this.cookieManager = new CookieManager();
        this.httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).build();
        this.baseUri = baseUri;
    }

    public static CsrfAwareTestClient create(URI baseUri) {
        return new CsrfAwareTestClient(baseUri);
    }

    public HttpResponse<String> send(HttpRequest.Builder requestBuilder) throws Exception {
        HttpRequest request = requestBuilder.build();
        HttpRequest withCsrf = applyCsrf(request);
        return httpClient.send(withCsrf, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<Void> sendDiscarding(HttpRequest.Builder requestBuilder) throws Exception {
        HttpRequest request = requestBuilder.build();
        HttpRequest withCsrf = applyCsrf(request);
        return httpClient.send(withCsrf, HttpResponse.BodyHandlers.discarding());
    }

    public URI uri(String path) {
        return baseUri.resolve(path);
    }

    public String csrfTokenOrNull() {
        return csrfCookieValue().orElse(null);
    }

    public void clearCookies() {
        cookieManager.getCookieStore().removeAll();
    }

    /** Ignores the CSRF cookie for the next state-changing request (simulates an attacker's context). */
    public void clearCsrfCookie() {
        csrfSuppressed = true;
    }

    private boolean csrfSuppressed;

    private HttpRequest applyCsrf(HttpRequest request) {
        if (!isStateChanging(request.method())) {
            return request;
        }
        if (csrfSuppressed) {
            csrfSuppressed = false;
            return request;
        }
        if (csrfCookieValue().isEmpty()) {
            return request;
        }
        HttpRequest.Builder builder = request.newBuilder(request.uri());
        String[] headers = flattenHeaders(request);
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return builder
                .method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                .header(CSRF_HEADER_NAME, csrfCookieValue().orElseThrow())
                .build();
    }

    private static String[] flattenHeaders(HttpRequest request) {
        return request.headers().map().entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> new String[] { entry.getKey(), value }))
                .flatMap(Arrays::stream)
                .toArray(String[]::new);
    }

    private Optional<String> csrfCookieValue() {
        return cookieManager.getCookieStore().getCookies().stream()
                .filter(cookie -> CSRF_COOKIE_NAME.equals(cookie.getName()))
                .map(HttpCookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static boolean isStateChanging(String method) {
        return !"GET".equals(method) && !"HEAD".equals(method) && !"OPTIONS".equals(method);
    }
}