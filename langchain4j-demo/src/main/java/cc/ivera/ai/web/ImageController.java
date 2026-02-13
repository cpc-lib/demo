package cc.ivera.ai.web;

import cc.ivera.ai.domain.ImageReq;
import cc.ivera.ai.domain.ImageResp;
import cc.ivera.ai.service.ImageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/generate")
    public ImageResp generate(@RequestBody ImageReq req) {
        String url = imageService.generateImageUrl(req.getPrompt());
        return new ImageResp(url, "URL 通常是临时有效，请尽快下载保存");
    }
}
