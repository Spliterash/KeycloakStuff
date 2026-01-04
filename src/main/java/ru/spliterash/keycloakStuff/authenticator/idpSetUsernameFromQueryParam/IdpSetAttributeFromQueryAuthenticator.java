package ru.spliterash.keycloakStuff.authenticator.idpSetUsernameFromQueryParam;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Map;

import static org.keycloak.models.Constants.CFG_DELIMITER_PATTERN;
import static org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX;
import static ru.spliterash.keycloakStuff.authenticator.idpSetUsernameFromQueryParam.IdpSetAttributeFromQueryAuthenticatorFactory.PROVIDER_ID;

/**
 * Записывает параметр из урла в контекст сессии, потому что, а почему нет???
 */
@Slf4j
public class IdpSetAttributeFromQueryAuthenticator extends AbstractIdpAuthenticator {
    public static final String QUERY_FIELD_MAPPING = "field_mapping";

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
        String value = null;
        var authenticatorConfig = context.getAuthenticatorConfig();
        if (authenticatorConfig != null) {
            Map<String, String> config = authenticatorConfig.getConfig();
            if (config != null)
                value = config.get(QUERY_FIELD_MAPPING);

        }
        if (value == null || value.isBlank()) {
            context.success();
            return;
        }
        var list = CFG_DELIMITER_PATTERN.split(value);
        boolean any = false;
        for (String line : list) {
            var split = line.split(":", 2);
            if (split.length != 2) {
                log.warn("Failed to split {} authenticator line {}", PROVIDER_ID, line);
                continue;
            }

            var note = context.getAuthenticationSession().getClientNote(LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + split[0]);
            if (note != null && !note.isBlank()) {
                serializedCtx.setSingleAttribute(split[1], note);
                any = true;
            }
        }
        if (any)
            serializedCtx.saveToAuthenticationSession(
                    context.getAuthenticationSession(),
                    BROKERED_CONTEXT_NOTE
            );

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
