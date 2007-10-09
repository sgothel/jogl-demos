package demos.nurbs.curveapp;

import java.io.File;
import java.util.Vector;

import simple.xml.Element;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.Serializer;
import simple.xml.load.Persister;

/**
 * Třída definice NURBS křivky, vystavěna podle návrhového vzoru Singleton
 * @author Tomáš Hráský
 *
 */
@Root(name="curve")
public class Curve
{
  /**
   * Odkaz na instanci třídy
   */
  private static Curve singleton;
  /**
   * Indikuje, zda je zadání křivky kompletní
   */
  @Element(name="finished")
    private boolean isCurveFinished;

  /**
   * Index aktuálního vybraného řídícího bodu
   */
  private int bodIndex = -1;

  /**
   * Stupeň křivky
   */
  @Element(name="order")
    private int order=3;
	
  /**
   * Pole souřadnic řídícíh bodů 
   * 
   */
  private float[] ctrlPoints;
	
  /**
   * Pole hodnot uzlového vektoru
   */
  private	float knots[];

  /**
   * Kolekce vektor pro persistenci souřadnic řídících bodů
   */
  @ElementList(name="ctrlpoints",type=MyFloat.class)
    private Vector<MyFloat> ctrlVector;
	
  /**
   * Kolekce vektor pro persistenci uzlového vektoru
   */
  @ElementList(name="knots",type=MyFloat.class)
    private Vector<MyFloat> knotVector;
	
  /**
   * Vytvoří prázdnou definici křivky
   */
  public void clear(){
    isCurveFinished=false;
    ctrlPoints=new float[0];
    knots=new float[0];
    order=3;
  }
	
  /**
   * Pomocí framweorku Simple serializuje definici křivky do XML souboru
   * @param f soubor pro uložení
   */
  public void persist(File f){
    ctrlVector=new Vector<MyFloat>(ctrlPoints.length);
    knotVector=new Vector<MyFloat>(knots.length);
		
    for(Float ff:ctrlPoints)
      ctrlVector.add(new MyFloat(ff));
		
    for(Float ff:knots)
      knotVector.add(new MyFloat(ff));
		
    Serializer s=new Persister(); 
    try {
      System.out.println("ukládám");
      s.write(Curve.getInstance(),f);
    } catch (Exception e1) {
      e1.printStackTrace();
    }


  }
	
  /**
   * Vytvoří pomocí frameworku Simple křivku z definice uložené v XML souboru 
   * @param f soubor,z něhož se má definice načíst
   * @throws Exception chyba při čtení ze souboru
   */
  public void unPersist(File f) throws Exception{
    Serializer s=new Persister();
    Curve c=s.read(Curve.class,f);
    initFromCurve(c);
  }
	
  /**
   * Inicializuje objekt podle jiného objektu typu Curve
   * @param c referenční objekt - křivka
   */
  private void initFromCurve(Curve c) {
    this.order=c.getOrder();
    this.ctrlPoints=new float[c.getCtrlVector().size()];
    this.knots=new float[c.getKnotVector().size()];
    int i=0;
    for(MyFloat f:c.getCtrlVector())
      ctrlPoints[i++]=f.getValue();
    i=0;
    for(MyFloat f:c.getKnotVector())
      knots[i++]=f.getValue();
		
    this.isCurveFinished=c.isCurveFinished();
  }

  /**
   * Konstruktor, nastaví prázdné hodnoty polí definujících NURBS křivku
   */
  private Curve(){
    ctrlPoints=new float[0];
    knots=new float[0];
    isCurveFinished=false;
  }
	
  /**
   * Vrací instanci třídy (podle návrhového vzoru Singleton)
   * @return instance třídy Curve
   */
  public static Curve getInstance() {
    if (singleton == null)
      singleton = new Curve();
    return singleton;

  }
	
  /**
   * Vrací pole uzlového vektoru
   * @return pole hodnot uzlového vektoru
   */
  public float[] getKnots() {
    return this.knots;
  }

  /**
   * Vrací pole s hodnotami souřadnic řídících bodů
   * @return pole souřadnic řídících bodů
   */
  public float[] getCtrlPoints() {
    return this.ctrlPoints;
  }

  /**
   * Vrací stupeň NURBS křivky
   * @return stupeň NURBS křivky
   */
  public int getOrder() {
    return this.order;
  }

  /**
   * Vrací index aktuálně vybraného řídícího bodu
   * @return index aktuálně vybraného řídícího bodu
   */
  public int getBodIndex() {
    return bodIndex;
  }

