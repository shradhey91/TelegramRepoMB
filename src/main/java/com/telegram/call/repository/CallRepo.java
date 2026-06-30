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
            SELECT DISTINCT c FROM Call c
            JOIN CallParticipant p ON p.call = c
            WHERE p.user.id = :userId
              AND c.status IN :activeStatuses
            """)
    List<Call> findActiveCallsForUser(
            @Param("userId") Long userId,
            @Param("activeStatuses") List<CallStatus> activeStatuses);

    @Query("""
            SELECT DISTINCT c FROM Call c
            JOIN CallParticipant p ON p.call = c
            WHERE p.user.id = :userId
            ORDER BY c.createdAt DESC
            """)
    Page<Call> findCallHistoryByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT c FROM Call c
            WHERE c.status = com.telegram.common.enums.CallStatus.RINGING
              AND c.createdAt < :cutoff
            """)
    List<Call> findTimedOutRingingCalls(@Param("cutoff") java.time.OffsetDateTime cutoff);
}