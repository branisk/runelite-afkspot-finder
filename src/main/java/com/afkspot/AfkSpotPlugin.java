package com.afkspot;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@PluginDescriptor(
	name = "AFK Spot Finder",
	description = "Finds dense tiles of enemies to identify the best afk spot",
	tags = { "combat", "afk", "markers", "density", "tile" }
)
public class AfkSpotPlugin extends Plugin
{
	private static final Comparator<Map.Entry<WorldPoint, Set<Integer>>> COMPARATOR = Comparator.comparingInt(e -> e.getValue().size());
	private static final Pattern DELIM = Pattern.compile("[,;\\n]");

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private AfkSpotConfig config;

	@Inject
	private AfkSpotOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	private final Set<String> npcNameFilters = new HashSet<>();

	private final Map<WorldPoint, Set<Integer>> tileDensity = new HashMap<>();
	private int region = 0;
	private int plane = 0;

	@Override
	protected void startUp()
	{
		log.info("AFK Spot Finder started!");
		parseFilters(config.npcNames());
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown()
	{
		log.info("AFK Spot Finder stopped!");
		clientThread.invoke(() -> {
			this.region = this.plane = 0;
			this.clear();
			this.npcNameFilters.clear();
		});
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

		boolean updated = false;
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
			String name = npc.getName();
			if (!npcNameFilters.isEmpty() && (name == null || !npcNameFilters.contains(name.toLowerCase())))
			{
				continue;
			}

			WorldArea area = npc.getWorldArea();
			for (int dx = 0; dx < area.getWidth(); dx++) {
				for (int dy = 0; dy < area.getHeight(); dy++) {
					WorldPoint occupiedTile = new WorldPoint(area.getX() + dx, area.getY() + dy, area.getPlane());
					updated |= tileDensity.computeIfAbsent(occupiedTile, k -> new HashSet<>()).add(index);
				}
			}
		}

		if (updated) {
			overlay.updateTopTiles(getTopTiles(config.numberOfTiles()));
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!event.getGroup().equals("afkspot")) {
			return;
		}

		if (event.getKey().equals("npcNames")) {
			// Update npcNameFilters
			this.parseFilters(event.getNewValue());

			// Clear the tileDensity map and the overlay when the NPC name is changed
			clientThread.invoke(this::clear);
		}
	}

	private void clear()
	{
		tileDensity.clear();
		overlay.updateTopTiles(Collections.emptyList());
	}

	private void parseFilters(String npcNames)
	{
		clientThread.invoke(() -> {
			npcNameFilters.clear();
			DELIM.splitAsStream(npcNames)
							.filter(StringUtils::isNotBlank)
							.map(String::trim)
							.map(String::toLowerCase)
							.forEach(npcNameFilters::add);
		});
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
