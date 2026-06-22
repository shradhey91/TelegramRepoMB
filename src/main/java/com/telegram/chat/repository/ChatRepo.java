package com.telegram.chat.repository;

import com.telegram.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepo extends JpaRepository<Chat, Long> {

    @Query("""
            SELECT DISTINCT c FROM Chat c
            JOIN FETCH c.members m
            JOIN FETCH m.user
            JOIN FETCH c.createdBy
            WHERE c.id IN (
                SELECT cm.chat.id FROM ChatMember cm WHERE cm.user.id = :userId
            )
            ORDER BY c.updatedAt DESC
            """)
    List<Chat> findChatsByUserId(@Param("userId") Long userId);

    @Query(value = """
            SELECT DISTINCT c FROM Chat c
            JOIN FETCH c.members m
            JOIN FETCH m.user
            JOIN FETCH c.createdBy
            WHERE c.id IN (
                SELECT cm.chat.id FROM ChatMember cm WHERE cm.user.id = :userId
            )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT c) FROM Chat c
            JOIN c.members m
            WHERE m.user.id = :userId
            """)
    Page<Chat> findChatsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT c FROM Chat c
            JOIN FETCH c.members m
            JOIN FETCH m.user
            JOIN FETCH c.createdBy
            WHERE c.id IN (
                SELECT cm.chat.id FROM ChatMember cm
                WHERE cm.chat.type = com.telegram.common.enums.ChatType.PRIVATE
                AND cm.chat.id IN (
                    SELECT cm2.chat.id FROM ChatMember cm2 WHERE cm2.user.id = :userId1
                )
                AND cm.user.id = :userId2
            )
            """)
    Optional<Chat> findPrivateChatBetween(@Param("userId1") Long userId1,
                                          @Param("userId2") Long userId2);


    @Query("""
            SELECT c FROM Chat c
            JOIN FETCH c.members m
            JOIN FETCH m.user
            JOIN FETCH c.createdBy
            WHERE c.inviteLink = :inviteLink
            """)
    Optional<Chat> findByInviteLink(@Param("inviteLink") String inviteLink);
}