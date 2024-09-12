package ru.spliterash.keycloakStuff.validation;

import com.google.auto.service.AutoService;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@AutoService(ValidatorFactory.class)
public class CooldownValidator extends AbstractStringValidator implements ConfiguredProvider {
    private static final String MESSAGE_COOLDOWN_KEY = "spliterash.validation.cooldown-validation-error";


    private static final String COOLDOWN_AMOUNT_IN_SECONDS = "cooldown_amount_in_seconds";
    private static final String LAST_UPDATE_ATTRIBUTE_KEY_PREFIX = "_updated_at";


    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(COOLDOWN_AMOUNT_IN_SECONDS);
        property.setLabel("Amount");
        property.setHelpText("Cooldown amount in seconds");
        property.setRequired(true);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }


    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (!(context instanceof UserProfileAttributeValidationContext castedContext))
            throw new IllegalStateException("Only UserProfileAttributeValidationContext supported");
        if (castedContext.getAttributeContext().getContext().isAdminContext()) return;

        String attributeKey = castedContext.getAttributeContext().getMetadata().getName();

        UserModel targetUser = castedContext.getAttributeContext().getUser();
        String actualValue = targetUser.getFirstAttribute(attributeKey);
        if (actualValue == null || actualValue.equals(value)) return;

        String lastUpdateAttributeKey = attributeKey + LAST_UPDATE_ATTRIBUTE_KEY_PREFIX;
        Integer cooldownAmountInSeconds = config.getInt(COOLDOWN_AMOUNT_IN_SECONDS);
        if (cooldownAmountInSeconds == null)
            throw new IllegalArgumentException(COOLDOWN_AMOUNT_IN_SECONDS + " is null");
        // Получаем информацию о последнем изменении
        String lastUpdateStr = targetUser.getFirstAttribute(lastUpdateAttributeKey);

        Instant lastUpdate = lastUpdateStr != null ? Instant.parse(lastUpdateStr) : Instant.EPOCH;
        Instant now = Instant.now();


        long timeLeftInMillis = lastUpdate.plusSeconds(cooldownAmountInSeconds).toEpochMilli() - Instant.now().toEpochMilli();

        // Проверяем, прошло ли больше 24 часов с последнего изменения
        if (timeLeftInMillis > 0) {
            long timeLeftInSeconds = timeLeftInMillis / 1000;
            context.addError(new ValidationError(getId(), inputHint, MESSAGE_COOLDOWN_KEY, timeLeftInSeconds));
        } else {
            targetUser.setSingleAttribute(lastUpdateAttributeKey, now.toString());
        }
    }

    @Override
    protected boolean skipValidation(Object value, ValidatorConfig config) {
        return false;
    }

    @Override
    public String getId() {
        return "cooldown-validator";
    }

    @Override
    public String getHelpText() {
        return "Attribute change cooldown";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}