package com.stephanofer.prismapractice.paper.scoreboard;

import org.bukkit.entity.Player;

import java.util.Map;

public interface ScoreboardPlaceholderResolver {

    Map<String, String> resolve(Player player, ScoreboardContextSnapshot snapshot);
}
