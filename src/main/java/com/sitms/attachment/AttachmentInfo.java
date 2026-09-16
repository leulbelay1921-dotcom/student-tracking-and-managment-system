package com.sitms.attachment;

import java.util.UUID;

/** Lightweight view of an {@link Attachment} resolved against its stored document. */
public record AttachmentInfo(
    UUID attachmentId, UUID documentId, String fileName, String contentType, long size) {}
