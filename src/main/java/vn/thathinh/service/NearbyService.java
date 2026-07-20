package vn.thathinh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.FriendshipStatus;
import vn.thathinh.dto.NearbyUserResponse;
import vn.thathinh.exception.BusinessException;
import vn.thathinh.model.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tìm người dùng ở quanh vị trí hiện tại (2dsphere + $near). Loại trừ bản thân,
 * người đã bị chặn/chặn mình, tài khoản bị ban/khoá và người chưa bật chia sẻ vị trí.
 */
@Service
@RequiredArgsConstructor
public class NearbyService {

    private static final double DEFAULT_RADIUS_KM = 10;
    private static final double MAX_RADIUS_KM = 100;
    private static final int MAX_RESULTS = 50;

    private final MongoTemplate mongoTemplate;
    private final UserService userService;
    private final BlockService blockService;
    private final FriendService friendService;

    public List<NearbyUserResponse> findNearby(String userId, Double radiusKm) {
        User me = userService.findUser(userId);
        if (!me.isLocationEnabled() || me.getLocation() == null) {
            throw new BusinessException(ApiCode.LOCATION_NOT_ENABLED);
        }

        double radius = radiusKm == null ? DEFAULT_RADIUS_KM : Math.min(Math.max(radiusKm, 1), MAX_RADIUS_KM);

        Set<String> excluded = new HashSet<>();
        excluded.add(userId);
        excluded.addAll(blockService.blockedIdsBy(userId));
        excluded.addAll(blockService.blockerIdsOf(userId));

        Query filter = new Query()
                .addCriteria(Criteria.where("deleted").is(false))
                .addCriteria(Criteria.where("banned").is(false))
                .addCriteria(Criteria.where("active").is(true))
                .addCriteria(Criteria.where("locationEnabled").is(true))
                .addCriteria(Criteria.where("id").nin(excluded));

        Point point = new Point(me.getLocation().getX(), me.getLocation().getY());
        NearQuery near = NearQuery.near(point)
                .spherical(true)
                .maxDistance(new Distance(radius, Metrics.KILOMETERS))
                .limit(MAX_RESULTS)
                .query(filter);

        GeoResults<User> results = mongoTemplate.geoNear(near, User.class);

        Map<String, FriendshipStatus> statuses = friendService.getFriendshipStatuses(userId);

        List<NearbyUserResponse> list = new ArrayList<>();
        for (GeoResult<User> result : results) {
            User u = result.getContent();
            FriendshipStatus status = statuses.get(u.getId());
            list.add(NearbyUserResponse.builder()
                    .userId(u.getId())
                    .nickname(u.getNickname())
                    .avatarUrl(u.getAvatarUrl())
                    .gender(u.getGender())
                    .age(u.getAge())
                    .bio(u.getBio())
                    .distanceKm(Math.round(result.getDistance().getValue() * 10.0) / 10.0)
                    .friendshipStatus(status != null ? status.name() : null)
                    .build());
        }
        return list;
    }
}
