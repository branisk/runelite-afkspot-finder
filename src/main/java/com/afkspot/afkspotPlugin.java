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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.ToIntFunction;

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

	private final Map<WorldPoint, Set<Integer>> tileDensity = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		log.info("AFK Spot Finder started!");
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("AFK Spot Finder stopped!");
		tileDensity.clear();
		overlay.getTopTiles().clear();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			tileDensity.clear();
			overlay.getTopTiles().clear();
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

		updateTopTiles(config.numberOfTiles());
	}

	@Provides
	afkspotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(afkspotConfig.class);
	}

	private void updateTopTiles(int count)
	{
		if (tileDensity.isEmpty())
		{
			return;
		}
		
		int threshold = findKthLargest(tileDensity.values(), Collection::size, count);
		Map<WorldPoint, Integer> top = overlay.getTopTiles();
		top.clear();
		for (Map.Entry<WorldPoint, Set<Integer>> entry : tileDensity.entrySet())
		{
			int n = entry.getValue().size();
			if (n >= threshold)
			{
				top.put(entry.getKey(), n);
				
				if (top.size() >= count)
				{
					break;
				}
			}
		}
	}

	private static <T> Integer findKthLargest(Collection<T> values, ToIntFunction<T> valueToInt, int k)
	{
		final Queue<Integer> heap = new PriorityQueue<>(k);
		for (T t : values)
		{
			int intValue = valueToInt.applyAsInt(t);
			int n = heap.size(); // O(1)
			if (n < k || intValue > heap.peek())
			{
				if (n + 1 > k)
				{
					heap.poll(); // O(log k)
				}
				heap.add(intValue); // O(log k)
			}
		}
		return heap.peek(); // O(1)
	}
}
