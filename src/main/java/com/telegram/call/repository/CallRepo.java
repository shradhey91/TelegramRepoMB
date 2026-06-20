package com.telegram.call.repository;

import com.telegram.call.entity.Call;
import com.telegram.common.enums.CallStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallRepo extends JpaRepository<Call, Long> {

    @Query("""
            SELECT c FROM Call c
            WHERE (c.caller.id = :userId OR c.receiver.id = :userId)
              AND c.status IN :activeStatuses
            """)
    List<Call> findActiveCallsForUser(
            @Param("userId") Long userId,
            @Param("activeStatuses") List<CallStatus> activeStatuses);


    @Query("""
            SELECT c FROM Call c
            WHERE c.caller.id = :userId OR c.receiver.id = :userId
            ORDER BY c.createdAt DESC
            """)
    Page<Call> findCallHistoryByUserId(
            @Param("userId") Long userId,
            Pageable pageable);


    @Query("""
            SELECT c FROM Call c
            WHERE (c.caller.id = :userId1 AND c.receiver.id = :userId2)
               OR (c.caller.id = :userId2 AND c.receiver.id = :userId1)
            ORDER BY c.createdAt DESC
            """)
    Page<Call> findCallHistoryBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            Pageable pageable);

    @Query("""
            SELECT c FROM Call c
            WHERE c.status = com.telegram.common.enums.CallStatus.RINGING
              AND c.createdAt < :cutoff
            """)
    List<Call> findTimedOutRingingCalls(@Param("cutoff") java.time.LocalDateTime cutoff);
}