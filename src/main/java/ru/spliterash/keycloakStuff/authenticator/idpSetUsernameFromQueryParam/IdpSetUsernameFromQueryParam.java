package ru.spliterash.keycloakStuff.authenticator.idpSetUsernameFromQueryParam;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;

import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;

/**
 * Записывает параметр из урла в контекст сессии, потому что, а почему нет???
 */
public class IdpSetUsernameFromQueryParam extends AbstractIdpAuthenticator {
    public static final String QUERY_FIELD_CONFIG_VALUE = "query_field";

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        String from = null;
        var authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig != null) {
            Map<String, String> config = authenticatorConfig.getConfig();
            if (config != null)
                from = config.get(QUERY_FIELD_CONFIG_VALUE);

        }
        if (from == null || from.isBlank()) {
            from = "username";
        }
        var note = context.getAuthenticationSession().getClientNote(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + from);
        if (note != null && !note.isBlank()) {
            serializedCtx.setUsername(note);
            serializedCtx.setModelUsername(note);

            serializedCtx.saveToAuthenticationSession(
                    context.getAuthenticationSession(),
                    BROKERED_CONTEXT_NOTE
            );
        }

        context.success();
    }

    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {

    }


    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {

    }

    @Override
    public void close() {

    }
}
