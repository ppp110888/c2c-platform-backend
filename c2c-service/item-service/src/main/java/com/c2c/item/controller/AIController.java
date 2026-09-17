package com.c2c.item.controller;

import com.c2c.common.result.Result;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/item/ai")
public class AIController {

    @Resource
    private MinioClient minioClient;

    @Resource
    private RestTemplate restTemplate;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.base-url}")
    private String baseUrl;

    @Value("${openai.api.model}")
    private String model;

    /**
     * AI 识别图片生成标题或描述
     * @param body { imageUrls: [...], type: "title" | "content" }
     */
    @PostMapping("/describe")
    public Result<Map<String, String>> describeByImages(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) body.get("imageUrls");

        if (imageUrls == null || imageUrls.isEmpty()) {
            return Result.failed("请至少上传一张商品图片");
        }

        String type = body.get("type") != null ? body.get("type").toString() : "content";

        try {
            // 1. MinIO 下载 → base64
            List<String> base64Images = new ArrayList<>();
            for (String url : imageUrls) {
                String objectName = extractObjectName(url);
                byte[] imageBytes = downloadFromMinio(objectName);
                String base64 = Base64.getEncoder().encodeToString(imageBytes);
                String mimeType = guessMimeType(objectName);
                base64Images.add("data:image/" + mimeType + ";base64," + base64);
            }

            // 2. 构建请求
            Map<String, Object> requestBody = buildRequest(base64Images, type);

            // 3. 调用 API
            String apiUrl = baseUrl + "/chat/completions";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            System.out.println("🤖 调用智谱 GLM type=" + type);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            if (response.getBody() != null) {
                String text = extractContent(response.getBody());
                Map<String, String> result = new HashMap<>();
                result.put("description", text);
                return Result.success(result);
            }
            return Result.failed("AI 返回为空");

        } catch (Exception e) {
            String msg = e.getMessage();
            Throwable cause = e;
            while (cause.getCause() != null) cause = cause.getCause();
            String err = cause.getMessage() != null ? cause.getMessage() : msg;
            System.err.println("❌ AI 失败: " + err);
            e.printStackTrace();
            return Result.failed("AI调用失败: " + err);
        }
    }

    // ==================== 请求构建 ====================

    private Map<String, Object> buildRequest(List<String> base64Images, String type) {
        List<Map<String, Object>> userContent = new ArrayList<>();

        Map<String, Object> textPart = new LinkedHashMap<>();
        textPart.put("type", "text");
        textPart.put("text", type.equals("title") ? titlePrompt() : contentPrompt());
        userContent.add(textPart);

        for (String b64 : base64Images) {
            Map<String, Object> imgPart = new LinkedHashMap<>();
            imgPart.put("type", "image_url");
            Map<String, String> urlObj = new LinkedHashMap<>();
            urlObj.put("url", b64);
            imgPart.put("image_url", urlObj);
            userContent.add(imgPart);
        }

        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> sys = new LinkedHashMap<>();
        sys.put("role", "system");
        sys.put("content", "你是闲鱼卖家，根据图片写商品文案。直接输出，不解释。");
        messages.add(sys);

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("role", "user");
        user.put("content", userContent);
        messages.add(user);

        Map<String, Object> req = new LinkedHashMap<>();
        req.put("model", model);
        req.put("messages", messages);
        req.put("max_tokens", type.equals("title") ? 100 : 500);
        req.put("temperature", 0.7);

        return req;
    }

    /** 标题：只输出一行，不解释 */
    private String titlePrompt() {
        return "看图片识别商品，按以下风格输出一行标题，只输出标题本身：\n" +
               "自用闲置 Fila斐乐蘑菇鞋 黑色女款 36.5码";
    }

    /** 描述：直接按示例风格写 */
    private String contentPrompt() {
        return "看图片描述这个商品，按以下风格写，直接输出文案不要解释：\n\n" +
               "正品旗舰店购入 厚底设计 休闲运动都合适\n" +
               "成色几乎全新 保存很好 没怎么穿\n" +
               "包邮 支持自提\n" +
               "不议价 细节私聊";
    }

    // ==================== 响应解析 ====================

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> body) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg != null) {
                    Object content = msg.get("content");
                    if (content instanceof String) return ((String) content).trim();
                    if (content instanceof List) {
                        List<Map<String, String>> list = (List<Map<String, String>>) content;
                        if (!list.isEmpty()) return list.get(0).getOrDefault("text", "").trim();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("解析响应失败: " + e.getMessage());
        }
        return "AI 生成失败，请手动填写";
    }

    // ==================== MinIO 工具 ====================

    private byte[] downloadFromMinio(String objectName) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder().bucket(bucketName).object(objectName).build();
        try (InputStream is = minioClient.getObject(args);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
            return baos.toByteArray();
        }
    }

    private String extractObjectName(String url) {
        String path = url;
        if (path.contains("://")) {
            path = path.substring(path.indexOf("://") + 3);
            int i = path.indexOf('/');
            if (i >= 0) path = path.substring(i + 1);
        }
        if (path.startsWith(bucketName + "/")) path = path.substring(bucketName.length() + 1);
        if (path.startsWith("/")) path = path.substring(1);
        return path;
    }

    private String guessMimeType(String f) {
        f = f.toLowerCase();
        if (f.endsWith(".png")) return "png";
        if (f.endsWith(".gif")) return "gif";
        if (f.endsWith(".webp")) return "webp";
        if (f.endsWith(".bmp")) return "bmp";
        return "jpeg";
    }
}
