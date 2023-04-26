package com.afkspot;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class afkspotTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(afkspotPlugin.class);
		RuneLite.main(args);
	}
}