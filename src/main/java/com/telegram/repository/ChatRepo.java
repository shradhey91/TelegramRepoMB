package com.telegram.repository;

import com.telegram.entities.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepo extends JpaRepository<Chat, Long> {

    @Query("""
            SELECT c FROM Chat c
            JOIN c.members m
            WHERE m.user.id = :userId
            ORDER BY c.updatedAt DESC
            """)
    List<Chat> findChatsByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT c FROM Chat c
            JOIN c.members m1 ON m1.user.id = :userId1
            JOIN c.members m2 ON m2.user.id = :userId2
            WHERE c.type = com.telegram.enums.ChatType.PRIVATE
            """)
    Optional<Chat> findPrivateChatBetween(@Param("userId1") Long userId1,
                                          @Param("userId2") Long userId2);
}