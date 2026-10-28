package dev.lab.coolify;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndReadsTask() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"DEMO","payload":"hello coolify"}
                    """))
            .andExpect(status().isAccepted())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andExpect(jsonPath("$.status", is("PENDING")))
            .andReturn();

        String location = created.getResponse().getHeader("Location");
        mockMvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type", is("DEMO")))
            .andExpect(jsonPath("$.payload", is("hello coolify")));
    }

    @Test
    void replaysRequestWithSameIdempotencyKey() throws Exception {
        String request = """
            {"type":"DEMO","payload":"idempotent payload"}
            """;

        MvcResult first = mockMvc.perform(post("/api/tasks")
                .header("Idempotency-Key", "test-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Idempotency-Replayed", "false"))
            .andReturn();

        String firstId = JsonTestHelper.readId(first.getResponse().getContentAsString());

        mockMvc.perform(post("/api/tasks")
                .header("Idempotency-Key", "test-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Idempotency-Replayed", "true"))
            .andExpect(jsonPath("$.id", is(firstId)));
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentRequest() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .header("Idempotency-Key", "test-key-conflict")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"DEMO","payload":"first"}
                    """))
            .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/tasks")
                .header("Idempotency-Key", "test-key-conflict")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"DEMO","payload":"different"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title", is("Idempotency conflict")));
    }

    @Test
    void rejectsInvalidTask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"type":"","payload":""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title", is("Invalid request")));
    }

    @Test
    void exposesHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }


    private static final class JsonTestHelper {

        private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

        private static String readId(String json) throws Exception {
            return OBJECT_MAPPER.readTree(json).get("id").asText();
        }
    }
}
