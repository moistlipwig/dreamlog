package pl.kalin.dreamlog.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import pl.kalin.dreamlog.IntegrationSpec
import pl.kalin.dreamlog.user.FederatedIdentityRepository
import pl.kalin.dreamlog.user.User
import pl.kalin.dreamlog.user.UserRepository
import pl.kalin.dreamlog.user.service.OAuth2SuccessHandler

/**
 * Unit test for OAuth2SuccessHandler.
 * Verifies that users are created/updated in database after successful OAuth2 authentication.
 */
class OAuth2SuccessHandlerSpec extends IntegrationSpec {

    @Autowired
    OAuth2SuccessHandler oAuth2SuccessHandler

    @Autowired
    UserRepository userRepository

    @Autowired
    FederatedIdentityRepository federatedIdentityRepository

    HttpServletRequest request = Mock(HttpServletRequest)
    HttpServletResponse response = Mock(HttpServletResponse)

    def setup() {
        // Clean database before each test
        federatedIdentityRepository.deleteAll()
        userRepository.deleteAll()
    }

    def "should create user and federated identity on first Google OAuth login"() {
        given: "Google OAuth user attributes"
        def googleSub = "google-user-123456"
        def email = "testuser@gmail.com"
        def name = "Test User"

        and: "OAuth2 authentication token"
        def oAuth2User = createOAuth2User(googleSub, email, name)
        def authentication = createAuthentication(oAuth2User, "google")

        when: "Success handler is invoked"
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication)

        then: "User is created in database"
        def users = userRepository.findAll()
        users.size() == 1
        def user = users[0]
        user.email == email
        user.name == name
        user.emailVerified == true

        and: "Federated identity is created"
        def identities = federatedIdentityRepository.findAll()
        identities.size() == 1
        def identity = identities[0]
        identity.provider == "google"
        identity.providerUserId == googleSub
        identity.user.id == user.id

        and: "Last login timestamp is set"
        user.lastLoginAt != null
    }

    def "should update last login on subsequent Google OAuth login"() {
        given: "Google OAuth user attributes"
        def googleSub = "google-user-789"
        def email = "existing@gmail.com"
        def name = "Existing User"

        and: "First login"
        def oAuth2User = createOAuth2User(googleSub, email, name)
        def authentication = createAuthentication(oAuth2User, "google")
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication)

        and: "Get user after first login"
        def existingUser = userRepository.findByEmail(email).get()
        def existingUserId = existingUser.id
        def firstLoginTime = existingUser.lastLoginAt

        when: "User logs in again"
        Thread.sleep(100) // Ensure different timestamp
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication)

        then: "Still only one user exists"
        userRepository.count() == 1

        and: "Still only one federated identity exists"
        federatedIdentityRepository.count() == 1

        and: "Last login timestamp is updated"
        def updatedUser = userRepository.findById(existingUserId).get()
        updatedUser.lastLoginAt > firstLoginTime
    }

    def "should link Google OAuth to existing email-registered user"() {
        given: "Existing user registered with email/password"
        def email = "linktest@example.com"
        def existingUser = userRepository.save(
            User.builder()
                .email(email)
                .name("Link Test")
                .emailVerified(false)
                .build()
        )

        and: "Google OAuth login with same email"
        def googleSub = "google-link-user-456"
        def oAuth2User = createOAuth2User(googleSub, email, "Link Test")
        def authentication = createAuthentication(oAuth2User, "google")

        when: "Success handler is invoked"
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication)

        then: "Still only one user exists"
        userRepository.count() == 1

        and: "Federated identity is created and linked to existing user"
        def identities = federatedIdentityRepository.findAll()
        identities.size() == 1
        identities[0].user.id == existingUser.id
        identities[0].provider == "google"
        identities[0].providerUserId == googleSub

        and: "Email verification is updated to true (from OAuth provider)"
        def updatedUser = userRepository.findById(existingUser.id).get()
        updatedUser.emailVerified == true
    }

    def "should handle Facebook OAuth login"() {
        given: "Facebook OAuth user attributes"
        def facebookId = "facebook-user-999"
        def email = "fbuser@example.com"
        def name = "FB User"

        and: "OAuth2 authentication token (Facebook uses 'id' not 'sub')"
        def oAuth2User = createFacebookOAuth2User(facebookId, email, name)
        def authentication = createAuthentication(oAuth2User, "facebook")

        when: "Success handler is invoked"
        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, authentication)

        then: "User is created with facebook provider"
        def users = userRepository.findAll()
        users.size() == 1
        users[0].email == email

        and: "Federated identity has facebook provider"
        def identities = federatedIdentityRepository.findAll()
        identities.size() == 1
        identities[0].provider == "facebook"
        identities[0].providerUserId == facebookId
    }

    // ============================================================================
    // Helper methods
    // ============================================================================

    /**
     * Creates OAuth2User with Google user attributes.
     * Google uses OpenID Connect standard with 'sub' attribute.
     */
    private static OAuth2User createOAuth2User(String sub, String email, String name) {
        def attributes = [
            sub           : sub,
            email         : email,
            name          : name,
            email_verified: true
        ]
        def authorities = []
        return new DefaultOAuth2User(authorities, attributes, "sub")
    }

    /**
     * Creates OAuth2User with Facebook user attributes.
     * Facebook uses 'id' instead of 'sub' for user identification.
     */
    private static OAuth2User createFacebookOAuth2User(String id, String email, String name) {
        def attributes = [
            id   : id,
            email: email,
            name : name
        ]
        def authorities = []
        return new DefaultOAuth2User(authorities, attributes, "id")
    }

    /**
     * Creates OAuth2AuthenticationToken for given provider.
     */
    private static OAuth2AuthenticationToken createAuthentication(OAuth2User oAuth2User, String provider) {
        return new OAuth2AuthenticationToken(
            oAuth2User,
            oAuth2User.getAuthorities(),
            provider // This becomes authorizedClientRegistrationId
        )
    }
}
