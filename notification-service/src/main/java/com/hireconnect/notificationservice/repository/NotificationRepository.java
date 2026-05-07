package com.hireconnect.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import com.hireconnect.notificationservice.entity.NotificationLog;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<NotificationLog> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    List<NotificationLog> findByUserIdAndTypeInOrderByCreatedAtDesc(Long userId, List<String> types);

    List<NotificationLog> findByUserIdAndReadFalseAndTypeInOrderByCreatedAtDesc(Long userId, List<String> types);

    long countByUserIdAndReadFalse(Long userId);

    long countByUserIdAndReadFalseAndTypeIn(Long userId, List<String> types);

    @Modifying
    @Query("update NotificationLog n set n.read = true where n.userId = :userId and n.read = false")
    int markAllReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("update NotificationLog n set n.read = true where n.userId = :userId and n.read = false and n.type in :types")
    int markAllReadByUserIdAndTypeIn(@Param("userId") Long userId, @Param("types") List<String> types);
}
