package demos.nurbs.knotslidercomponent;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.JComponent;

/**
 * GUI component for editing NURBS curve/surface knotvector
 * Komponenta grafického uživatelského rozhraní pro editaci uzlového vektoru NURBS křivky
 * @author Tomáš Hráský
 *
 */
@SuppressWarnings("serial")
public class JKnotSlider extends JComponent implements ComponentListener,
		MouseMotionListener, MouseListener {

	/**
	 * Knot value change event
	 * Událost změny hodoty prvku uzlového vektoru
	 */
	private static final String KNOT_MOVED = "KnotMoved";

	/**
	 * Vector representing knots
	 * Vektor objektů reprezentujících prvky uzlového vektoru
	 */
	private Vector<KnotPolygon> knots;
	
	/**
	 * Previous knot vector (for recovery after user wrong setting)
	 * Předchozí vektor objektů reprezentujících prvky uzlového vektoru - pro obnovení při chybě uživatele
	 */
	private Vector<KnotPolygon> previousState;

	/**
	 * List of listeners attached to component
	 * Seznam ActionListenerů navázaných na komponentu
	 */
	private LinkedList<ActionListener> actionListeners;

	/**
	 * Value of one pixel movement
	 * Hodnota posunu o jeden pixel
	 */
	private double oneStep;

	/**
	 * Side space
	 * Mezera na straně osy
	 */
	private int side;

	/**
	 * Top space
	 * Mezera nad osou
	 */
	private int top;

	/**
	 * Actually selected knot index
	 * Index aktuálně vybraného prvku uzlového vektoru
	 */
	private int activeKnot;

//	private Vector<Integer> xVector;


	/**
	 * Creates component
	 * Vytvoří komponentu
	 */
	public JKnotSlider() {
		oneStep = 0;
		top = 0;
		side = 0;
		knots = new Vector<KnotPolygon>();
		previousState=new Vector<KnotPolygon>();
//		xVector=new Vector<Integer>();
		
		actionListeners = new LinkedList<ActionListener>();
		this.addComponentListener(this);
		this.addMouseMotionListener(this);
		this.addMouseListener(this);
	}

	/**
	 * Adds listener to notified list. list
	 * Přidá zadaný listener do seznamu naslouchajících objektů
	 * @param listener added listener
	 */
	public void addActionListener(ActionListener listener) {
		actionListeners.add(listener);
	}

	/**
	 * Creates component with given knotvector knots
	 * Vytvoří komponentu se zadanými hodnotami uzlového vektoru
	 * @param knots knot vector
	 */
	public JKnotSlider(double[] knots) {
		this();
		for (double d : knots) {
			addKnot(new Double(d));
		}
	}

	/**
	 * Creates component with given knotvector knots
	 * Vytvoří komponentu se zadanými hodnotami uzlového vektoru
	 * @param knots knot vector
	 */
	public JKnotSlider(Vector<Double> knots) {
		this();
		for (Double d : knots) {
			addKnot(d);
		}
	}
	/**
	 * Creates component with given knotvector knots
	 * Vytvoří komponentu se zadanými hodnotami uzlového vektoru
	 * @param knots uzlový vektor
	 */
	public JKnotSlider(float[] knots) {
		this();
		if(knots!=null)
			for(double d:knots)
				addKnot(new Double(d));
	}

	/**
	 * Adds a knot
	 * Přidá uzel do uzlového vektoru
	 * @param d knot 
	 */
	public void addKnot(Double d) {
//		preserveState();
		knots.add(new KnotPolygon(d, oneStep, top, side));
	}
	
	
	/**
	 * Saves actual knotvector for later recovery
	 * Uloží aktuální uzlový vektor pro pozdější obnovení
	 */
	private void preserveState(){
		previousState.clear();
		previousState.addAll(knots);
	}
	
//	/**
//	 * Přidá uzel do uzlového vektoru
//	 * @param d hodnota přidávaného uzlu
//	 */
//	public void addKnot(double d) {
//		addKnot(new Double(d));
//	}

	/**
	 * Returns knotvector
	 * Vrací uzlový vektor
	 * @return knotvector array
	 */
	@SuppressWarnings("unchecked")
	public double[] getKnots() {
		double[] output = new double[knots.size()];
		int i = 0;
		if (activeKnot >= 0) {
			Double d = knots.get(activeKnot).getValue();
			Collections.sort(knots);
			for (int j = 0; j < knots.size(); j++)
				if (knots.get(j).getValue().equals(d)) {
					activeKnot = j;
					break;
				}
		} else
			Collections.sort(knots);
		for (KnotPolygon p : knots)
			output[i++] = p.getValue().doubleValue();
		return output;
	}
	/**
	 * Returns knotvector
	 * Vrací uzlový vektor
	 * @return knotvector array
	 */
	public float[] getKnotsFloat(){
		float[] output=new float[knots.size()];
		int i=0;
		for(double d:getKnots()){
			output[i++]=(float) d;
		}
		return output;
	}

	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		drawBaseLine((Graphics2D) g);
		updateMultis((Graphics2D)g);
		drawKnots((Graphics2D) g);
	}

	/**
	 * Draws info about knot multiplicity
	 * Vykreslí informaci o násobnosti uzlů
	 * @param graphics2D object to draw to
	 */
	private void updateMultis(Graphics2D graphics2D) {
		Vector<Integer> vrcholy=new Vector<Integer>();
		for(int i=0;i<knots.size();i++){
			knots.get(i).setMulti(0);
			vrcholy.add(knots.get(i).xpoints[0]);
		}
		
		for(int i=0;i<knots.size();i++){
			//k.xpoints[0] //hodnota na X -> počet stejných hodnot nám zjistí multiplicitu
			for(Integer ii:vrcholy)
				if(ii.intValue()==knots.get(i).xpoints[0])
					knots.get(i).setMulti(knots.get(i).getMulti()+1);
		}
	}

	/**
	 * Sends event to all notified listeners
	 * Pošle všem navešeným listenerům událost
	 * @param ae event being sent
	 */
	private void notificateActionListeners(ActionEvent ae) {
		for (ActionListener a : actionListeners)
			a.actionPerformed(ae);
	}

	/**
	 * "Draws" knotvector
	 * Vykreslí reprezentaci uzlového vektoru
	 * @param g object to draw to
	 */
	private void drawKnots(Graphics2D g) {
		String txt;
//		int freq;
		for (KnotPolygon p : knots) {
			g.drawPolygon(p);
			g.drawString(p.getMulti()+"x",p.xpoints[1],top);
//			freq=Collections.frequency(xVector,Integer.valueOf(p.xpoints[0]));
//			g.drawString(freq+"x",p.xpoints[1],top-8);
		}
		g.rotate(Math.PI / 2);
		for (KnotPolygon p : knots) {
			txt = p.getValue().toString();
			if (txt.length() > 5)
				txt = txt.substring(0, 4);
			g.translate(top + 15, -p.xpoints[1]);
			g.drawString(txt, 0, 0);
			g.translate(-(top + 15), p.xpoints[1]);
		}
	}

	
	/**
	 * Draws baseline
	 * Vykreslí základní linku
	 * @param g object to draw to
	 */
	private void drawBaseLine(Graphics2D g) {
		g.drawLine(side, top, (int) (side + (this.getWidth() * .8)), top);
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#getMinimumSize()
	 */
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(100, 60);
	}

	/* (non-Javadoc)
	 * @see java.awt.Component#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(250, 60);
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
	 */
	public void componentResized(ComponentEvent e) {
		int width = this.getWidth();
		int height = this.getHeight();

		side = (int) (width * .1);
		top = (int) (height * .3);

		width *= .8;
		height *= .8;

		oneStep = 1d / (width);

		updateKnotPolygons();
		repaint();
	}

	/**
	 * Updates all objects representing knots
	 * Aktualizuje nastavení všech objektů reprezentujících uzly
	 */
	private void updateKnotPolygons() {
		for (KnotPolygon p : knots) {
			p.update(oneStep, top, side);
		}

	}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
	 */
	public void componentMoved(ComponentEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
	 */
	public void componentShown(ComponentEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
	 */
	public void componentHidden(ComponentEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
	 */
	public void mouseDragged(MouseEvent e) {
		
		if (activeKnot >= 0) {
//			preserveState();
			
			if (e.getX() >= side && e.getX() <= (getWidth() * .8 + side)) {
				knots.get(activeKnot).updateByX(e.getX());
			} else if (e.getX() < side)
				knots.get(activeKnot).updateByValue(new Double(0));
			else
				knots.get(activeKnot).updateByValue(new Double(1));

			notificateActionListeners(new ActionEvent(this,
					ActionEvent.ACTION_PERFORMED, KNOT_MOVED));
			repaint();
		}
	}

//	private void updateXVector() {
//		xVector.clear();
//		for(KnotPolygon p:knots){
//			xVector.add(Integer.valueOf(p.xpoints[0]));
//		}
//	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
	 */
	public void mouseMoved(MouseEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		KnotPolygon p;
		this.activeKnot = -1;
		for (int i = 0; i < knots.size(); i++) {
			p = knots.get(i);
			if (p.contains(x, y)) {
				activeKnot = i;
				break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
	 */
	public void mouseReleased(MouseEvent e) {
		this.activeKnot = -1;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
	 */
	public void mouseEntered(MouseEvent e) {
	}

	/* (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
	 */
	public void mouseExited(MouseEvent e) {
	}

//	/**
//	 * Nastaví uzlový vektor
//	 * @param knots nový uzlový vektor
//	 */
//	public void setKnots(double[] knots) {
//		preserveState();
//		for(double d:knots)
//			addKnot(new Double(d));
//		repaint();
//	}
	/**
	 * Sets knotvector
	 * Nastaví uzlový vektor
	 * @param knots new knotvector
	 */
	public void setKnots(float[] knots) {
//		preserveState();
		this.knots.clear();
		for(double d:knots)
			addKnot(new Double(d));
		repaint();
	}

	/**
	 * Checks whether knot multiplicity is not bigger than given value
	 * Zkontroluje, zda násobnost uzlů nepřekročila zadanou hodnotu
	 * @param maxMulti maximum multiplicity
	 * @return true if multiplicity is NOT bigger
	 */
	public boolean checkKnotMulti(int maxMulti) {
		updateMultis((Graphics2D) this.getGraphics());
		for(KnotPolygon p:knots)
			if(p.getMulti()>maxMulti)
				return false;
		return true;
	}

//	/**
//	 * Obnosví poslední uložený stav uzlového vektoru
//	 */
//	public void restoreState() {
//		knots.clear();
//		knots.addAll(previousState);
//		repaint();
//	}
}
