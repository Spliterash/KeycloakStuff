package ru.spliterash.keycloakStuff.account;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.events.EventBuilder;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProvider;
import org.keycloak.protocol.oidc.AccessTokenIntrospectionProviderFactory;
import org.keycloak.protocol.oidc.TokenIntrospectionProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.cors.Cors;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resources.account.CorsPreflightService;
import org.keycloak.services.util.UserSessionUtil;

import java.util.List;

/**
 * Класс представлят полную копипасту org.keycloak.services.resources.account.AccountLoader
 * <p>
 * Почему есть AdminRealmResourceProvider, но нет AccountResourceProvider? НЕПОРЯДОК
 */
@JBossLog
@RequiredArgsConstructor
public class AccountRestServiceExtensionProvider implements RealmResourceProvider {
    private final KeycloakSession session;

    public Object getResource() {
        RealmModel realm = session.getContext().getRealm();
        ClientModel client = getAccountManagementClient(realm);

        HttpRequest request = session.getContext().getHttpRequest();
        HttpHeaders headers = session.getContext().getRequestHeaders();
        MediaType content = headers.getMediaType();
        List<MediaType> accepts = headers.getAcceptableMediaTypes();
        UriInfo uriInfo = session.getContext().getUri();
        EventBuilder event = new EventBuilder(session.getContext().getRealm(), session, session.getContext().getConnection());


        if (request.getHttpMethod().equals(HttpMethod.OPTIONS)) {
            return new CorsPreflightService();
        } else if ((accepts.contains(MediaType.APPLICATION_JSON_TYPE) || MediaType.APPLICATION_JSON_TYPE.equals(content)) && !uriInfo.getPath().endsWith("keycloak.json")) {
            return getAccountRestService(event, client);
        } else {
            throw new NotFoundException();
        }
    }

    private AccountRestServiceExtension getAccountRestService(EventBuilder event, ClientModel client) {
        AuthenticationManager.AuthResult authResult = new AppAuthManager.BearerTokenAuthenticator(session)
                .authenticate();
        if (authResult == null) {
            throw new NotAuthorizedException("Bearer token required");
        }

        AccessToken accessToken = authResult.getToken();

        UserSessionUtil.checkTokenIssuedAt(client.getRealm(), accessToken, authResult.getSession(), event, authResult.getClient());

        if (accessToken.getAudience() == null || accessToken.getResourceAccess(client.getClientId()) == null) {
            // transform for introspection to get the required claims
            AccessTokenIntrospectionProvider provider = (AccessTokenIntrospectionProvider) session.getProvider(TokenIntrospectionProvider.class,
                    AccessTokenIntrospectionProviderFactory.ACCESS_TOKEN_TYPE);
            accessToken = provider.transformAccessToken(accessToken, authResult.getSession());
        }

        if (!accessToken.hasAudience(client.getClientId())) {
            throw new NotAuthorizedException("Invalid audience for client " + client.getClientId());
        }

        Auth auth = new Auth(session.getContext().getRealm(), accessToken, authResult.getUser(), client, authResult.getSession(), false);

        Cors.builder().allowedOrigins(auth.getToken()).allowedMethods("GET", "PUT", "POST", "DELETE").auth().add();

        if (authResult.getUser().getServiceAccountClientLink() != null) {
            throw new NotAuthorizedException("Service accounts are not allowed to access this service");
        }

        return new AccountRestServiceExtension(session, auth, event);
    }

    private ClientModel getAccountManagementClient(RealmModel realm) {
        ClientModel client = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (client == null || !client.isEnabled()) {
            log.debug("account management not enabled");
            throw new NotFoundException("account management not enabled");
        }
        return client;
    }

    @Override
    public void close() {
    }
}
