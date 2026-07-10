package vn.thathinh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E flow: đăng ký → join topic → chat → thả thính → báo cáo → admin xử lý.
 * Cần MongoDB chạy tại localhost:27017 (database: thathinh_e2e_test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndFlowIntegrationTest {

    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);
    private static String tokenA;
    private static String tokenB;
    private static String topicId;
    private static String flirtSessionId;
    private static String reportId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void registerTwoUsers() throws Exception {
        tokenA = registerUser(
                "user_a_" + RUN_ID + "@test.thathinh.vn",
                "nick_a_" + RUN_ID,
                "MALE");
        tokenB = registerUser(
                "user_b_" + RUN_ID + "@test.thathinh.vn",
                "nick_b_" + RUN_ID,
                "FEMALE");

        assertThat(tokenA).isNotBlank();
        assertThat(tokenB).isNotBlank();
    }

    @Test
    @Order(2)
    void joinTopicAndChat() throws Exception {
        topicId = fetchFirstTopicId(tokenA);

        mockMvc.perform(post("/api/topics/{id}/join", topicId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.joined").value(true));

        mockMvc.perform(post("/api/topics/{id}/messages", topicId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Xin chào từ user A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Xin chào từ user A"));

        mockMvc.perform(post("/api/topics/{id}/join", topicId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk());

        MvcResult messages = mockMvc.perform(get("/api/topics/{id}/messages", topicId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(messages.getResponse().getContentAsString()).get("data");
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThanOrEqualTo(1);
        assertThat(list.get(0).get("content").asText()).contains("Xin chào");
    }

    @Test
    @Order(3)
    void flirtMatchAndChat() throws Exception {
        mockMvc.perform(post("/api/flirt/start")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        MvcResult matchResult = mockMvc.perform(post("/api/flirt/start")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MATCHED"))
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andReturn();

        flirtSessionId = objectMapper.readTree(matchResult.getResponse().getContentAsString())
                .get("data").get("sessionId").asText();

        mockMvc.perform(post("/api/flirt/{sessionId}/messages", flirtSessionId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hi đối tác!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Hi đối tác!"));
    }

    @Test
    @Order(4)
    void reportFlirtSession() throws Exception {
        mockMvc.perform(post("/api/flirt/{sessionId}/report", flirtSessionId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Test báo cáo E2E\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(5)
    void reportedUserCanFlirtWhilePending() throws Exception {
        // User B bị báo cáo (A là reporter) — báo cáo vẫn PENDING, B vẫn thả thính được
        mockMvc.perform(get("/api/user/me/account-status")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flirtAllowed").value(true))
                .andExpect(jsonPath("$.data.banned").value(false))
                .andExpect(jsonPath("$.data.pendingReportsAgainstMe").value(1));

        mockMvc.perform(post("/api/flirt/start")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        mockMvc.perform(post("/api/flirt/cancel")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    void adminReviewsReport() throws Exception {
        String adminToken = login("admin@thathinh.vn", "Admin@123");

        MvcResult reports = mockMvc.perform(get("/api/admin/reports")
                        .param("status", "PENDING")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(reports.getResponse().getContentAsString()).get("data");
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThanOrEqualTo(1);

        reportId = list.get(0).get("id").asText();

        mockMvc.perform(post("/api/admin/reports/{id}/review", reportId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "REVIEWED",
                                "note", "E2E test reviewed"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").exists());
    }

    private String registerUser(String email, String nickname, String gender) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "Test@123456",
                "nickname", nickname,
                "gender", gender,
                "birthDate", LocalDate.of(2000, 1, 1).toString()
        ));

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", password));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String fetchFirstTopicId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/topics")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andReturn();

        JsonNode topics = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        assertThat(topics.size()).isGreaterThan(0);
        return topics.get(0).get("id").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
