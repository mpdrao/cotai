package dev.cotai.giftlist;

import dev.cotai.common.BusinessException;
import dev.cotai.common.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Accepts JPEG, PNG and WebP, detected from the file bytes (never from the client's content type or name). */
final class PhotoFormat {

    static final int MAX_BYTES = 5 * 1024 * 1024;

    private PhotoFormat() {
    }

    static String contentTypeOf(byte[] data) {
        if (data.length > MAX_BYTES) {
            throw new BusinessException(ErrorCode.PHOTO_TOO_LARGE, "Photo is larger than 5 MB.");
        }
        if (startsWith(data, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
            return "image/jpeg";
        }
        if (startsWith(data, 0, new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return "image/png";
        }
        if (startsWith(data, 0, ascii("RIFF")) && startsWith(data, 8, ascii("WEBP"))) {
            return "image/webp";
        }
        throw new BusinessException(ErrorCode.PHOTO_INVALID, "Photo must be a JPEG, PNG or WebP image.");
    }

    private static boolean startsWith(byte[] data, int offset, byte[] prefix) {
        return data.length >= offset + prefix.length
                && Arrays.equals(data, offset, offset + prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }
}
