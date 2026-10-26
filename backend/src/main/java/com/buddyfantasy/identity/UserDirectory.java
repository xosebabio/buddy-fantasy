package com.buddyfantasy.identity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Public API of the identity module: batch lookup of user profiles for other modules. */
public interface UserDirectory {

    /** One query for all ids; unknown ids are absent from the result. */
    Map<UUID, UserSummary> findSummaries(Set<UUID> userIds);
}
