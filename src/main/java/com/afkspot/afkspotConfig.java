package com.afkspot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("afkspot")
public interface afkspotConfig extends Config
{
	@ConfigItem(
			keyName = "numberOfTiles",
			name = "Number of Tiles",
			description = "The number of top NPC density tiles to display (1, 2, or 3)",
			position = 1
	)
	default int numberOfTiles()
	{
		return 3;
	}
}
