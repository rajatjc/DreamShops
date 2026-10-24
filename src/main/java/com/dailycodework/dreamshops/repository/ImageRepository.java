package com.dailycodework.dreamshops.repository;

import com.dailycodework.dreamshops.dto.ImageDto;
import com.dailycodework.dreamshops.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    @Query("""
            select new com.dailycodework.dreamshops.dto.ImageDto(i.id, i.fileName, i.fileType, i.product.id)
            from Image i where i.product.id in :productIds order by i.id""")
    List<ImageDto> findSummariesByProductIds(Collection<Long> productIds);
}
