package pl.kalin.dreamlog.support

import org.apache.hc.client5.http.config.RequestConfig
import org.apache.hc.client5.http.cookie.BasicCookieStore
import org.apache.hc.client5.http.cookie.CookieStore
import org.apache.hc.client5.http.cookie.StandardCookieSpec
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate

class SessionRestClient {

    protected final RestTemplate rest
    protected final String baseUrl

    private final CookieStore cookieStore

    SessionRestClient(TestRestTemplate template, String baseUrl) {
        this.baseUrl = baseUrl
        this.cookieStore = new BasicCookieStore()

        RequestConfig requestConfig = RequestConfig.custom()
            .setCookieSpec(StandardCookieSpec.RELAXED)
            .build()

        CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setDefaultCookieStore(cookieStore)
            .build()

        RestTemplate source = template.getRestTemplate()
        this.rest = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient))
        this.rest.setMessageConverters(new ArrayList<>(source.getMessageConverters()))
        this.rest.setErrorHandler(source.getErrorHandler())
        this.rest.setUriTemplateHandler(source.getUriTemplateHandler())

        def interceptors = new ArrayList<>(source.getInterceptors())
        interceptors.add(new CsrfTokenInterceptor())
        this.rest.setInterceptors(interceptors)
    }

    protected <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return rest.exchange(resolve(path), HttpMethod.GET, HttpEntity.EMPTY, responseType)
    }

    protected <T> ResponseEntity<T> delete(String path, Class<T> responseType) {
        return rest.exchange(resolve(path), HttpMethod.DELETE, HttpEntity.EMPTY, responseType)
    }

    protected <T> ResponseEntity<T> submitForm(String path, MultiValueMap<String, String> body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED)
        return rest.exchange(resolve(path), HttpMethod.POST, new HttpEntity<>(body, headers), responseType)
    }

    protected <T> ResponseEntity<T> json(HttpMethod method, String path, Object payload, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        HttpEntity<?> entity = payload != null ? new HttpEntity<>(payload, headers) : new HttpEntity<>(headers)
        return rest.exchange(resolve(path), method, entity, responseType)
    }

    protected CookieStore cookieStore() {
        return cookieStore
    }

    private String resolve(String path) {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        return "${baseUrl}${path}"
    }

    private void fetchCsrfToken() {
        try {
            rest.exchange(resolve("/api/me"), HttpMethod.GET, HttpEntity.EMPTY, Map)
        } catch (Exception ignored) {
            // Request may return 401 before login, but cookie repository still updates.
        }
    }

    private String currentCsrfToken() {
        def cookie = cookieStore.getCookies().find { it.name == "XSRF-TOKEN" }
        return cookie?.value
    }

    private class CsrfTokenInterceptor implements ClientHttpRequestInterceptor {
        private final Set<String> csrfMethods = Set.of("POST", "PUT", "PATCH", "DELETE")

        @Override
        ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                     ClientHttpRequestExecution execution) throws IOException {
            def methodName = request.method?.name()
            if (methodName && csrfMethods.contains(methodName)) {
                def token = currentCsrfToken()
                if (!token) {
                    fetchCsrfToken()
                    token = currentCsrfToken()
                }
                if (token) {
                    request.headers.set("X-XSRF-TOKEN", token)
                }
            }
            return execution.execute(request, body)
        }
    }
}
