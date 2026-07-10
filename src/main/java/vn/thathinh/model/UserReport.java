package vn.thathinh.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import vn.thathinh.constant.ReportStatus;
import vn.thathinh.model.base.BaseEntity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_reports")
public class UserReport extends BaseEntity {

    @Id
    private String id;

    private String reporterId;
    private String reportedId;
    private String sessionId;
    private String reason;

    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    private String adminNote;
}
