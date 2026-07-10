package vn.thathinh.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportFlirtHistoryResponse {
    private int importedCount;
    private boolean alreadyImported;
}
