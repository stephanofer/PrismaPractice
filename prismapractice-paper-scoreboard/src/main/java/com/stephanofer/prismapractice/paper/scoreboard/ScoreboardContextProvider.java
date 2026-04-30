package com.stephanofer.prismapractice.paper.scoreboard;

import org.bukkit.entity.Player;

public interface ScoreboardContextProvider {

    ScoreboardContextSnapshot snapshot(Player player);
}
