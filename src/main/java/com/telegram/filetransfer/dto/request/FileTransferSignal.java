package com.telegram.filetransfer.dto.request;

public record FileTransferSignal(
        Long transferId,
        Long receiverId,
        /** SDP_OFFER, SDP_ANSWER, ICE_CANDIDATE, PROGRESS, CANCEL */
        String type,
        /** JSON string: SDP description, ICE candidate, or progress data */
        String payload
) {}