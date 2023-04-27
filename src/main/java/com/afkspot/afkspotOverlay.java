package com.afkspot;

import lombok.Getter;
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

public class afkspotOverlay extends Overlay
{
    private final Client client;
    private final afkspotPlugin plugin;
    @Getter
    private Collection<Map.Entry<WorldPoint, Set<Integer>>> topTiles = Collections.emptyList();

    @Inject
    public afkspotOverlay(Client client, afkspotPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
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

    public void updateTopTiles(Collection<Map.Entry<WorldPoint, Set<Integer>>> tiles)
    {
        this.topTiles = tiles;
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
