package io.rxbridge.enrollment.common.web;

import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

public final class TestHelpers {

    public static final UUID PATIENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID MEDICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID ENROLLMENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private TestHelpers() { }

    public static MockHttpServletRequestBuilder postJson(
        String url,
        Object id,
        String body
    ) {
        return post(url, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    public static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    public static MockHttpServletRequestBuilder patchJson(
        String url,
        Object id,
        String body
    ) {
        return patch(url, id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    public static MockHttpServletRequestBuilder patchJson(String url, String body) {
        return patch(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body);
    }

    public static <T> T withId(T t, UUID id) {
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    public static String jsonSerialize(JsonMapper mapper, Object value) {
        return mapper.writeValueAsString(value);
    }

    public static UUID anyValidUUID() {
        return argThat(uuid -> uuid != null && !uuid.equals(new UUID(0L, 0L)));
    }

}
