package pl.maps.tms.providers;

import java.util.Locale;

public class Dimension {

	public int width;
	public int height;
	public Dimension(int width, int height) {
		super();
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return String.format("[%d,%d]", width, height, Locale.ENGLISH);
	}	
}
