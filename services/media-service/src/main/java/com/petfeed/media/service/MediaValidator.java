package com.petfeed.media.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class MediaValidator {
    private MediaValidator() {}
    public static final int MAX_BYTES = 50 * 1024 * 1024;

    public static String validate(byte[] data, String declared) {
        if (data.length == 0 || declared == null) throw invalid();
        if (data.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        if ("video/mp4".equals(declared)) {
            validateMp4(data);
            return declared;
        }
        if (!Set.of("image/jpeg", "image/png").contains(declared)) throw invalid();
        if (data.length > 10 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                reader.setInput(input);
                String actual = reader.getFormatName().equalsIgnoreCase("JPEG") ? "image/jpeg"
                        : reader.getFormatName().equalsIgnoreCase("PNG") ? "image/png" : "";
                if (!actual.equals(declared) || (long) reader.getWidth(0) * reader.getHeight(0) > 40_000_000L)
                    throw invalid();
                if (reader.read(0) == null) throw invalid();
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException e) { throw invalid(); }
        return declared;
    }

    // Validate the ISO BMFF container, not its codecs. Transcoding is intentionally separate.
    private static void validateMp4(byte[] data) {
        int offset = 0;
        boolean ftyp = false, moov = false, mdat = false;
        while (offset < data.length) {
            if (data.length - offset < 8) throw invalid();
            long size = Integer.toUnsignedLong(ByteBuffer.wrap(data, offset, 4).getInt());
            String type = new String(data, offset + 4, 4, StandardCharsets.US_ASCII);
            int header = 8;
            if (size == 1) {
                if (data.length - offset < 16) throw invalid();
                size = ByteBuffer.wrap(data, offset + 8, 8).getLong();
                header = 16;
            } else if (size == 0) size = (long) data.length - offset;
            if (size < header || size > data.length - offset) throw invalid();
            if (offset == 0 && !type.equals("ftyp")) throw invalid();
            if (type.equals("ftyp")) {
                if (size < header + 8) throw invalid();
                String brand = new String(data, offset + header, 4, StandardCharsets.US_ASCII);
                if (!Set.of("isom", "iso2", "iso4", "iso5", "iso6", "mp41", "mp42", "avc1", "M4V ", "MSNV").contains(brand)) throw invalid();
                ftyp = true;
            }
            moov |= type.equals("moov") && size > header;
            mdat |= type.equals("mdat") && size > header;
            offset += (int) size;
        }
        if (!ftyp || !moov || !mdat) throw invalid();
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Upload a valid JPEG, PNG or MP4 file");
    }
}
