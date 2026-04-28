package com.zzyl.controller;

import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.config.OSSAliyunFileStorageService;
import com.zzyl.dto.CheckInDto;
import com.zzyl.service.CheckInService;
import com.zzyl.vo.CheckInDetailVo;
import com.zzyl.vo.CheckInVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/check-in")
@Api(tags = "入住办理")
public class CheckInController extends BaseController {

    @Resource
    private CheckInService checkInService;

    @Autowired
    private OSSAliyunFileStorageService fileStorageService;

    @PostMapping("/apply")
    @ApiOperation("发起入住申请")
    public ResponseResult apply(@RequestBody CheckInDto checkInDto) {
        checkInService.apply(checkInDto);
        return success();
    }

    @GetMapping("/pageQuery")
    @ApiOperation("分页查询")
    public ResponseResult<PageResponse<CheckInVo>> pageQuery(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "elderName", required = false) String name,
            @RequestParam(value = "idCardNo", required = false) String idCard
    ) {
        return success(checkInService.pageQuery(pageNum, pageSize, name, idCard));
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("获取详情")
    public ResponseResult<CheckInDetailVo> detail(@PathVariable Long id) {
        return success(checkInService.detail(id));
    }

    @PostMapping("/upload")
    @ApiOperation("入住办理文件上传")
    public ResponseResult<String> upload(
            @ApiParam(value = "上传的文件", required = true)
            @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return error("上传文件不能为空");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            return error("文件大小超过10MB");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return error("文件格式不支持");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        String extensionName = extension.replace(".", "");
        List<String> allowExtensions = Arrays.asList("jpg", "jpeg", "png", "pdf");
        if (!allowExtensions.contains(extensionName)) {
            return error("仅支持上传JPG、JPEG、PNG、PDF文件");
        }
        try {
            String fileName = "uploads/check-in-docs/" + UUID.randomUUID() + extension;
            String filePath = fileStorageService.store(fileName, file.getInputStream());
            if (filePath == null) {
                return error("文件上传失败");
            }
            return ResponseResult.success(filePath);
        } catch (Exception e) {
            logger.error("入住办理文件上传失败", e);
            return error("文件上传失败");
        }
    }
}
