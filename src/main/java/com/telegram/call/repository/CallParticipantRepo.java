package com.telegram.call.repository;

import com.telegram.call.entity.CallParticipant;
import com.telegram.common.enums.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallParticipantRepo extends JpaRepository<CallParticipant, Long> {

    List<CallParticipant> findByCallId(Long callId);
    List<CallParticipant> findByCallIdAndLeftAtIsNull(Long callId);
    Optional<CallParticipant> findByCallIdAndUserId(Long callId, Long userId);
    List<CallParticipant> findByCallIdAndStatus(Long callId, ParticipantStatus status);
    long countByCallIdAndStatus(Long callId, ParticipantStatus status);
}