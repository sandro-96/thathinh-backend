package vn.thathinh.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.thathinh.constant.WebSocketMessageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    private WebSocketMessageType type;
    private T data;
}
