package com.realestate.service;

import com.realestate.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Server-side magic-byte (file signature) validation.
 *
 * The declared {@code Content-Type} on an upload is client-supplied and trivially
 * forged — a {@code shell.php} can be sent as {@code image/jpeg}. This validator
 * sniffs the actual leading bytes of the file and confirms they match a real
 * image (or, for documents, a real PDF/image). It is a defense-in-depth layer
 * ON TOP OF the existing size + declared-type checks, not a replacement.
 */
final class FileContentValidator {

    private FileContentValidator() {}

    /** Detected content kind from the file's leading bytes. */
    enum Kind { JPEG, PNG, WEBP, PDF, UNKNOWN }

    /** Images only — for property photos. */
    static void validateImage(MultipartFile file) {
        Kind kind = sniff(file);
        if (kind != Kind.JPEG && kind != Kind.PNG && kind != Kind.WEBP) {
            throw new BadRequestException(
                "File content is not a valid image. Only real JPEG, PNG or WebP files are allowed.");
        }
    }

    /** Images or PDFs — for verification documents (FMB / EC / patta / approval). */
    static void validateDocument(MultipartFile file) {
        Kind kind = sniff(file);
        if (kind == Kind.UNKNOWN) {
            throw new BadRequestException(
                "File content is not a valid document. Only real PDF, JPEG, PNG or WebP files are allowed.");
        }
    }

    /** Reads the first bytes off a fresh stream and classifies them. */
    static Kind sniff(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new BadRequestException("Could not read uploaded file");
        }
        return classify(head, read);
    }

    private static Kind classify(byte[] b, int len) {
        // JPEG: FF D8 FF
        if (len >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Kind.JPEG;
        }
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (len >= 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A
                && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return Kind.PNG;
        }
        // WEBP: "RIFF" .... "WEBP"
        if (len >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Kind.WEBP;
        }
        // PDF: "%PDF-"
        if (len >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
            return Kind.PDF;
        }
        return Kind.UNKNOWN;
    }
}
