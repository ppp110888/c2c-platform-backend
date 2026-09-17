package com.c2c.item.controller;

import com.c2c.common.result.Result;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.Set;
import java.util.Arrays;

@RestController
@RequestMapping("/item/file")
public class FileController {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    @Resource
    private MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.bucketName}")
    private String bucketName;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE || !ALLOWED_TYPES.contains(file.getContentType())) {
                return Result.failed(400, "仅支持5MB以内的 JPG、PNG 或 WebP 图片");
            }
            byte[] header = Arrays.copyOf(file.getBytes(), (int) Math.min(file.getSize(), 12));
            if (!matchesImageSignature(header, file.getContentType())) {
                return Result.failed(400, "图片内容与文件类型不匹配");
            }
            // 1. 获取文件原始名称和后缀 (例如: my-headphone.jpg -> .jpg)
            String originalFilename = file.getOriginalFilename();
            String extension = switch (file.getContentType()) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };

            // 2. 生成一个全球唯一的全新文件名 (按日期分文件夹存放，保持整洁)
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String newFileName = datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;

            // 3. 将文件流推送到 MinIO 服务器
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(newFileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
                );
            }

            // 4. 拼接出这根图片的绝对公网 URL，返回给前端！
            String imageUrl = endpoint + "/" + bucketName + "/" + newFileName;
            return Result.success(imageUrl);

        } catch (Exception e) {
            return Result.failed(500, "文件上传失败");
        }
    }

    private boolean matchesImageSignature(byte[] bytes, String contentType) {
        if (bytes.length < 4) return false;
        if ("image/jpeg".equals(contentType)) {
            return (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
        }
        if ("image/png".equals(contentType)) {
            return (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        }
        return bytes.length >= 12 && new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");
    }
}
