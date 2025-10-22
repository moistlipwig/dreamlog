package pl.kalin.dreamlog

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus

/**
 * Integration test for OpenAPI documentation endpoint with real HTTP server.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiSpec extends IntegrationSpec {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    String baseUrl() {
        "http://localhost:${port}"
    }

    def "should expose OpenAPI specification"() {
        when: "requesting OpenAPI docs"
        def response = restTemplate.getForEntity("${baseUrl()}/v3/api-docs", Map)

        then: "OpenAPI spec is returned"
        response.statusCode == HttpStatus.OK
        response.body.openapi != null
    }
}
