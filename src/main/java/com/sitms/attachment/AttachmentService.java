package com.sitms.attachment;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.document.Document;
import com.sitms.document.DocumentRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Associates stored {@link Document}s with an owning record (activity or weekly report) without
 * duplicating file bytes. Validation ensures every attached document exists and belongs to the
 * same student as the owning record.
 */
@Service
public class AttachmentService {

  public static final String DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
  public static final String ATTACHMENT_NOT_OWNED = "ATTACHMENT_NOT_OWNED";

  private final AttachmentRepository attachmentRepository;
  private final DocumentRepository documentRepository;

  public AttachmentService(
      AttachmentRepository attachmentRepository, DocumentRepository documentRepository) {
    this.attachmentRepository = attachmentRepository;
    this.documentRepository = documentRepository;
  }

  /** Replaces the attachments of an owner with the given document ids (order preserved). */
  @Transactional
  public void replace(
      AttachmentOwnerType ownerType, UUID ownerId, Collection<UUID> documentIds, UUID studentId) {
    attachmentRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
    if (documentIds == null || documentIds.isEmpty()) {
      return;
    }
    List<Attachment> links = new ArrayList<>();
    for (UUID documentId : documentIds) {
      if (documentId == null) {
        continue;
      }
      Document document =
          documentRepository
              .findById(documentId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          DOCUMENT_NOT_FOUND, "Document not found: " + documentId));
      if (studentId != null && !studentId.equals(document.getStudentId())) {
        throw new BusinessException(
            ATTACHMENT_NOT_OWNED, "Document does not belong to this student: " + documentId);
      }
      links.add(
          Attachment.builder()
              .ownerType(ownerType)
              .ownerId(ownerId)
              .documentId(documentId)
              .build());
    }
    attachmentRepository.saveAll(links);
  }

  /** Deletes every attachment link for an owner (used when the owner is removed). */
  @Transactional
  public void deleteForOwner(AttachmentOwnerType ownerType, UUID ownerId) {
    attachmentRepository.deleteByOwnerTypeAndOwnerId(ownerType, ownerId);
  }

  /** Resolves the attachments of one owner. */
  @Transactional(readOnly = true)
  public List<AttachmentInfo> list(AttachmentOwnerType ownerType, UUID ownerId) {
    return resolve(attachmentRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId));
  }

  /** Resolves attachments for many owners at once, keyed by owner id. */
  @Transactional(readOnly = true)
  public Map<UUID, List<AttachmentInfo>> listByOwners(
      AttachmentOwnerType ownerType, Collection<UUID> ownerIds) {
    Map<UUID, List<AttachmentInfo>> result = new HashMap<>();
    if (ownerIds == null || ownerIds.isEmpty()) {
      return result;
    }
    List<Attachment> links = attachmentRepository.findByOwnerTypeAndOwnerIdIn(ownerType, ownerIds);
    Map<UUID, Document> documents = new HashMap<>();
    for (Document document : documentRepository.findAllById(links.stream().map(Attachment::getDocumentId).toList())) {
      documents.put(document.getId(), document);
    }
    for (Attachment link : links) {
      Document document = documents.get(link.getDocumentId());
      if (document == null) {
        continue;
      }
      result
          .computeIfAbsent(link.getOwnerId(), key -> new ArrayList<>())
          .add(toInfo(link, document));
    }
    return result;
  }

  private List<AttachmentInfo> resolve(List<Attachment> links) {
    if (links.isEmpty()) {
      return List.of();
    }
    Map<UUID, Document> documents = new HashMap<>();
    for (Document document : documentRepository.findAllById(links.stream().map(Attachment::getDocumentId).toList())) {
      documents.put(document.getId(), document);
    }
    List<AttachmentInfo> result = new ArrayList<>();
    for (Attachment link : links) {
      Document document = documents.get(link.getDocumentId());
      if (document != null) {
        result.add(toInfo(link, document));
      }
    }
    return result;
  }

  private static AttachmentInfo toInfo(Attachment link, Document document) {
    return new AttachmentInfo(
        link.getId(),
        document.getId(),
        document.getFileName(),
        document.getContentType(),
        document.getSize());
  }
}
