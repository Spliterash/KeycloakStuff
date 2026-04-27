package ru.spliterash.keycloakStuff.authenticator.blockGoogleWorkspace;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sessions.AuthenticationSessionModel;

public class BlockGoogleWorkspaceAuthenticator implements Authenticator {

    static final String MESSAGE_KEY = "spliterash.authenticator.google-workspace-blocked-error";
    private static final String GOOGLE_PROVIDER_ID = "google";
    private static final String HD_CLAIM = "hd";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(
                authSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);
        if (serializedCtx == null) {
            context.success();
            return;
        }

        BrokeredIdentityContext brokered = serializedCtx.deserialize(context.getSession(), authSession);
        if (brokered == null
                || brokered.getIdpConfig() == null
                || !GOOGLE_PROVIDER_ID.equals(brokered.getIdpConfig().getProviderId())) {
            context.success();
            return;
        }

        Object idTokenObj = brokered.getContextData().get(OIDCIdentityProvider.VALIDATED_ID_TOKEN);
        if (!(idTokenObj instanceof JsonWebToken idToken)) {
            context.success();
            return;
        }

        Object hd = idToken.getOtherClaims().get(HD_CLAIM);
        if (hd == null) {
            context.success();
            return;
        }

        context.getEvent()
                .detail("reason", "google_workspace_blocked")
                .detail("hd", hd.toString())
                .error(Errors.NOT_ALLOWED);

        Response errorPage = context.form()
                .setError(MESSAGE_KEY)
                .createErrorPage(Response.Status.FORBIDDEN);

        context.failureChallenge(AuthenticationFlowError.INVALID_USER, errorPage);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
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
