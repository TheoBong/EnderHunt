package com.bongbong.enderhunt.utils.scoreboardapi.api;

import com.bongbong.kitpvp.util.scoreboardapi.ScoreboardUpdateEvent;

public interface ScoreboardAdapter {

    void onUpdate(ScoreboardUpdateEvent event);

    int updateRate();
}
