package ru.spliterash.keycloakStuff.account;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.*;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.managers.UserConsentManager;

import java.util.List;

@JBossLog
public class AccountRestServiceExtension {
    private final HttpRequest request;

    protected final HttpHeaders headers;

    protected final ClientConnection clientConnection;

    private final KeycloakSession session;
    private final EventBuilder event;
    private final Auth auth;

    private final RealmModel realm;
    private final UserModel user;

    public AccountRestServiceExtension(KeycloakSession session, Auth auth, EventBuilder event) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.auth = auth;
        this.realm = auth.getRealm();
        this.user = auth.getUser();
        this.event = event;
        event.client(auth.getClient()).user(auth.getUser());
        this.request = session.getContext().getHttpRequest();
        this.headers = session.getContext().getRequestHeaders();
    }

    /**
     * Тотально выбить приложение, даже внутренее, пошло оно нафиг
     */
    @Path("/applications/{clientId}/access")
    @DELETE
    public Response delete(final @PathParam("clientId") String clientId) {
        auth.requireOneOf(AccountRoles.MANAGE_ACCOUNT, AccountRoles.MANAGE_CONSENT);
        event.event(EventType.REVOKE_GRANT);

        ClientModel client = realm.getClientByClientId(clientId);
        if (client == null) {
            String msg = String.format("No client with clientId: %s found.", clientId);
            event.error(msg);
            throw ErrorResponse.error(msg, Response.Status.NOT_FOUND);
        }

        auth.getSession().removeAuthenticatedClientSessions(List.of(client.getId()));
        // Чтобы два раза не дёргать, сделаем всё сразу
        UserConsentManager.revokeConsentToClient(session, client, user);

        event.detail("DELETE_CLIENT_FROM_SESSION", client.getClientId()).success();

        return Response.noContent().build();
    }
}
