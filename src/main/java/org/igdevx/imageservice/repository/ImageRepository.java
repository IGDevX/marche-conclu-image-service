package org.igdevx.imageservice.repository;

import org.igdevx.imageservice.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, UUID> {

    Optional<Image> findByIdAndDeletedAtIsNull(UUID id);

    List<Image> findByDeletedAtIsNull();

    List<Image> findByUserIdAndEntityTypeAndDeletedAtIsNull(String userId, Image.EntityType entityType);

    Optional<Image> findByProductIdAndDeletedAtIsNull(String productId);

    List<Image> findByUserIdAndDeletedAtIsNull(String userId);
}

