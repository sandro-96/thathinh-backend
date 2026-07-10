package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "app_settings")
public class VapidKeys {

    /** Fixed id so we always store a single keypair. */
    @Id
    private String id;

    private String publicKey;
    private String privateKey;
}
