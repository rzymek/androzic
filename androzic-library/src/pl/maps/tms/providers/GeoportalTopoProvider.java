package pl.maps.tms.providers;

import com.androzic.map.online.geoportal.Coordinates;
import com.androzic.map.online.geoportal.PUWG92;
import com.androzic.map.online.geoportal.Position;

public class GeoportalTopoProvider {
	public static final Dimension GRID = new Dimension(3, 2);
	private static final long LZTS = 409600;
	public static final long WIDTH_IN_METERS = LZTS * GRID.width;
	public static final long HEIGHT_IN_METERS = LZTS * GRID.height;
	public static final Dimension TILE_SIZE = new Dimension(256, 256);

	public Dimension getTileSize() {
		return TILE_SIZE;
	}

	public Position move(Position tile, double dx, double dy) {
		tile.x += dx;
		tile.y += dy;
		return tile;
	}

	public Position getTilePosition(Coordinates coordinates, int zoom) {
		Dimension tileCount = getTileCount(zoom);
		PUWG92 puwg92 = new PUWG92(coordinates);
		Position p = new Position();
		p.x = puwg92.getX() * tileCount.width / WIDTH_IN_METERS;
		p.y = tileCount.height - puwg92.getY() * tileCount.height / HEIGHT_IN_METERS;
		return p;
	}

	public Dimension getTileCount(int zoom) {
		int z = 2 << (zoom - 1);// 2^(z-1)
		if (zoom <= 0)
			z = 1;
		int xTileCount = GRID.width * z;
		int yTileCount = GRID.height * z;
		return new Dimension(xTileCount, yTileCount);
	}

	public Coordinates getCoords(Position position, int zoom) {
		Dimension tileCount = getTileCount(zoom);
		long pw = tileCount.width * TILE_SIZE.width;
		long ph = tileCount.height * TILE_SIZE.height;
		double px = position.x * TILE_SIZE.width;
		double py = position.y * TILE_SIZE.height;
		double x = WIDTH_IN_METERS * px / pw;
		double y = HEIGHT_IN_METERS - HEIGHT_IN_METERS * py / ph;
		return new PUWG92(x, y);
	}

	public String getName() {
		return "topo";
	}

	public String toString() {
		return "GeoPortal TOPO";
	}

}