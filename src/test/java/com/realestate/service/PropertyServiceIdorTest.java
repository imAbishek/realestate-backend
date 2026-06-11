package com.realestate.service;

import com.realestate.entity.Property;
import com.realestate.entity.PropertyDocument;
import com.realestate.entity.PropertyImage;
import com.realestate.entity.User;
import com.realestate.exception.ResourceNotFoundException;
import com.realestate.repository.PropertyDocumentRepository;
import com.realestate.repository.PropertyImageRepository;
import com.realestate.repository.PropertyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for the IDOR fix in {@link PropertyService}.
 *
 * Before the fix, deleteImage()/deleteDocument() verified ownership of the property
 * in the path but loaded the image/document by id alone — so an owner of one listing
 * could delete another listing's image/document by pairing their own propertyId with
 * a victim's imageId/documentId. The fix asserts the image/document actually belongs
 * to the property in the path. These tests pin that behaviour.
 */
@ExtendWith(MockitoExtension.class)
class PropertyServiceIdorTest {

    @Mock private PropertyRepository         propertyRepository;
    @Mock private PropertyImageRepository    imageRepository;
    @Mock private PropertyDocumentRepository documentRepository;
    @Mock private StorageService             imageUploadService;

    @InjectMocks private PropertyService propertyService;

    private static final String OWNER_EMAIL = "owner@propfind.in";

    private Property ownedProperty(UUID id) {
        User owner = User.builder().id(UUID.randomUUID()).email(OWNER_EMAIL).build();
        return Property.builder().id(id).owner(owner).build();
    }

    // ── deleteImage ──────────────────────────────────────────────

    @Test
    void deleteImage_rejectsImageBelongingToAnotherProperty() {
        UUID pathPropertyId  = UUID.randomUUID();   // the property the caller owns
        UUID otherPropertyId = UUID.randomUUID();   // where the image actually lives
        UUID imageId         = UUID.randomUUID();

        when(propertyRepository.findById(pathPropertyId))
            .thenReturn(Optional.of(ownedProperty(pathPropertyId)));

        PropertyImage victimImage = PropertyImage.builder()
            .id(imageId)
            .property(ownedProperty(otherPropertyId))   // different property
            .url("https://cdn/properties/" + otherPropertyId + "/x.jpg")
            .build();
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(victimImage));

        assertThatThrownBy(() ->
            propertyService.deleteImage(pathPropertyId, imageId, OWNER_EMAIL))
            .isInstanceOf(ResourceNotFoundException.class);

        // The cross-tenant delete must never reach storage or the DB.
        verify(imageUploadService, never()).deleteImage(org.mockito.ArgumentMatchers.anyString());
        verify(imageRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteImage_succeedsWhenImageBelongsToTheProperty() {
        UUID propertyId = UUID.randomUUID();
        UUID imageId    = UUID.randomUUID();

        Property property = ownedProperty(propertyId);
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

        PropertyImage image = PropertyImage.builder()
            .id(imageId)
            .property(property)
            .url("https://cdn/properties/" + propertyId + "/x.jpg")
            .build();
        when(imageRepository.findById(imageId)).thenReturn(Optional.of(image));

        assertDoesNotThrow(() ->
            propertyService.deleteImage(propertyId, imageId, OWNER_EMAIL));

        verify(imageUploadService, times(1)).deleteImage(image.getUrl());
        verify(imageRepository, times(1)).delete(image);
    }

    // ── deleteDocument ───────────────────────────────────────────

    @Test
    void deleteDocument_rejectsDocumentBelongingToAnotherProperty() {
        UUID pathPropertyId  = UUID.randomUUID();
        UUID otherPropertyId = UUID.randomUUID();
        UUID documentId      = UUID.randomUUID();

        when(propertyRepository.findById(pathPropertyId))
            .thenReturn(Optional.of(ownedProperty(pathPropertyId)));

        PropertyDocument victimDoc = PropertyDocument.builder()
            .id(documentId)
            .property(ownedProperty(otherPropertyId))   // different property
            .url("https://cdn/documents/" + otherPropertyId + "/x.pdf")
            .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(victimDoc));

        assertThatThrownBy(() ->
            propertyService.deleteDocument(pathPropertyId, documentId, OWNER_EMAIL))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(imageUploadService, never()).deleteDocument(org.mockito.ArgumentMatchers.anyString());
        verify(documentRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteDocument_succeedsWhenDocumentBelongsToTheProperty() {
        UUID propertyId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        Property property = ownedProperty(propertyId);
        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));

        PropertyDocument doc = PropertyDocument.builder()
            .id(documentId)
            .property(property)
            .url("https://cdn/documents/" + propertyId + "/x.pdf")
            .build();
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));

        assertDoesNotThrow(() ->
            propertyService.deleteDocument(propertyId, documentId, OWNER_EMAIL));

        verify(imageUploadService, times(1)).deleteDocument(doc.getUrl());
        verify(documentRepository, times(1)).delete(doc);
    }
}
