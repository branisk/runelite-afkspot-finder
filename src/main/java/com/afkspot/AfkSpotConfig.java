package com.afkspot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("afkspot")
public interface AfkSpotConfig extends Config
{
	@ConfigItem(
			keyName = "numberOfTiles",
			name = "Number of Tiles",
			description = "The number of top NPC density tiles to display (1, 2, 3, ..., n)",
			position = 1
	)
	@Range(min = 1)
	default int numberOfTiles()
	{
		return 3;
	}

	@ConfigItem(
			keyName = "npcName",
			name = "NPC Name",
			description = "Specify the name of the NPC to highlight tiles for",
			position = 2
	)
	default String npcName()
	{
		return "";
	}

}

