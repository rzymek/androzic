package com.androzic.map.online.geoportal;

public class Geoportal {
	public static final Dimension GRID = new Dimension(3, 2);
	private static final long LZTS = 409600;
	private static final long WIDTH_IN_METERS = LZTS * GRID.width;
	private static final long HEIGHT_IN_METERS = LZTS * GRID.height;
	private static Dimension tileSize = new Dimension(256, 256);

	public static Dimension getTileSize() {
		return tileSize;
	}

	public static Position getTilePosition(Coordinates coordinates, int zoom) {
		Dimension tileCount = getTileCount(zoom);
		PUWG92 puwg92 = new PUWG92(coordinates);
		Position p = new Position();
		p.x = puwg92.getX() * tileCount.width / WIDTH_IN_METERS;
		p.y = tileCount.height - puwg92.getY() * tileCount.height / HEIGHT_IN_METERS;
		return p;
	} 

	protected static Dimension getTileCount(int zoom) {
		int z = 2 << (zoom - 1);// 2^(z-1)
		if (zoom <= 0)
			z = 1;
		int xTileCount = GRID.width * z;
		int yTileCount = GRID.height * z;
		return new Dimension(xTileCount, yTileCount);
	}

	
	public static Coordinates getCoords(Position position, int zoom) {
		Dimension tileCount = getTileCount(zoom);
		long pw = tileCount.width * tileSize.width;
		long ph = tileCount.height * tileSize.height;
		double px = position.x * tileSize.width;
		double py = position.y * tileSize.height;
		double x = WIDTH_IN_METERS * px / pw;
		double y = HEIGHT_IN_METERS - HEIGHT_IN_METERS * py / ph;
		return new PUWG92(x, y);
	}

}
