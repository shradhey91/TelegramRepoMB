package com.telegram.chat.repository;

import com.telegram.chat.entity.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PinnedMessageRepo extends JpaRepository<PinnedMessage, Long> {

    @Query("""
            SELECT pm FROM PinnedMessage pm
            JOIN FETCH pm.message m
            JOIN FETCH m.sender
            JOIN FETCH pm.pinnedBy
            WHERE pm.chat.id = :chatId
            ORDER BY pm.pinnedAt DESC
            """)
    List<PinnedMessage> findByChatIdOrderByPinnedAtDesc(@Param("chatId") Long chatId);

    Optional<PinnedMessage> findByChatIdAndMessageId(Long chatId, Long messageId);

    boolean existsByChatIdAndMessageId(Long chatId, Long messageId);

    long countByChatId(Long chatId);
}