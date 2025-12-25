package ru.spliterash.keycloakStuff.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ConfiguredProvider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.userprofile.UserProfileAttributeValidationContext;
import org.keycloak.validate.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@AutoService(ValidatorFactory.class)
public class HttpRequestValidator extends AbstractStringValidator implements ConfiguredProvider {

    private static final String URL = "url";
    private static final String HEADERS = "headers";

    private static final String ONLY_ON_UPDATE = "only_on_update";
    private static final String SKIP_EMPTY = "skip_empty";
    private static final String FAIL_PASS = "fail_pass";


    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    static {
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        {
            var property = new ProviderConfigProperty();
            property.setName(URL);
            property.setLabel("Endpoint URL");
            property.setHelpText("Where's check value");
            property.setType(ProviderConfigProperty.STRING_TYPE);
            property.setRequired(true);
            configProperties.add(property);
        }
        {
            var property = new ProviderConfigProperty();
            property.setName(HEADERS);
            property.setLabel("Headers");
            property.setHelpText("Request custom headers, key value by Key: Value");
            property.setType(ProviderConfigProperty.MULTIVALUED_STRING_TYPE);
            property.setRequired(true);
            configProperties.add(property);
        }

        {
            var property = new ProviderConfigProperty();
            property.setName(ONLY_ON_UPDATE);
            property.setLabel("Check only on update");
            property.setHelpText("Run validation only on property update");
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue(true);
            configProperties.add(property);
        }
        {
            var property = new ProviderConfigProperty();
            property.setName(SKIP_EMPTY);
            property.setLabel("Skip if empty");
            property.setHelpText("Skip validation if value empty");
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue(true);
            configProperties.add(property);
        }
        {
            var property = new ProviderConfigProperty();
            property.setName(FAIL_PASS);
            property.setLabel("Fail pass");
            property.setHelpText("Allow validation if target resource not answer (network problem / format)");
            property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
            property.setDefaultValue(false);
            configProperties.add(property);
        }
    }


    @Override
    protected void doValidate(String value, String inputHint, ValidationContext context, ValidatorConfig config) {
        if (!(context instanceof UserProfileAttributeValidationContext castedContext))
            throw new IllegalStateException("Only UserProfileAttributeValidationContext supported");

        String attributeKey = castedContext.getAttributeContext().getMetadata().getName();

        UserModel targetUser = castedContext.getAttributeContext().getUser();
        String actualValue = targetUser.getFirstAttribute(attributeKey);

        Boolean onlyOnUpdate = config.getBoolean(ONLY_ON_UPDATE);
        if (onlyOnUpdate == null) onlyOnUpdate = true;
        if (onlyOnUpdate && (Objects.equals(actualValue, value))) return;


        Boolean skipEmpty = config.getBoolean(SKIP_EMPTY);
        if (skipEmpty == null) skipEmpty = true;
        if (skipEmpty && (value == null || value.isBlank())) return;


        Boolean failPass = config.getBoolean(FAIL_PASS);
        if (failPass == null) failPass = false;

        HttpRequest.Builder request = HttpRequest
                .newBuilder(URI.create(config.getString(URL)));

        request.header("Content-Type", "application/json");
        var headersLines = config.getStringListOrDefault(HEADERS, List.of());
        for (String headersLine : headersLines) {
            var split = headersLine.split(":", 2);
            request.header(split[0].trim(), split[1].trim());
        }
        String body;
        try {
            body = objectMapper.writeValueAsString(new ValidationRequest(attributeKey, actualValue, value));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        request.method("POST", HttpRequest.BodyPublishers.ofString(body));
        ValidationResponse response;
        String bodyRaw = null;
        try {
            HttpResponse<String> requestResponse = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            bodyRaw = requestResponse.body();
            response = objectMapper.readValue(bodyRaw, ValidationResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize json: {}", bodyRaw, e);
            if (!failPass)
                context.addError(new ValidationError(getId(), inputHint, "Json deserialization failed"));
            return;
        } catch (Exception exception) {
            log.error("Failed process http request validator", exception);
            if (!failPass)
                context.addError(new ValidationError(getId(), inputHint, "Failed to process validation request"));
            return;
        }

        if (!response.valid) {
            var message = response.errorMessage;
            if (message == null) message = "Validation failed";

            context.addError(new ValidationError(getId(), inputHint, message));
        }

        // Ура, всё крута
    }

    @Override
    protected boolean skipValidation(Object value, ValidatorConfig config) {
        return false;
    }

    @Override
    public String getId() {
        return "http-request-validator";
    }

    @Override
    public String getHelpText() {
        return "Validation through http";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    private record ValidationRequest(
            String propertyName,
            String before,
            String after
    ) {
    }

    private record ValidationResponse(
            boolean valid,
            String errorMessage
    ) {
    }
}