package org.kiwiproject.jaxrs.exception;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.extern.slf4j.Slf4j;
import org.kiwiproject.collect.KiwiMaps;
import org.kiwiproject.json.JsonHelper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Map a {@link JaxrsException} to a {@link Response}.
 */
@Slf4j
public class JaxrsExceptionMapper implements ExceptionMapper<JaxrsException> {

    /**
     * The map key under which the list of {@link ErrorMessage} objects resides.
     */
    public static final String KEY_ERRORS = "errors";

    private static final JsonHelper JSON_HELPER = JsonHelper.newDropwizardJsonHelper();

    /**
     * Convert the given {@link JaxrsException} to a response containing a JSON entity.
     *
     * @param exception the exception to convert
     * @return a response
     * @see #buildResponse(JaxrsException)
     */
    @Override
    public Response toResponse(JaxrsException exception) {
        return buildResponse(exception);
    }

    /**
     * Convert the given {@link JaxrsException} to a JSON response.
     *
     * @param exception the exception to convert
     * @return a response
     */
    public static Response buildResponse(JaxrsException exception) {
        return buildResponseBuilder(exception).build();
    }

    /**
     * Convert the given {@link JaxrsException} to a JSON response <em>builder</em>.
     *
     * @param exception the exception to convert
     * @return a response builder
     */
    public static Response.ResponseBuilder buildResponseBuilder(JaxrsException exception) {
        return Response.status(exception.getStatusCode())
                .entity(buildResponseEntity(exception))
                .type(MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Convert the given {@link JaxrsException} to a map that can be used as a JSON response entity.
     * <p>
     * The response entity will contain an "errors" key containing the {@link ErrorMessage} objects.
     * If the exception contains any other data ({@link JaxrsException#getOtherData()}, those are inclued
     * in the response entity.
     *
     * @param exception the exception to convert
     * @return a map that can be used as a response entity
     */
    public static Map<String, Object> buildResponseEntity(JaxrsException exception) {
        var entity = new HashMap<String, Object>();

        if (KiwiMaps.isNotNullOrEmpty(exception.getOtherData())) {
            entity.putAll(exception.getOtherData());
        }

        entity.put(KEY_ERRORS, exception.getErrors());

        return entity;
    }

    /**
     * Convert the given {@link Response} to a {@link JaxrsException}.
     * <p>
     * Attempts to convert a response entity into errors and other data in the resulting exception
     * using {@link #toJaxrsException(int, Map)}.
     *
     * @param response the response to convert
     * @return a new exception instance, or null if the response is null
     * @see #toJaxrsException(int, Map)
     */
    public static JaxrsException toJaxrsException(Response response) {
        if (isNull(response)) {
            return null;
        }

        int status = response.getStatus();

        if (!response.hasEntity()) {
            return new JaxrsException(response.getStatusInfo().getReasonPhrase(), status);
        }

        String entityText;
        try {
            entityText = response.readEntity(String.class);
            if (isBlank(entityText)) {
                return new JaxrsException(response.getStatusInfo().getReasonPhrase(), status);
            }
        } catch (Exception e) {
            LOG.warn("Error reading entity from response", e);
            return new JaxrsException((String) null, status);
        }

        Map<String, Object> entity;
        try {
            entity = JSON_HELPER.toMap(entityText);
        } catch (Exception e) {
            LOG.warn("Error converting response text to Map<String, Object>", e);
            return new JaxrsException(entityText, status);
        }

        return toJaxrsException(status, entity);
    }

    /**
     * Convert the given HTTP status code and an entity into a {@link JaxrsException}.
     * <p>
     * Looks for an entry with key "errors" and a list of objects, and attempts to convert them into
     * {@link ErrorMessage} objects. The conversion from a generic map to an {@link ErrorMessage} is
     * done using {@link ErrorMessage#valueOf(Map)}.
     * <p>
     * Any other entries in the map are converted to "other data" in the exception.
     *
     * @param status the HTTP status
     * @param entity the entity as a map
     * @return a new exception instance
     * @see JaxrsException#setErrors(List)
     * @see JaxrsException#setOtherData(Map)
     */
    public static JaxrsException toJaxrsException(int status, Map<String, Object> entity) {
        if (KiwiMaps.isNullOrEmpty(entity) || !entity.containsKey(KEY_ERRORS)) {
            var ex = new JaxrsException((String) null, status);
            ex.setOtherData(entity);
        }

        try {
            var errorMessages = extractErrorMessages(entity);
            var ex = new JaxrsException(errorMessages, status);

            var otherData = new HashMap<>(entity);
            otherData.remove(KEY_ERRORS);
            otherData.remove(null);
            ex.setOtherData(unmodifiableMap(otherData));

            return ex;
        } catch (Exception e) {
            LOG.warn("Error converting given entity map: {}", entity, e);
            return new JaxrsException((String) null, status);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ErrorMessage> extractErrorMessages(Map<String, Object> entity) {
        if (entity.containsKey(KEY_ERRORS)) {
            var errorObjects = (List<Object>) entity.get(KEY_ERRORS);
            return errorObjects.stream()
                    .map(JaxrsExceptionMapper::toErrorMessageOrNull)
                    .filter(Objects::nonNull)
                    .collect(toUnmodifiableList());
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static ErrorMessage toErrorMessageOrNull(Object obj) {
        if (obj instanceof ErrorMessage) {
            return (ErrorMessage) obj;
        } else if (obj instanceof Map) {
            return ErrorMessage.valueOf((Map<String, Object>) obj);
        }

        return null;
    }
}
