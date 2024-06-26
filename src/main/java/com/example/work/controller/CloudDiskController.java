package com.example.work.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.net.URLEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class CloudDiskController {

    @Value("${cloud.disk.root-dir}")
    private String rootDir;

    @GetMapping({"/", "/path/**"})
    public String listFiles(Model model, @RequestParam(value = "path", required = false) String path) throws IOException {
        String directoryPath = rootDir;
        if (path != null) {
            // directoryPath = rootDir;
            directoryPath = Paths.get(rootDir, path).toString();
        }
        File file = new File(directoryPath);
        File[] files = file.listFiles();
        List<String> fname = new ArrayList<>();
        List<String> fsname = new ArrayList<>();
        if (files != null) {
            for (File temp : files) {
                if (temp.isDirectory()) {
                    fsname.add(temp.getName());
                } else {
                    fname.add(temp.getName());
                }
            }
        }
        model.addAttribute("files", fname);
        model.addAttribute("folders", fsname);
        model.addAttribute("currentPath", path != null ? path : "");
        return "index";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, @RequestParam(value = "path", required = false) String path, RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "选择文件上传");
            return "redirect:/";
        }
        try {
            String uploadPath = rootDir;
            if (path != null) {
                uploadPath = Paths.get(rootDir, path).toString();
            }
            Path filePath = Paths.get(uploadPath, file.getOriginalFilename());
            try(InputStream inputStream = file.getInputStream())
            {
            Files.copy(inputStream,filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            redirectAttributes.addFlashAttribute("message", "成功上传了:" + file.getOriginalFilename());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "redirect:/path?path=" + (path != null ? path : "");
    }
    @GetMapping("/delete/**")
    public String deleteFile(@RequestParam("filename") String filename, @RequestParam("path") String path,RedirectAttributes redirectAttributes) throws IOException {
        Path filePath = Paths.get(rootDir+path, filename);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            redirectAttributes.addFlashAttribute("message", "成功删除了:" + filename);
            return "redirect:/";
        } else {
            redirectAttributes.addFlashAttribute("message", "成功删除了:" + filename);
            System.out.println(filePath);
            return "redirect:/";
        }
    }
    @PostMapping("/batchDownload")
    public ResponseEntity<InputStreamResource> batchDownload(@RequestParam("selectedFiles") List<String> selectedFiles,
                                                             @RequestParam("path") String path) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (String file : selectedFiles) {
                File targetFile = new File(rootDir+path, file);
                if (targetFile.exists()) {
                    zos.putNextEntry(new ZipEntry(file));
                    FileInputStream fis = new FileInputStream(targetFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                    fis.close();
                }
            }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=files.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(bis));
    }
    @GetMapping("/files/**")
    public ResponseEntity<Resource> downloadFile(@RequestParam("filename") String filename, @RequestParam("path") String path) throws IOException {
        Path file = Paths.get(rootDir, path, filename);
        Resource resource = new UrlResource(file.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + URLEncoder.encode(file.getFileName().toString(),"utf-8") + "\"")
                .body(resource);
    }
}
