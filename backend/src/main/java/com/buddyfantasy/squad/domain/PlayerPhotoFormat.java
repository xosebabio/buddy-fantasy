package com.buddyfantasy.squad.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.experimental.UtilityClass;

/**
 * Accepted player photos: the browser resizes them to a ~400 px WebP avatar, usually tens of KB. Checked both when
 * the upload URL is requested (declared values) and when the photo is confirmed (stored object).
 */
@UtilityClass
public class PlayerPhotoFormat {

    public final String CONTENT_TYPE = "image/webp";
    public final long MAX_SIZE_BYTES = 512 * 1024;

    public void requireAcceptable(String contentType, long sizeBytes) {
        if (!CONTENT_TYPE.equals(contentType)) {
            throw new DomainValidationException("Player photos must be " + CONTENT_TYPE);
        }
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new DomainValidationException("Player photos must be 1-" + MAX_SIZE_BYTES + " bytes");
        }
    }
}
