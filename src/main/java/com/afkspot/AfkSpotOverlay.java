package com.afkspot;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class AfkSpotOverlay extends Overlay
{
    private final Client client;
    private final AfkSpotPlugin plugin;
    private Collection<Map.Entry<WorldPoint, Set<Integer>>> topTiles;

    @Inject
    public AfkSpotOverlay(Client client, AfkSpotPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        this.topTiles = Collections.emptyList();
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        for (Map.Entry<WorldPoint, Set<Integer>> entry : topTiles)
        {
            WorldPoint worldPoint = entry.getKey();
            if (client.getPlane() != worldPoint.getPlane())
            {
                continue;
            }

            LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
            if (localPoint != null)
            {
                Polygon tilePoly = Perspective.getCanvasTilePoly(client, localPoint);
                if (tilePoly != null)
                {
                    Color color = getColorForDensity(entry.getValue().size());
                    OverlayUtil.renderPolygon(graphics, tilePoly, color);
                }
            }
        }

        return null;
    }

    public void updateTopTiles(Collection<Map.Entry<WorldPoint, Set<Integer>>> topTiles)
    {
        this.topTiles = topTiles;
    }

    private Color getColorForDensity(int density)
    {
        if (density >= 3)
        {
            return Color.RED;
        }
        else if (density == 2)
        {
            return Color.YELLOW;
        }
        else
        {
            return Color.GREEN;
        }
    }
}
