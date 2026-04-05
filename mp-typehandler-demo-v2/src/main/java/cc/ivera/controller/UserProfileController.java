package cc.ivera.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.ivera.dto.ExportTaskResponse;
import cc.ivera.dto.UserProfileExportRequest;
import cc.ivera.entity.CreateUserRequest;
import cc.ivera.entity.UserProfile;
import cc.ivera.service.UserProfileService;
import cc.ivera.service.export.UserProfileExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/userProfile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserProfileExportService userProfileExportService;

    @GetMapping("/page")
    public Page<UserProfile> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {
        return userProfileService.pageQuery(current, size, name);
    }

    @PostMapping("/add")
    public Map<String, Object> add() {
        Long id = userProfileService.addDemoUser();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }

    @PostMapping("/custom")
    public Map<String, Object> addCustom(@RequestBody CreateUserRequest request) {
        Long id = userProfileService.addCustomUser(request.getName(), request.getHobbies(), request.getTags());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }

    @GetMapping("/{id}")
    public UserProfile get(@PathVariable Long id) {
        return userProfileService.getById(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id) {
        userProfileService.updateDemoUser(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }

    @PostMapping("/export/tasks")
    public ExportTaskResponse submitExportTask(@Valid @RequestBody UserProfileExportRequest request) {
        return userProfileExportService.submit(request);
    }

    @GetMapping("/export/tasks/{taskId}")
    public ExportTaskResponse getExportTask(@PathVariable String taskId) {
        return userProfileExportService.getTask(taskId);
    }

    @GetMapping("/export/tasks/{taskId}/download")
    public ResponseEntity<Resource> download(@PathVariable String taskId) {
        Resource resource = userProfileExportService.getExportFile(taskId);
        String filename = resource.getFilename() == null ? "user-profile-export.xlsx" : resource.getFilename();

        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);

    }
}
