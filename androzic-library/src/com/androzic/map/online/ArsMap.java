package com.androzic.map.online;

import pl.maps.tms.providers.Dimension;
import pl.maps.tms.providers.GeoportalTopoProvider;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.androzic.map.online.geoportal.LatLon;

public class ArsMap extends OnlineMap {
	private static final long serialVersionUID = 1L;
	private GeoportalTopoProvider grid = new GeoportalTopoProvider();
	private Dimension tileCount = new Dimension(0, 0); // cached
	private int zoom = 9;
	public boolean showGrid = true;

	public ArsMap(TileProvider provider, byte z) {
		super(provider, z);
	}

	@Override
	public boolean drawMap(double[] loc, int[] lookAhead, int width, int height, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError {
		LatLon center = new LatLon(loc[0], loc[1]);
		com.androzic.map.online.geoportal.Position position = grid.getTilePosition(center, zoom);
		Paint paint = new Paint();
		paint.setColor(0xffff0000);
		c.drawLine(0, 0, width, height, paint);

		tileCount.width = (width / width) + 1;
		tileCount.height = (height / height) + 1;

		final Dimension ts = grid.getTileSize();
		final int offsetx = (int) ((position.x % 1) * ts.width);
		final int offsety = (int) ((position.y % 1) * ts.height);
		final int posx = (int) position.x;
		final int posy = (int) position.y;
		for (int y = 0; y <= tileCount.height; ++y) {
			for (int x = 0; x <= tileCount.width; ++x) {
				try {
					final int xx = x * ts.width - offsetx;
					final int yy = y * ts.height - offsety;
					final int tilex = posx + x;
					final int tiley = posy + y;

					// Image tile = images.getTile(tilex, tiley, zoom);
					// c.drawImage(tile, xx, yy, null);
					if (showGrid) {
						String msg = String.format("%d %d %d", (int) tilex, (int) tiley, zoom);
						c.drawText(msg, xx, yy + ts.height, paint);
						c.drawRect(xx, yy, ts.width, ts.height, paint);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}

}
