package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.dto.BlockedUserResponse;
import vn.thathinh.model.UserBlock;
import vn.thathinh.repository.UserBlockRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final UserBlockRepository blockRepository;
    private final UserService userService;

    public boolean isBlockedEitherDirection(String userA, String userB) {
        return blockRepository.existsActiveBlockBetween(userA, userB);
    }

    public boolean hasBlocked(String blockerId, String blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedIdAndDeletedFalse(blockerId, blockedId);
    }

    /** Tập user mà {@code blockerId} đã chặn. */
    public java.util.Set<String> blockedIdsBy(String blockerId) {
        return blockRepository.findByBlockerIdAndDeletedFalse(blockerId).stream()
                .map(UserBlock::getBlockedId)
                .collect(Collectors.toSet());
    }

    /** Tập user đã chặn {@code blockedId}. */
    public java.util.Set<String> blockerIdsOf(String blockedId) {
        return blockRepository.findByBlockedIdAndDeletedFalse(blockedId).stream()
                .map(UserBlock::getBlockerId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void blockUser(String blockerId, String blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        userService.findUser(blockedId);
        if (blockRepository.existsByBlockerIdAndBlockedIdAndDeletedFalse(blockerId, blockedId)) {
            throw new BusinessException(ApiCode.USER_ALREADY_BLOCKED);
        }
        blockRepository.save(UserBlock.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build());
    }

    @Transactional
    public void unblockUser(String blockerId, String blockedId) {
        UserBlock block = blockRepository.findByBlockerIdAndBlockedIdAndDeletedFalse(blockerId, blockedId)
                .orElseThrow(() -> new BusinessException(ApiCode.USER_NOT_BLOCKED));
        block.setDeleted(true);
        block.setDeletedAt(Instant.now());
        blockRepository.save(block);
    }

    public void ensureNotBlocked(String userA, String userB) {
        if (isBlockedEitherDirection(userA, userB)) {
            throw new BusinessException(ApiCode.USER_BLOCKED);
        }
    }

    public List<BlockedUserResponse> listBlocked(String blockerId) {
        return blockRepository.findByBlockerIdAndDeletedFalse(blockerId).stream()
                .map(block -> {
                    var blocked = userService.findUser(block.getBlockedId());
                    return BlockedUserResponse.builder()
                            .userId(block.getBlockedId())
                            .nickname(blocked.getNickname())
                            .avatarUrl(blocked.getAvatarUrl())
                            .blockedAt(block.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
