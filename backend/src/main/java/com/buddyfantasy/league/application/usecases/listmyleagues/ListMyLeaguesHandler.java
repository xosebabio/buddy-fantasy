package com.buddyfantasy.league.application.usecases.listmyleagues;

import java.util.List;

import com.buddyfantasy.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
class ListMyLeaguesHandler implements QueryHandler<ListMyLeaguesQuery, List<MyLeagueView>> {

    private final MyLeaguesReadModel readModel;

    ListMyLeaguesHandler(MyLeaguesReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<MyLeagueView> handle(ListMyLeaguesQuery query) {
        return readModel.findByUserId(query.userId());
    }
}
