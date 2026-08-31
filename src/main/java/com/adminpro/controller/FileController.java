package com.adminpro.controller;

import com.adminpro.annotation.OperationLog;
import com.adminpro.annotation.RequiresPermission;
import com.adminpro.common.Result;
import com.adminpro.common.exception.BusinessException;
import com.adminpro.entity.SysFile;
import com.adminpro.mapper.SysFileMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final SysFileMapper fileMapper;

    @Value("${adminpro.file.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    @RequiresPermission("system:file:upload")
    @OperationLog("文件上传")
    public Result<SysFile> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        try {
            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                throw new BusinessException("上传目录创建失败");
            }
            String originalName = file.getOriginalFilename() == null ? "unnamed" : file.getOriginalFilename();
            String suffix = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
            String storedName = UUID.randomUUID().toString().replace("-", "") + suffix;
            Path target = Paths.get(uploadDir, storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            SysFile record = new SysFile();
            record.setFileName(storedName);
            record.setOriginalName(originalName);
            record.setFilePath(target.toString());
            record.setFileSize(file.getSize());
            record.setFileType(file.getContentType());
            Object uid = request.getAttribute("userId");
            record.setUploaderId(uid instanceof Long l ? l : null);
            fileMapper.insert(record);
            return Result.success(record);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/page")
    @RequiresPermission("system:file:list")
    public Result<Page<SysFile>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(fileMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysFile>().orderByDesc(SysFile::getId)));
    }

    @GetMapping("/download/{id}")
    @RequiresPermission("system:file:list")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        SysFile record = fileMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("文件不存在");
        }
        Path path = Paths.get(record.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException("文件已被移除");
        }
        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @GetMapping("/preview/{id}")
    @RequiresPermission("system:file:list")
    public ResponseEntity<Resource> preview(@PathVariable Long id) throws IOException {
        SysFile record = fileMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("文件不存在");
        }
        Path path = Paths.get(record.getFilePath());
        if (!Files.exists(path)) {
            throw new BusinessException("文件已被移除");
        }
        Resource resource = new FileSystemResource(path);
        MediaType mediaType = record.getFileType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(record.getFileType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(mediaType)
                .body(resource);
    }
}
