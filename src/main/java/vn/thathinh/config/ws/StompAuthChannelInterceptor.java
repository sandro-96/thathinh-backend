package vn.thathinh.config.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import vn.thathinh.repository.ConversationRepository;
import vn.thathinh.repository.FlirtSessionRepository;
import vn.thathinh.repository.FriendshipRepository;
import vn.thathinh.repository.TopicMembershipRepository;
import vn.thathinh.security.JwtUtil;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern TOPIC_MESSAGES = Pattern.compile("^/topic/topics/([^/]+)/messages$");
    private static final Pattern TOPIC_PRESENCE = Pattern.compile("^/topic/topics/([^/]+)/presence$");
    private static final Pattern FLIRT_MESSAGES = Pattern.compile("^/topic/flirt/([^/]+)/messages$");
    private static final Pattern USER_FLIRT = Pattern.compile("^/topic/users/([^/]+)/flirt$");
    private static final Pattern CONVERSATION_MESSAGES = Pattern.compile("^/topic/conversations/([^/]+)/messages$");
    private static final Pattern USER_FRIENDS = Pattern.compile("^/topic/users/([^/]+)/friends$");
    private static final Pattern VERIFY_TOPIC = Pattern.compile("^/topic/verify/([^/]+)$");

    private final JwtUtil jwtUtil;
    private final TopicMembershipRepository membershipRepository;
    private final FlirtSessionRepository flirtSessionRepository;
    private final ConversationRepository conversationRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        switch (accessor.getCommand()) {
            case CONNECT -> handleConnect(accessor);
            case SUBSCRIBE -> handleSubscribe(accessor);
            default -> {}
        }
        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractBearer(accessor);
        if (token == null) return;
        if (!jwtUtil.isTokenValid(token)) {
            throw new MessagingException("Invalid JWT");
        }
        accessor.setUser(new StompPrincipal(jwtUtil.extractUserId(token), jwtUtil.extractRole(token)));
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String dest = accessor.getDestination();
        if (!StringUtils.hasText(dest)) throw new MessagingException("Missing destination");

        if (VERIFY_TOPIC.matcher(dest).matches()) return;

        String userId = principalUserId(accessor);
        if (userId == null) throw new MessagingException("Authentication required");

        var topicMatch = TOPIC_MESSAGES.matcher(dest);
        if (topicMatch.matches()) {
            String topicId = topicMatch.group(1);
            if (!membershipRepository.existsByTopicIdAndUserId(topicId, userId)) {
                throw new MessagingException("Not a topic member");
            }
            return;
        }

        var topicPresenceMatch = TOPIC_PRESENCE.matcher(dest);
        if (topicPresenceMatch.matches()) {
            String topicId = topicPresenceMatch.group(1);
            if (!membershipRepository.existsByTopicIdAndUserId(topicId, userId)) {
                throw new MessagingException("Not a topic member");
            }
            return;
        }

        var flirtMatch = FLIRT_MESSAGES.matcher(dest);
        if (flirtMatch.matches()) {
            String sessionId = flirtMatch.group(1);
            if (!isFlirtParticipant(sessionId, userId)) {
                throw new MessagingException("Not a flirt session participant");
            }
            return;
        }

        var userFlirtMatch = USER_FLIRT.matcher(dest);
        if (userFlirtMatch.matches()) {
            if (!userId.equals(userFlirtMatch.group(1))) {
                throw new MessagingException("Cannot subscribe to another user's flirt channel");
            }
            return;
        }

        var convMatch = CONVERSATION_MESSAGES.matcher(dest);
        if (convMatch.matches()) {
            if (!isConversationParticipant(convMatch.group(1), userId)) {
                throw new MessagingException("Not a conversation participant");
            }
            return;
        }

        var userFriendsMatch = USER_FRIENDS.matcher(dest);
        if (userFriendsMatch.matches()) {
            if (!userId.equals(userFriendsMatch.group(1))) {
                throw new MessagingException("Cannot subscribe to another user's friends channel");
            }
            return;
        }

        throw new MessagingException("Destination not allowed: " + dest);
    }

    private boolean isConversationParticipant(String conversationId, String userId) {
        return conversationRepository.findByIdAndDeletedFalse(conversationId)
                .map(c -> userId.equals(c.getUserLowId()) || userId.equals(c.getUserHighId()))
                .orElse(false);
    }

    private String principalUserId(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof StompPrincipal p) return p.getUserId();
        return null;
    }

    private boolean isFlirtParticipant(String sessionId, String userId) {
        return flirtSessionRepository.findByIdAndDeletedFalse(sessionId)
                .map(s -> userId.equals(s.getUserAId()) || userId.equals(s.getUserBId()))
                .orElse(false);
    }

    private String extractBearer(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7);
        return null;
    }
}
