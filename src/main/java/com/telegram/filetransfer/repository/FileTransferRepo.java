package com.telegram.filetransfer.repository;

import com.telegram.common.enums.FileTransferStatus;
import com.telegram.filetransfer.entity.FileTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileTransferRepo extends JpaRepository<FileTransfer, Long> {

    @Query("""
            SELECT ft FROM FileTransfer ft
            WHERE ft.chat.id = :chatId
            ORDER BY ft.createdAt DESC
            """)
    Page<FileTransfer> findByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Query("""
            SELECT ft FROM FileTransfer ft
            WHERE (ft.sender.id = :userId OR ft.receiver.id = :userId)
              AND ft.status IN :activeStatuses
            """)
    List<FileTransfer> findActiveTransfersForUser(
            @Param("userId") Long userId,
            @Param("activeStatuses") List<FileTransferStatus> activeStatuses);
}