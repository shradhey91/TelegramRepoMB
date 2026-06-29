package com.telegram.chat.repository;

import com.telegram.chat.entity.ChatMember;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepo extends JpaRepository<ChatMember, Long> {

    Optional<ChatMember> findByChatIdAndUserId(Long chatId, Long userId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);


    @Query("SELECT cm.user.id FROM ChatMember cm WHERE cm.chat.id = :chatId")
    List<Long> findUserIdsByChatId(@Param("chatId") Long chatId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMember cm WHERE cm.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}