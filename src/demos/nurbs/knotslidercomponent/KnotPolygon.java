package demos.nurbs.knotslidercomponent;

import java.awt.Polygon;

/**
 * Object representing knot
 * Objekt reprezentující uzel v uzlovém vektoru
 * @author Tomáš Hráský
 *
 */
@SuppressWarnings("serial")
class KnotPolygon extends Polygon implements Comparable {
	/**
	 * Knot value
	 * Hodnota uzlu
	 */
	private Double value;

	/**
	 * Size of change when moved by one pixel
	 * Velikost změny při posunu o jeden pixel
	 */
	private double oneStep;

	/**
	 * Top space
	 * Horní mezera osy
	 */
	private int top;

	/**
	 * Side space
	 * Boční mezera osy
	 */
	private int side;

	/**
	 * Knot multiplicity
	 * Násobnost uzlu
	 */
	private int multi;
	
	/**
	 * Creates new instance with given values
	 * Vytvoří instanci se zadanými hodnotami
	 * @param d knot value
	 * @param oneStep change of one pixel movement
	 * @param top top space
	 * @param side side space
	 */
	public KnotPolygon(Double d, double oneStep, int top, int side) {
		this.value = d;
		xpoints = new int[3];
		ypoints = new int[3];
		npoints = 3;
		multi=1;
		makeCoords(oneStep, top, side);
	}

	/**
	 * Computes coords of polygon representing knot
	 * Vypočte souřadnice polygonu reprezentujícího uzel
	 * @param oneStep change of one pixel movement
	 * @param top top space
	 * @param side side space
	 */
	private void makeCoords(double oneStep, int top, int side) {
		this.oneStep = oneStep;
		this.top = top;
		this.side = side;

		int x = (int) (value / oneStep);
		x += side;

		xpoints[0] = x;
		xpoints[1] = x - 4;
		xpoints[2] = x + 4;
		ypoints[0] = top + 2;
		ypoints[1] = top + 12;
		ypoints[2] = top + 12;
		
		invalidate();
	}

	/**
	 * Computes coords from set values
	 * Vypočte souřadnice podle nastavených hodont
	 */
	private void makeCoords() {
		makeCoords(oneStep, top, side);
	}

	/**
	 * Computes coords from given values
	 * Vypočte souřadnice podle zadaných hodont
	 * @param oneStep step of one pixel movement
	 * @param top top space
	 * @param side side space
	 */
	public void update(double oneStep, int top, int side) {
		makeCoords(oneStep, top, side);
	}

	/**
	 * Updates coords from given coord of polygon top
	 * Upraví souřadnice podle nové zadané souřadnice vrcholu polygonu
	 * @param x nová souřadnice vrcholu
	 */
	public void updateByX(int x) {
		value = oneStep * (x - side);
		makeCoords();
	}
	
	/**
	 * Updates coords from given value of knot
	 * Upraví souřadnice polygonu podle nové hodnoty
	 * @param d nová hodnota
	 */
	public void updateByValue(Double d){
		value=d;
		makeCoords();
	}

	public int compareTo(Object o) {
		if (o instanceof KnotPolygon) {
			KnotPolygon kp = (KnotPolygon) o;
			return getValue().compareTo(kp.getValue());
		} else
			return 0;
	}

	/**
	 * Returns knot value
	 * Vrací hodnotu uzlu
	 * @return knot value
	 */
	public Double getValue() {
		return value;
	}

	/**
	 * Returns knot multiplicity
	 * Vrací násobnost uzlu
	 * @return knot multiplicity
	 */
	public int getMulti() {
		return multi;
	}

	/**
	 * Sets knot multiplicity
	 * Nastavuje násobnost uzlu
	 * @param multi knot multiplicity
	 */
	public void setMulti(int multi) {
		this.multi = multi;
	}

}
