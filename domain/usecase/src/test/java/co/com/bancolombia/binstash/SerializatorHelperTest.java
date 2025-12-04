package co.com.bancolombia.binstash;


import co.com.bancolombia.binstash.demo.Person;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SerializatorHelperTest {

    @Mock
    ObjectMapper objectMapper;

    @SneakyThrows
    @Test
    @DisplayName("Should handle error on write json")
    void testRaiseError() {
        JacksonException e = new JacksonException("Dummy Error") {};
        doThrow(e).when(objectMapper).writeValueAsString(any());
        SerializatorHelper<Person> sHelper = new SerializatorHelper<>(objectMapper);
        String s = sHelper.serialize(new Person());
        assertNull(s);
        verify(objectMapper).writeValueAsString(any(Person.class));
    }

    @SneakyThrows
    @Test
    @DisplayName("Should handle error on parse json")
    void testRaiseError2() {
        JacksonException e = new JacksonException("Dummy Error") {};
        doThrow(e).when(objectMapper).readValue(anyString(), any(Class.class));
        SerializatorHelper<Person> sHelper = new SerializatorHelper<>(objectMapper);
        sHelper.deserializeTo("{}", Person.class);
        verify(objectMapper).readValue(anyString(), any(Class.class));
    }

    @SneakyThrows
    @Test
    @DisplayName("Should handle error on parse json II")
    void testRaiseError3() {
        JacksonException e = new JacksonException("Dummy Error") {};
        doThrow(e).when(objectMapper).readValue(anyString(), any(TypeReference.class));
        SerializatorHelper<Person> sHelper = new SerializatorHelper<>(objectMapper);
        Person p = sHelper.deserializeWith("{}", new TypeReference<>() {});
        assertNull(p);
        verify(objectMapper).readValue(anyString(), any(TypeReference.class));
    }

    @SneakyThrows
    @Test
    @DisplayName("Should handle null args")
    void testHandleNullArgs() {
        SerializatorHelper<Person> sHelper = new SerializatorHelper<>(objectMapper);

        assertNull(sHelper.serialize(null));
        assertNull(sHelper.deserializeTo(null, Person.class));
        assertNull(sHelper.deserializeTo("pparker", null));
        assertNull(sHelper.deserializeWith(null, new TypeReference<>() {}));
        assertNull(sHelper.deserializeWith("pparker", null));

        verify(objectMapper, times(0)).writeValueAsString(any(Person.class));
        verify(objectMapper, times(0)).readValue(anyString(), any(Class.class));
        verify(objectMapper, times(0)).readValue(anyString(), any(TypeReference.class));
    }
}
