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
		name = "AFK Spot Finder"
)
public class afkspotPlugin extends Plugin
{
	private static final Comparator<Map.Entry<WorldPoint, Set<Integer>>> COMPARATOR = Comparator.comparingInt(e -> e.getValue().size());

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
		overlay.updateTopTiles(Collections.emptyList());
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			tileDensity.clear();
			overlay.updateTopTiles(Collections.emptyList());
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		String npcNameFilter = config.npcName().trim().toLowerCase();

		NPC[] npcs = client.getCachedNPCs();
		int n = npcs.length;
		for (int index = 0; index < n; index++)
		{
			NPC npc = npcs[index];

			if (npc == null || npc.isDead() || !isAttackable(client, npc))
			{
				continue;
			}

			// Skip the NPC if its name doesn't match the specified name
			String npcName = npc.getName().toLowerCase();
			if (!npcNameFilter.isEmpty() && !npcName.equals(npcNameFilter))
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
			tileDensity.clear();
			overlay.updateTopTiles(Collections.emptyList());
		}
	}

	@Provides
	afkspotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(afkspotConfig.class);
	}

	private boolean isAttackable(Client client, NPC npc) {
		NPCComposition npcComposition = client.getNpcDefinition(npc.getId());

		if (npcComposition != null) {
			String[] actions = npcComposition.getActions();

			for (String action : actions) {
				if (action != null && action.toLowerCase().contains("attack")) {
					return true;
				}
			}
		}
		return false;
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
