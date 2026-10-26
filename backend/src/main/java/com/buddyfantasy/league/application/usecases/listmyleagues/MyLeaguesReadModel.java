package com.buddyfantasy.league.application.usecases.listmyleagues;

import java.util.List;
import java.util.UUID;

/** Read-side port: direct projection of the user's leagues, without going through the domain. */
public interface MyLeaguesReadModel {

    List<MyLeagueView> findByUserId(UUID userId);
}
