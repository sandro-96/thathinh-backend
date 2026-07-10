package vn.thathinh.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.exception.BusinessException;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    @Value("${app.aws.s3.bucket:}")
    private String bucket;

    @Value("${app.aws.s3.region:ap-southeast-1}")
    private String region;

    @Value("${app.aws.s3.public-url-base:}")
    private String publicUrlBase;

    @Value("${app.aws.s3.required:false}")
    private boolean s3Required;

    private final S3Client s3Client;

    public FileUploadService(@Autowired(required = false) S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadAvatar(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        validateImage(file);

        if (s3Client == null || bucket == null || bucket.isBlank()) {
            if (s3Required) {
                throw new BusinessException(ApiCode.INTERNAL_ERROR, "Chưa cấu hình lưu trữ ảnh (S3)");
            }
            log.warn("S3 not configured — using generated avatar for user {}", userId);
            return dicebearUrl(userId);
        }

        try {
            String ext = getExtension(file.getOriginalFilename());
            String key = "avatars/" + userId + "/" + UUID.randomUUID() + ext;
            PutObjectRequest.Builder request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType());

            s3Client.putObject(request.build(), RequestBody.fromBytes(file.getBytes()));
            return buildPublicUrl(key);
        } catch (Exception e) {
            log.error("S3 upload failed for user {}: {}", userId, e.getMessage());
            if (s3Required) {
                throw new BusinessException(ApiCode.INTERNAL_ERROR, "Không thể tải ảnh lên. Vui lòng thử lại sau.");
            }
            return dicebearUrl(userId);
        }
    }

    /**
     * Tải ảnh tin nhắn chat lên S3. Khác avatar: ảnh chat bắt buộc phải có lưu trữ
     * thật (S3), không dùng ảnh sinh tự động làm fallback.
     */
    public String uploadChatImage(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        validateImage(file);

        if (s3Client == null || bucket == null || bucket.isBlank()) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "Chưa cấu hình lưu trữ ảnh (S3)");
        }

        try {
            String ext = getExtension(file.getOriginalFilename());
            String key = "chat/" + userId + "/" + UUID.randomUUID() + ext;
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return buildPublicUrl(key);
        } catch (Exception e) {
            log.error("S3 chat image upload failed for user {}: {}", userId, e.getMessage());
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "Không thể tải ảnh lên. Vui lòng thử lại sau.");
        }
    }

    public String uploadProfilePhoto(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ApiCode.BAD_REQUEST);
        }
        validateImage(file);

        if (s3Client == null || bucket == null || bucket.isBlank()) {
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "Chưa cấu hình lưu trữ ảnh (S3)");
        }

        try {
            String ext = getExtension(file.getOriginalFilename());
            String key = "profile/" + userId + "/" + UUID.randomUUID() + ext;
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return buildPublicUrl(key);
        } catch (Exception e) {
            log.error("S3 profile photo upload failed for user {}: {}", userId, e.getMessage());
            throw new BusinessException(ApiCode.INTERNAL_ERROR, "Không thể tải ảnh lên. Vui lòng thử lại sau.");
        }
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, "Chỉ chấp nhận ảnh JPEG, PNG, WebP hoặc GIF");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException(ApiCode.VALIDATION_ERROR, "Ảnh tối đa 5MB");
        }
    }

    private String buildPublicUrl(String key) {
        if (publicUrlBase != null && !publicUrlBase.isBlank()) {
            String base = publicUrlBase.endsWith("/") ? publicUrlBase.substring(0, publicUrlBase.length() - 1) : publicUrlBase;
            return base + "/" + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String dicebearUrl(String userId) {
        return "https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId;
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) return ".jpg";
        return name.substring(name.lastIndexOf('.')).toLowerCase();
    }
}
