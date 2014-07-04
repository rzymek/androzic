package com.androzic.map.online;

import pl.maps.tms.providers.Dimension;
import pl.maps.tms.providers.GeoportalTopoProvider;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import com.androzic.map.Tile;
import com.androzic.map.online.geoportal.Coordinates;
import com.androzic.map.online.geoportal.LatLon;
import com.androzic.map.online.geoportal.Position;

public class ArsMap extends OnlineMap {
	private static final long serialVersionUID = 1L;
	private GeoportalTopoProvider grid = new GeoportalTopoProvider();

	public ArsMap(TileProvider provider, byte z) {
		super(provider, z);
	}

	@Override
	public boolean drawMap(double[] loc, int[] lookAhead, int width, int height, boolean cropBorder, boolean drawBorder, Canvas c) throws OutOfMemoryError {
		Dimension tileSize = grid.getTileSize();
		LatLon leftTop = new LatLon(loc[0], loc[1]);
		com.androzic.map.online.geoportal.Position position = grid.getTilePosition(leftTop, getSrcZoom());
		
		position.x -= (width / 2.0) / tileSize.width;
		position.y -= (height/ 2.0) / tileSize.width;
		
		Dimension tileCount = new Dimension(0, 0);
		tileCount.width = (width / tileSize.width) + 1;
		tileCount.height = (height / tileSize.height) + 1;

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
					int tiley = posy + y;

					tiley = grid.getTileCount((int) getSrcZoom()).height - 1 - tiley;
					Bitmap tile = getTile(tilex, tiley);

					if (tile != null && !tile.isRecycled()) {
						c.drawBitmap(tile, xx, yy, null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	private int getSrcZoom() {
		return srcZoom;
	}

	protected Bitmap getTile(int x, int y) throws OutOfMemoryError {
		Tile tile = tileController.getTile(x, y, (byte) getSrcZoom());
		return tile.bitmap;
	}

	@Override
	public int getScaledWidth() {
		Dimension tileCount = grid.getTileCount(getSrcZoom());
		return (int) (tileCount.width * GeoportalTopoProvider.TILE_SIZE.width * zoom);
	}

	@Override
	public int getScaledHeight() {
		Dimension tileCount = grid.getTileCount(getSrcZoom());
		return (int) (tileCount.height * GeoportalTopoProvider.TILE_SIZE.height * zoom);
	}

	@Override
	public boolean getXYByLatLon(double lat, double lon, int[] xy) {
		Position pos = grid.getTilePosition(new LatLon(lat, lon), getSrcZoom());
		xy[0] = (int) (pos.x * GeoportalTopoProvider.TILE_SIZE.width);
		xy[1] = (int) (pos.y * GeoportalTopoProvider.TILE_SIZE.height);
		return true;
	}

	@Override
	public boolean getLatLonByXY(int x, int y, double[] ll) {
		Coordinates coords = grid.getCoords(new Position(
				x / (double)GeoportalTopoProvider.TILE_SIZE.width, 
				y / (double)GeoportalTopoProvider.TILE_SIZE.height), getSrcZoom());
		ll[0] = coords.getLat();
		ll[1] = coords.getLon();
		return true;
	}
}
