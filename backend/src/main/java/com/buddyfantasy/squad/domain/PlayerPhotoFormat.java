package com.buddyfantasy.squad.domain;

import com.buddyfantasy.shared.domain.DomainValidationException;
import lombok.experimental.UtilityClass;

/**
 * Accepted player photos: the browser resizes them to a ~400 px WebP avatar, usually tens of KB. Checked both when
 * the upload URL is requested (declared values) and when the photo is confirmed (stored object). The maximum size is
 * {@code squad.limits.photo-max-size}.
 */
@UtilityClass
public class PlayerPhotoFormat {

    public final String CONTENT_TYPE = "image/webp";

    public void requireAcceptable(String contentType, long sizeBytes, SquadLimits limits) {
        if (!CONTENT_TYPE.equals(contentType)) {
            throw new DomainValidationException("Player photos must be " + CONTENT_TYPE);
        }
        limits.requirePhotoSize(sizeBytes);
    }
}
