package com.fillin.service.s3;

import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("이미지가 존재하지 않습니다.");
        }

        // 1. 파일 이름 중복 방지를 위한 UUID 생성
        String originalFilename = image.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String storedFileName = uuid + "_" + originalFilename;

        try (InputStream inputStream = image.getInputStream()) {
            // 2. S3에 파일 업로드
            s3Template.upload(bucketName, storedFileName, inputStream);

            // 3. 업로드된 파일의 URL 반환
            return s3Template.download(bucketName, storedFileName).getURL().toString();

        } catch (IOException | S3Exception e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("파일 업로드에 실패했습니다.");
        }
    }
}