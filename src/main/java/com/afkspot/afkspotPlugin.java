package com.afkspot;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
		name = "AFK Spot Finder"
)
public class afkspotPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private afkspotConfig config;

	@Inject
	private afkspotOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private Map<WorldPoint, Set<Integer>> tileDensity;

	@Override
	protected void startUp() throws Exception
	{
		log.info("AFK Spot Finder started!");
		tileDensity = new HashMap<>();
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("AFK Spot Finder stopped!");
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			tileDensity.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		NPC[] npcs = client.getCachedNPCs();
		int n = npcs.length;
		for (int index = 0; index < n; index++)
		{
			NPC npc = npcs[index];
			if (npc == null || npc.isDead())
			{
				continue;
			}

			WorldPoint npcTile = npc.getWorldLocation();
			tileDensity.computeIfAbsent(npcTile, k -> new HashSet<>()).add(index);
		}

		overlay.updateTopTiles(getTopTiles(config.numberOfTiles()));
	}

	@Provides
	afkspotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(afkspotConfig.class);
	}

	private Map<WorldPoint, Integer> getTopTiles(int count)
	{
		Map<WorldPoint, Integer> sortedTiles = tileDensity.entrySet().stream()
				.sorted((a, b) -> b.getValue().size() - a.getValue().size())
				.limit(count)
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));

		return sortedTiles;
	}
}
