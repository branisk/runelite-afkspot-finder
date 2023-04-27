package com.afkspot;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class afkspotOverlay extends Overlay
{
    private final Client client;
    private final afkspotPlugin plugin;
    @Getter
    private final Map<WorldPoint, Integer> topTiles = new HashMap<>();

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
        for (Map.Entry<WorldPoint, Integer> entry : topTiles.entrySet())
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
                    Color color = getColorForDensity(entry.getValue());
                    OverlayUtil.renderPolygon(graphics, tilePoly, color);
                }
            }
        }

        return null;
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
