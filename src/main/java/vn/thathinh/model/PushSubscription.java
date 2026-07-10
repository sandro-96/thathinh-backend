package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "push_subscriptions")
public class PushSubscription {

    @Id
    private String id;

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String endpoint;

    private String p256dh;
    private String auth;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
