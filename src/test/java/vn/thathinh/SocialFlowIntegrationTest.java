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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SocialFlowIntegrationTest {

    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);
    private static String tokenA;
    private static String tokenB;
    private static String userBId;
    private static String flirtSessionId;
    private static String friendRequestId;
    private static String conversationId;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    void registerUsers() throws Exception {
        tokenA = registerUser("soc_a_" + RUN_ID + "@test.thathinh.vn", "soc_a_" + RUN_ID, "MALE");
        tokenB = registerUser("soc_b_" + RUN_ID + "@test.thathinh.vn", "soc_b_" + RUN_ID, "FEMALE");
        userBId = fetchUserId(tokenB);
        assertThat(tokenA).isNotBlank();
        assertThat(userBId).isNotBlank();
    }

    @Test
    @Order(2)
    void flirtMatchAndFriendRequest() throws Exception {
        mockMvc.perform(post("/api/flirt/start").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
        MvcResult match = mockMvc.perform(post("/api/flirt/start").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andReturn();
        flirtSessionId = objectMapper.readTree(match.getResponse().getContentAsString())
                .get("data").get("sessionId").asText();

        mockMvc.perform(post("/api/flirt/{id}/friend-request", flirtSessionId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        MvcResult requests = mockMvc.perform(get("/api/friends/requests")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = objectMapper.readTree(requests.getResponse().getContentAsString()).get("data");
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThan(0);
        friendRequestId = list.get(0).get("id").asText();
    }

    @Test
    @Order(3)
    void acceptFriendAndPrivateChat() throws Exception {
        MvcResult accept = mockMvc.perform(post("/api/friends/requests/{id}/accept", friendRequestId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();
        conversationId = objectMapper.readTree(accept.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(post("/api/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Chào bạn từ chat riêng\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Chào bạn từ chat riêng"));

        mockMvc.perform(get("/api/conversations")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].unread").value(true));
    }

    @Test
    @Order(4)
    void blockAndUnblock() throws Exception {
        mockMvc.perform(post("/api/friends/{id}/block", userBId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/friends/blocked")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(userBId));

        mockMvc.perform(delete("/api/friends/{id}/block", userBId)
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk());
    }

    @Test
    @Order(5)
    void banBlocksChatApi() throws Exception {
        mockMvc.perform(post("/api/admin/users/{id}/ban", userBId)
                        .param("banned", "true")
                        .header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden());

        String adminToken = loginAdmin();
        mockMvc.perform(post("/api/admin/users/{id}/ban", userBId)
                        .param("banned", "true")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/conversations/{id}/messages", conversationId)
                        .header("Authorization", bearer(tokenB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Should fail\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("4106"));
    }

    private String registerUser(String email, String nickname, String gender) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Test@12345",
                                "nickname", nickname,
                                "gender", gender,
                                "birthDate", LocalDate.now().minusYears(20).toString()
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private String fetchUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/user/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asText();
    }

    private String loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@thathinh.vn\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
