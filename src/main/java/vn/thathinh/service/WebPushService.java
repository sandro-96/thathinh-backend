package vn.thathinh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import nl.martijndwars.webpush.Utils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.thathinh.model.PushSubscription;
import vn.thathinh.model.VapidKeys;
import vn.thathinh.repository.PushSubscriptionRepository;
import vn.thathinh.repository.VapidKeysRepository;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Web Push (giao thức VAPID) — cho phép nhận thông báo cả khi tab/trình duyệt đã đóng.
 * VAPID keypair được tự sinh và lưu vào Mongo lần đầu chạy nên không cần cấu hình tay.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    private static final String VAPID_ID = "vapid";

    private final PushSubscriptionRepository subscriptionRepository;
    private final VapidKeysRepository vapidKeysRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.push.subject:mailto:admin@thathinh.vn}")
    private String subject;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private PushService pushService;
    private String publicKey;
    private boolean enabled = false;

    @PostConstruct
    void init() {
        try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
            VapidKeys keys = vapidKeysRepository.findById(VAPID_ID).orElseGet(this::generateAndStore);
            this.publicKey = keys.getPublicKey();
            this.pushService = new PushService(keys.getPublicKey(), keys.getPrivateKey(), subject);
            this.enabled = true;
            log.info("[WebPush] enabled (VAPID public key length={})", publicKey.length());
        } catch (Exception ex) {
            this.enabled = false;
            log.warn("[WebPush] disabled — không khởi tạo được VAPID: {}", ex.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private VapidKeys generateAndStore() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            byte[] pub = Utils.encode((org.bouncycastle.jce.interfaces.ECPublicKey) kp.getPublic());
            byte[] pri = Utils.encode((org.bouncycastle.jce.interfaces.ECPrivateKey) kp.getPrivate());
            Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
            VapidKeys keys = VapidKeys.builder()
                    .id(VAPID_ID)
                    .publicKey(enc.encodeToString(pub))
                    .privateKey(enc.encodeToString(pri))
                    .build();
            return vapidKeysRepository.save(keys);
        } catch (Exception ex) {
            throw new IllegalStateException("Không sinh được VAPID keypair", ex);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void saveSubscription(String userId, String endpoint, String p256dh, String auth) {
        if (endpoint == null || endpoint.isBlank()) return;
        PushSubscription sub = subscriptionRepository.findByEndpoint(endpoint)
                .orElseGet(() -> PushSubscription.builder().endpoint(endpoint).build());
        sub.setUserId(userId);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        subscriptionRepository.save(sub);
    }

    public void removeSubscription(String endpoint) {
        if (endpoint != null && !endpoint.isBlank()) {
            subscriptionRepository.deleteByEndpoint(endpoint);
        }
    }

    public void sendToUser(String userId, String title, String body, String url, String tag) {
        if (!enabled || userId == null) return;
        List<PushSubscription> subs = subscriptionRepository.findByUserId(userId);
        if (subs.isEmpty()) return;

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        payloadMap.put("title", title);
        payloadMap.put("body", body);
        payloadMap.put("url", url);
        payloadMap.put("tag", tag);
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception ex) {
            log.warn("[WebPush] serialize payload failed: {}", ex.getMessage());
            return;
        }

        for (PushSubscription sub : subs) {
            executor.submit(() -> deliver(sub, payload));
        }
    }

    private void deliver(PushSubscription sub, String payload) {
        try {
            Subscription subscription = new Subscription(
                    sub.getEndpoint(), new Subscription.Keys(sub.getP256dh(), sub.getAuth()));
            var response = pushService.send(new Notification(subscription, payload));
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                subscriptionRepository.deleteByEndpoint(sub.getEndpoint());
            } else if (status >= 400) {
                log.debug("[WebPush] push status {} for endpoint {}", status, sub.getEndpoint());
            }
        } catch (Exception ex) {
            log.debug("[WebPush] delivery failed: {}", ex.getMessage());
        }
    }
}
