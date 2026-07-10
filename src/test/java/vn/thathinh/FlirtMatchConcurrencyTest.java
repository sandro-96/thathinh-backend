package vn.thathinh;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.thathinh.repository.FlirtSessionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FlirtMatchConcurrencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlirtSessionRepository sessionRepository;

    @Test
    void concurrentStarts_createSingleSessionPerPair() throws Exception {
        String runId = UUID.randomUUID().toString().substring(0, 8);
        String tokenA = register("m_a_" + runId, "MALE");
        String tokenB = register("m_b_" + runId, "FEMALE");
        String tokenC = register("m_c_" + runId, "MALE");

        long sessionsBefore = sessionRepository.count();

        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (String token : List.of(tokenA, tokenB, tokenC)) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                mockMvc.perform(post("/api/flirt/start")
                                .header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk());
                return null;
            }));
        }

        ready.await();
        go.countDown();
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        long newSessions = sessionRepository.count() - sessionsBefore;
        assertThat(newSessions).isEqualTo(1);
    }

    private String register(String nick, String gender) throws Exception {
        String email = nick + "@concurrency.test";
        String body = """
                {"email":"%s","password":"Test@123456","nickname":"%s","gender":"%s","birthDate":"2000-01-01"}
                """.formatted(email, nick, gender);

        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        return new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }
}
