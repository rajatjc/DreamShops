package com.dailycodework.dreamshops.service.image;

import com.dailycodework.dreamshops.dto.ImageDto;
import com.dailycodework.dreamshops.exceptions.ResourceNotFoundException;
import com.dailycodework.dreamshops.model.Image;
import com.dailycodework.dreamshops.model.Product;
import com.dailycodework.dreamshops.repository.ImageRepository;
import com.dailycodework.dreamshops.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ImageRepository imageRepository;
    private final ProductRepository productRepository;

    @Transactional
    public List<ImageDto> addImages(Long productId, List<MultipartFile> files) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + productId + " not found"));
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Attach at least one file");
        }
        files.forEach(ImageService::check);
        return files.stream()
                .map(file -> {
                    Image image = imageRepository.save(new Image(file.getOriginalFilename(), file.getContentType(), bytes(file), product));
                    product.getImages().add(image); // keep both sides in step so deleting the product cascades to it
                    return toDto(image);
                })
                .toList();
    }

    /** The image with its bytes, for download. */
    @Transactional(readOnly = true)
    public Image getImage(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Image " + id + " not found"));
    }

    @Transactional
    public ImageDto replaceImage(Long id, MultipartFile file) {
        check(file);
        Image image = getImage(id);
        image.setFileName(file.getOriginalFilename());
        image.setFileType(file.getContentType());
        image.setData(bytes(file));
        return toDto(image);
    }

    @Transactional
    public void deleteImage(Long id) {
        Image image = getImage(id);
        // Take it out of the product's collection first: deleting it while the collection still holds it
        // would be undone by the cascade from Product.
        image.getProduct().getImages().remove(image);
        imageRepository.delete(image);
    }

    private static void check(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File " + file.getOriginalFilename() + " is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP and GIF images are accepted");
        }
        if (file.getSize() > Image.MAX_SIZE) {
            throw new IllegalArgumentException("Images can be at most 5 MB");
        }
    }

    private static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }
    }

    private static ImageDto toDto(Image image) {
        return new ImageDto(image.getId(), image.getFileName(), image.getFileType(), image.getProduct().getId());
    }
}
