package com.dailycodework.dreamshops.controller;

import com.dailycodework.dreamshops.model.Image;
import com.dailycodework.dreamshops.response.ApiResponse;
import com.dailycodework.dreamshops.service.image.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("${api.prefix}")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /** Multipart upload of one or more images, each in a part named "files". */
    @PostMapping(path = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> uploadImages(@PathVariable Long productId, @RequestParam("files") List<MultipartFile> files) {
        var images = imageService.addImages(productId, files);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse("Uploaded " + images.size() + " images", images));
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> downloadImage(@PathVariable Long id) {
        Image image = imageService.getImage(id);
        String fileName = image.getFileName() == null ? "image-" + id : image.getFileName();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(image.getData());
    }

    @PutMapping(path = "/images/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> replaceImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(new ApiResponse("Image replaced", imageService.replaceImage(id, file)));
    }

    @DeleteMapping("/images/{id}")
    public ResponseEntity<ApiResponse> deleteImage(@PathVariable Long id) {
        imageService.deleteImage(id);
        return ResponseEntity.ok(new ApiResponse("Image deleted", null));
    }
}
