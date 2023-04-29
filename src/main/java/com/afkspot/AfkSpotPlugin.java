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
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

@Slf4j
@PluginDescriptor(
	name = "AFK Spot Finder",
	description = "Find the densest location of enemies in order to find the best afk spot",
	tags = { "combat", "afk", "markers", "density", "tile" }
)
public class AfkSpotPlugin extends Plugin
{
	private static final Comparator<Map.Entry<WorldPoint, Set<Integer>>> COMPARATOR = Comparator.comparingInt(e -> e.getValue().size());

	@Inject
	private Client client;

	@Inject
	private AfkSpotConfig config;

	@Inject
	private AfkSpotOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private final Map<WorldPoint, Set<Integer>> tileDensity = new HashMap<>();
	private int region = 0;
	private int plane = 0;

	@Override
	protected void startUp()
	{
		log.info("AFK Spot Finder started!");
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		log.info("AFK Spot Finder stopped!");
		this.region = this.plane = 0;
		this.clear();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			this.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// Clean up tileDensity on region changes (avoids memory leak)
		int plane = client.getPlane();
		int region = client.getLocalPlayer().getWorldLocation().getRegionID();
		if (this.plane != plane || this.region != region)
		{
			this.plane = plane;
			this.region = region;
			this.clear();
		}

		String npcNameFilter = config.npcName().trim();

		NPC[] npcs = client.getCachedNPCs();
		int n = npcs.length;
		for (int index = 0; index < n; index++)
		{
			NPC npc = npcs[index];

			if (npc == null || npc.isDead() || !isAttackable(npc))
			{
				continue;
			}

			// Skip the NPC if its name doesn't match the specified name
			if (!npcNameFilter.isEmpty() && !npcNameFilter.equalsIgnoreCase(npc.getName()))
			{
				continue;
			}

			WorldPoint npcTile = npc.getWorldLocation();
			tileDensity.computeIfAbsent(npcTile, k -> new HashSet<>()).add(index);
		}

		overlay.updateTopTiles(getTopTiles(config.numberOfTiles()));
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("afkspot")) {
			return;
		}

		if (event.getKey().equals("npcName")) {
			// Clear the tileDensity map and the overlay when the NPC name is changed
			this.clear();
		}
	}

	private void clear()
	{
		tileDensity.clear();
		overlay.updateTopTiles(Collections.emptyList());
	}

	@Provides
	AfkSpotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AfkSpotConfig.class);
	}

	private boolean isAttackable(NPC npc)
	{
		NPCComposition comp = npc.getTransformedComposition();
		return comp != null && ArrayUtils.contains(comp.getActions(), "Attack");
	}

	private Collection<Map.Entry<WorldPoint, Set<Integer>>> getTopTiles(int count)
	{
		if (tileDensity.isEmpty())
		{
			return Collections.emptyList();
		}

		final Queue<Map.Entry<WorldPoint, Set<Integer>>> heap = new PriorityQueue<>(count + 1, COMPARATOR);
		for (Map.Entry<WorldPoint, Set<Integer>> entry : tileDensity.entrySet())
		{
			int n = heap.size();
			if (n < count || COMPARATOR.compare(entry, heap.peek()) > 0)
			{
				if (n + 1 > count)
				{
					heap.poll();
				}
				heap.offer(entry);
			}
		}
		return heap;
	}
}