  /**
   * Nastavuje index požadovaného aktuálně vybraného řídícího bodu
   * @param bodIndex index požadovaného aktuálně vybraného řídícího bodu
   */
  public void setBodIndex(int bodIndex) {
    this.bodIndex = bodIndex;
  }
	
  /**
   * Vrací X-ovou souadnici aktuálně vybraného řídícího bodu, přepočítává hodnotu z homogenních souřadnic
   * @return X-ová souadnice aktuálně vybraného řídícího bodu
   */
  public float getActiveX(){
    if(bodIndex>=0){
      return ctrlPoints[bodIndex*4]/ctrlPoints[bodIndex*4+3];
    }
    else return 0;
  }
  /**
   * Vrací Y-ovou souadnici aktuálně vybraného řídícího bodu, přepočítává hodnotu z homogenních souřadnic
   * @return Y-ová souadnice aktuálně vybraného řídícího bodu
   */
  public float getActiveY(){
    if(bodIndex>=0){
      return ctrlPoints[bodIndex*4+1]/ctrlPoints[bodIndex*4+3];
    }
    else return 0;
  }
	
  /**
   * Vrací váhu aktuálně vybraného řídícího bodu
   * @return váha aktuálně vybraného řídícího bodu
   */
  public float getActiveW(){
    if(bodIndex>=0){
      return ctrlPoints[bodIndex*4+3];
    }
    else return 0;
  }
	
  /**
   * Nastavuje X-ovou souadnici aktuálně vybraného řídícího bodu, přepočítává hodnotu do homogenních souřadnic
   * @param x X-ová souřadnice aktuálně vybraného řídícího bodu
   */
  public void setActiveX(float x){
    if(bodIndex>=0){
      ctrlPoints[bodIndex*4]=x*ctrlPoints[bodIndex*4+3];
    }
  }
  /**
   * Nastavuje Y-ovou souadnici aktuálně vybraného řídícího bodu, přepočítává hodnotu do homogenních souřadnic
   * @param y Y-ová souřadnice aktuálně vybraného řídícího bodu
   */
	
  public void setActiveY(float y){
    if(bodIndex>=0){
      ctrlPoints[bodIndex*4+1]=y*ctrlPoints[bodIndex*4+3];
    }
  }
	
  /**
   *Nastavuje váhu aktuálně vybraného řídícího bodu, upravuje hodnoty stávajícíh souřadic vzhledem k váze a použití homogenních souřadnic 
   * @param w váha aktuálně vybraného řídícího bodu
   */
  public void setActiveW(float w){
    if(bodIndex>=0){
      float oldW=ctrlPoints[bodIndex*4+3];
      if(w>0){
        ctrlPoints[bodIndex*4+3]=w;
        //úprava souřadnic
        ctrlPoints[bodIndex*4]=ctrlPoints[bodIndex*4]/oldW*w;
        ctrlPoints[bodIndex*4+1]=ctrlPoints[bodIndex*4+1]/oldW*w;
      }
			
    }
  }

  /**
   * Nastavuje uzlový vektor
   * @param knots nový uzlový vektor
   */
  public void setKnots(float[] knots) {
    this.knots = knots;
  }

  /**
   * Vrací informaci o stavu dokončení definice křvky
   * @return true pokud je definice křivky kompletní, jinak false
   */
  public boolean isCurveFinished() {
    return isCurveFinished;
  }

  /**
   * Nastavuje řídící body
   * @param ctrlPoints pole souřadnic řídících bodů 
   */
  public void setCtrlPoints(float[] ctrlPoints) {
    this.ctrlPoints = ctrlPoints;
  }

  /**
   * Nastavuje stav dokončení definice křivky
   * @param b stav dokončení definice křivky
   *
   */
  public void setIsCurveFinished(boolean b) {
    isCurveFinished=b;
  }

  /**
   * Vrací vektor souřadnic řídích bodů pro serializaci
   * @return vektor souřadnic řídících bodů
   */
  private Vector<MyFloat> getCtrlVector() {
    return ctrlVector;
  }

  /**
   * Vrací vektor prvků uzlového vektoru pro serializaci
   * @return vektor prvků uzlového vektoru
   */
  private Vector<MyFloat> getKnotVector() {
    return knotVector;
  }

  /**
   * Nastaví stupeň křivky
   * @param order požadovaný stupeň
   */
  public void setOrder(int order) {
    this.order = order;
  }
}
