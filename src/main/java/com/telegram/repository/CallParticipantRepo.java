package com.telegram.repository;

import com.telegram.entities.CallParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CallParticipantRepo extends JpaRepository<CallParticipant, Long> {

    Optional<CallParticipant> findByCallIdAndUserId(Long callId, Long userId);

    List<CallParticipant> findByCallId(Long callId);

    List<CallParticipant> findByCallIdAndLeftAtIsNull(Long callId);
}