package doody.spring.common.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

@Service
public class S3FileStorageService {

    private final String bucket;
    private final String publicBaseUrl;
    private final S3Client s3Client;

    public S3FileStorageService(
        @Value("${cloud.aws.s3.bucket:}") String bucket,
        @Value("${cloud.aws.region.static:ap-northeast-2}") String region,
        @Value("${cloud.aws.s3.public-base-url:}") String publicBaseUrl
    ) {
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.strip();
        this.s3Client = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    public UploadedFile uploadEvidence(String userId, String missionId, MultipartFile file) {
        if (bucket == null || bucket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "s3 bucket is not configured.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required.");
        }

        String key = evidenceKey(userId, missionId, file.getOriginalFilename());
        String contentType = file.getContentType() == null || file.getContentType().isBlank()
            ? "application/octet-stream"
            : file.getContentType();

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(file.getSize())
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return new UploadedFile(publicUrl(key), contentType);
        } catch (S3Exception | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to upload file to s3.", e);
        }
    }

    private String evidenceKey(String userId, String missionId, String originalFilename) {
        String filename = sanitizeFilename(originalFilename);
        return "doody/mission/%s/%s/%s/%s".formatted(
            safePath(userId),
            safePath(missionId),
            LocalDate.now(),
            UUID.randomUUID() + "-" + filename
        );
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "evidence";
        }
        String filename = originalFilename.replace("\\", "/");
        int slash = filename.lastIndexOf('/');
        if (slash >= 0) {
            filename = filename.substring(slash + 1);
        }
        filename = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        return filename.isBlank() ? "evidence" : filename;
    }

    private String safePath(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String publicUrl(String key) {
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + "/" + encodePath(key);
        }
        return s3Client.utilities()
            .getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build())
            .toString();
    }

    private String encodePath(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20").replace("%2F", "/");
    }

    public record UploadedFile(
        String url,
        String contentType
    ) {
    }
}
