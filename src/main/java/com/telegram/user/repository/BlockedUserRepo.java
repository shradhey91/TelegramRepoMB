package com.telegram.user.repository;

import com.telegram.user.entity.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedUserRepo extends JpaRepository<BlockedUser, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<BlockedUser> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
            SELECT bu.blocked.id FROM BlockedUser bu
            WHERE bu.blocker.id = :userId
            """)
    List<Long> findBlockedUserIds(@Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(bu) > 0 THEN true ELSE false END
            FROM BlockedUser bu
            WHERE (bu.blocker.id = :userId1 AND bu.blocked.id = :userId2)
               OR (bu.blocker.id = :userId2 AND bu.blocked.id = :userId1)
            """)
    boolean isEitherBlocked(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlockedUser bu WHERE bu.blocker.id = :blockerId OR bu.blocked.id = :blockedId")
    void deleteAllByBlockerIdOrBlockedId(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);
}