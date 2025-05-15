package ru.spliterash.keycloakStuff.validation;

import com.google.auto.service.AutoService;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.validate.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@AutoService(ValidatorFactory.class)
public class OnlyRegistrationValidator extends AbstractStringValidator implements ConfiguredProvider {
    private static final String ONLY_REGISTRATION_KEY = "spliterash.validation.only-registration-error";

    private static Set<UserProfileContext> allowedContexts = Set.of(
            UserProfileContext.REGISTRATION,
            UserProfileContext.IDP_REVIEW,
            UserProfileContext.USER_API
    );

    @Override
    public String getHelpText() {
        return "Only registration attribute";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (!(context instanceof UserProfileAttributeValidationContext castedContext))
            throw new IllegalStateException("Only UserProfileAttributeValidationContext supported");
        if (allowedContexts.contains(castedContext.getAttributeContext().getContext())) return;

        String attributeKey = castedContext.getAttributeContext().getMetadata().getName();

        UserModel targetUser = castedContext.getAttributeContext().getUser();
        String actualValue = targetUser.getFirstAttribute(attributeKey);

        if (Objects.equals(normalizeString(value), normalizeString(actualValue))) return;

        context.addError(new ValidationError(getId(), inputHint, ONLY_REGISTRATION_KEY));
    }

    private String normalizeString(String str) {
        if (str == null) return null;
        if (str.isEmpty()) return null;

        return str;
    }

    @Override
    public String getId() {
        return "registration-only";
    }
}
