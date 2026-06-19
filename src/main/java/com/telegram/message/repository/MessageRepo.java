package com.telegram.message.repository;

import com.telegram.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE m.chat.id = :chatId AND m.isDeleted = false
            ORDER BY m.createdAt DESC
            """)
    Page<Message> findByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Query("""
            SELECT m FROM Message m
            WHERE m.chat.id = :chatId AND m.isDeleted = false
            ORDER BY m.createdAt DESC
            LIMIT 1
            """)
    Optional<Message> findLastMessageByChatId(@Param("chatId") Long chatId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.chat.id = :chatId
              AND m.isDeleted = false
              AND m.id > :afterMessageId
              AND m.sender.id <> :userId
            """)
    long countUnreadMessages(@Param("chatId") Long chatId,
                             @Param("afterMessageId") Long afterMessageId,
                             @Param("userId") Long userId);
}