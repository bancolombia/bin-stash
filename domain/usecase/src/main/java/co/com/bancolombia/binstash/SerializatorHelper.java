package co.com.bancolombia.binstash;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;

@Log
@RequiredArgsConstructor
public class SerializatorHelper<T> {

    private final ObjectMapper objectMapper;

    public String serialize(T obj) {
        if (obj == null)
            return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logFailure("serialize", obj.getClass(), e);
            return null;
        }
    }

    public T deserializeTo(String obj, Class<T> clazz) {
        if (obj == null || clazz == null)
            return null;
        try {
            return objectMapper.readValue(obj, clazz);
        } catch (Exception e) {
            logFailure("deserialize", clazz, e);
            return null;
        }
    }

    public T deserializeWith(String obj, TypeReference<? extends T> ref) {
        if (obj == null || ref == null)
            return null;
        try {
            return objectMapper.readValue(obj, ref);
        } catch (Exception e) {
            logFailure("deserialize", ref.getType(), e);
            return null;
        }
    }

    // The exception message is not logged: Jackson includes the offending value in it,
    // which would leak cached data (potentially PII or tokens) into the logs.
    private static void logFailure(String operation, Type type, Exception e) {
        log.warning(() -> String.format("Could not %s value of type %s%s: %s",
                operation, type.getTypeName(), describeLocation(e), e.getClass().getName()));
    }

    // Only the numeric location is included: the property path may contain map keys or other cached data.
    private static String describeLocation(Exception e) {
        if (!(e instanceof JacksonException jacksonException) || jacksonException.getLocation() == null
                || jacksonException.getLocation().getLineNr() <= 0)
            return "";
        return String.format(" at line %d, column %d",
                jacksonException.getLocation().getLineNr(), jacksonException.getLocation().getColumnNr());
    }
}
