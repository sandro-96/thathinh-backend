package vn.thathinh;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Nam + Nữ với preferences đã lưu (giống user thật sau khi hoàn thiện hồ sơ) phải match được.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlirtMatchPreferencesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void maleAndFemaleWithSavedPreferences_match() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String tokenMale = registerAndSaveProfile("male_" + runId, "MALE", "FEMALE");
        String tokenFemale = registerAndSaveProfile("female_" + runId, "FEMALE", "MALE");

        mockMvc.perform(post("/api/flirt/start").header("Authorization", bearer(tokenMale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        mockMvc.perform(post("/api/flirt/start").header("Authorization", bearer(tokenFemale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("MATCHED"))
                .andExpect(jsonPath("$.data.sessionId").exists());
    }

    @Test
    void femaleWithWrongLookingFor_doesNotMatchMale() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String tokenMale = registerAndSaveProfile("m2_" + runId, "MALE", "FEMALE");
        String tokenFemale = registerAndSaveProfile("f2_" + runId, "FEMALE", "FEMALE");

        mockMvc.perform(post("/api/flirt/start").header("Authorization", bearer(tokenMale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        mockMvc.perform(post("/api/flirt/start").header("Authorization", bearer(tokenFemale)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WAITING"));
    }

    private String registerAndSaveProfile(String nick, String gender, String lookingFor) throws Exception {
        String email = nick + "@prefs.test";
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "Test@123456",
                "nickname", nick,
                "gender", gender,
                "birthDate", LocalDate.of(2000, 1, 1).toString()
        ));

        MvcResult register = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(register.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        String profileBody = objectMapper.writeValueAsString(Map.of(
                "nickname", nick,
                "gender", gender,
                "birthDate", LocalDate.of(2000, 1, 1).toString(),
                "preferences", Map.of(
                        "minAge", 18,
                        "maxAge", 60,
                        "lookingFor", java.util.List.of(lookingFor)
                )
        ));

        mockMvc.perform(put("/api/user/me")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileBody))
                .andExpect(status().isOk());

        return token;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
