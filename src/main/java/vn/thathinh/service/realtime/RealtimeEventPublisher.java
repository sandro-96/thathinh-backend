package vn.thathinh.service.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.thathinh.constant.WebSocketMessageType;
import vn.thathinh.dto.websocket.WebSocketMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public <T> void publishTopicMessage(String topicId, T payload) {
        publish("/topic/topics/" + topicId + "/messages", WebSocketMessageType.TOPIC_MESSAGE, payload);
    }

    public <T> void publishFlirtMessage(String sessionId, T payload) {
        publish("/topic/flirt/" + sessionId + "/messages", WebSocketMessageType.FLIRT_MESSAGE, payload);
    }

    public <T> void publishFlirtEvent(String userId, WebSocketMessageType type, T payload) {
        publish("/topic/users/" + userId + "/flirt", type, payload);
    }

    public <T> void publishFriendEvent(String userId, WebSocketMessageType type, T payload) {
        publish("/topic/users/" + userId + "/friends", type, payload);
    }

    public <T> void publishPrivateMessage(String conversationId, T payload) {
        publish("/topic/conversations/" + conversationId + "/messages", WebSocketMessageType.PRIVATE_MESSAGE, payload);
    }

    public <T> void publishPrivateTyping(String conversationId, T payload) {
        publish("/topic/conversations/" + conversationId + "/messages", WebSocketMessageType.PRIVATE_TYPING, payload);
    }

    public <T> void publishPrivateRead(String conversationId, T payload) {
        publish("/topic/conversations/" + conversationId + "/messages", WebSocketMessageType.PRIVATE_READ, payload);
    }

    public <T> void publishFlirtTyping(String sessionId, T payload) {
        publish("/topic/flirt/" + sessionId + "/messages", WebSocketMessageType.FLIRT_TYPING, payload);
    }

    public <T> void publishPrivateMessageUpdated(String conversationId, T payload) {
        publish("/topic/conversations/" + conversationId + "/messages",
                WebSocketMessageType.PRIVATE_MESSAGE_UPDATED, payload);
    }

    public <T> void publishFlirtMessageUpdated(String sessionId, T payload) {
        publish("/topic/flirt/" + sessionId + "/messages",
                WebSocketMessageType.FLIRT_MESSAGE_UPDATED, payload);
    }

    public <T> void publishTopicPresence(String topicId, WebSocketMessageType type, T payload) {
        publish("/topic/topics/" + topicId + "/presence", type, payload);
    }

    private <T> void publish(String destination, WebSocketMessageType type, T payload) {
        if (!StringUtils.hasText(destination)) return;
        try {
            messagingTemplate.convertAndSend(destination, new WebSocketMessage<>(type, payload));
        } catch (Exception ex) {
            log.warn("Realtime publish failed ({} -> {}): {}", type, destination, ex.getMessage());
        }
    }
}
