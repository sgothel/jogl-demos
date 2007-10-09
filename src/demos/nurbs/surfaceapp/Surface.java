package demos.nurbs.surfaceapp;

import java.io.File;
import java.util.Vector;

import simple.xml.Element;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.Serializer;
import simple.xml.load.Persister;

/**
 * Třída definice NURBS plochy, vystavěna podle návrhového vzoru Singleton
 * @author Tomáš Hráský
 *
 */
@Root(name="surface")
public class Surface {
  /**
   * Odkaz na instanci třídy
   */
  private static Surface singleton;
  /**
   * Indikuje, zda je zadání plochy kompletní
   */
  @Element(name="finished")
    private boolean isSurfaceFinished=false;

  /**
   * Index aktuálního vybraného řídícího bodu
   */
  private int bodIndex = -1;

  /**
   * Stupeň plocy ve směru parametru U
   */
  @Element(name="orderU")
    private int orderU=3;
	
  /**
   * Stupeň plochy ve směru parametru V
   */
  @Element(name="orderV")
    private int orderV=3;
	
  /**
   * Počet řídících bodů ve směru parametru V
   */
  @Element(name="pointsInV")
    private int pointsInV=4;
	
  /**
   * Počet řídících bodů ve směru parametru U
   */
  @Element(name="pointsInU")
    private int pointsInU=4;

	
  /**
   * Pole souřadnic řídícíh bodů 
   * 
   */
  private float[] ctrlPoints
    //	={
    //	     -150f,-150f, 400f,1f,
    //	     -50f,-150f, 200f, 1f,
    //	     50f,-150f,-100f,  1f,
    //	     150f,-150f, 200f,1f,
    //	     -150f,-50f, 100f, 1f,
    //	     -50f,-50f, 300f, 1f,
    //	     50f,-50f, 0f,  1f,
    //	     150f,-50f,-100f,1f,
    //	     -150f, 50f, 400f, 1f,
    //	     -50f, 50f, 0f, 1f,
    //	     50f, 50f, 300f,  1f,
    //	     150f, 50f, 400f,1f,
    //	     -150f, 150f,-200f, 1f,
    //	     -50f, 150f,-200f, 1f,
    //	     50f, 150f, 0f,  1f,
    //	     150f, 150f,-100f,1f}
    ;
  /**
   * Pole hodnot uzlového vektoru ve směru parametru U
   */
  private	float knotsU[]
    //	       	            ={0.0f, 0.0f, 0.0f, 0.0f,
    //            1f, 1f, 1.0f, 1.0f}
    ;
	
  /**
   * Pole hodnot uzlového vektoru ve směru parametru V
   */
  private	float knotsV[]
    //	       	            ={0.0f, 0.0f, 0.0f, 0.0f,
    //            1f, 1f, 1.0f, 1.0f}
    ;
	
  /**
   * Kolekce vektor pro persistenci souřadnic řídících bodů
   */
  @ElementList(name="ctrlpoints",type=MyFloat.class)
    private Vector<MyFloat> ctrlVector;
	
  /**
   * Kolekce vektor pro persistenci uzlového vektoru ve směru parametru U
   */
  @ElementList(name="knotsU",type=MyFloat.class)
    private Vector<MyFloat> knotVectorU;
	
  /**
   * Kolekce vektor pro persistenci uzlového vektoru ve směru parametru V
   */
  @ElementList(name="knotsV",type=MyFloat.class)
    private Vector<MyFloat> knotVectorV;
	
  /**
   * Vytvoří prázdnou definici plochy
   */
  public void clear(){
    isSurfaceFinished=false;
    ctrlPoints=new float[0];
    knotsU=new float[0];
    knotsV=new float[0];
    orderU=3;
    orderV=3;
    pointsInU=0;
    pointsInV=0;
  }
	
  /**
   * Pomocí framweorku Simple serializuje definici křivky do XML souboru
   * @param f soubor pro uložení
   */
  public void persist(File f){
    ctrlVector=new Vector<MyFloat>(ctrlPoints.length);
    knotVectorU=new Vector<MyFloat>(knotsU.length);
    knotVectorV=new Vector<MyFloat>(knotsV.length);
		
    for(Float ff:ctrlPoints)
      ctrlVector.add(new MyFloat(ff));
		
    for(Float ff:knotsU)
      knotVectorU.add(new MyFloat(ff));
		
    for(Float ff:knotsV)
      knotVectorV.add(new MyFloat(ff));
		
    Serializer s=new Persister(); 
    try {
      System.out.println("ukládám");
      s.write(Surface.getInstance(),f);
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
    Surface c=s.read(Surface.class,f);
    initFromSurface(c);
  }
	
  /**
   * Inicializuje objekt podle jiného objektu typu Curve
   * @param c referenční objekt - křivka
   */
  private void initFromSurface(Surface c) {
    this.orderU=c.getOrderU();
    this.orderV=c.getOrderV();
    this.ctrlPoints=new float[c.getCtrlVector().size()];
    this.knotsU=new float[c.getKnotVectorU().size()];
    this.knotsV=new float[c.getKnotVectorV().size()];
    int i=0;
    for(MyFloat f:c.getCtrlVector())
      ctrlPoints[i++]=f.getValue();
    i=0;
    for(MyFloat f:c.getKnotVectorU())
      knotsU[i++]=f.getValue();
    i=0;
    for(MyFloat f:c.getKnotVectorV())
      knotsV[i++]=f.getValue();
		
    this.pointsInU=c.getPointsInU();
    this.pointsInV=c.getPointsInV();
		
    this.isSurfaceFinished=c.isSurfaceFinished();
  }

  /**
   * Konstruktor, nastaví prázdné hodnoty polí definujících NURBS plochu
   */
  private Surface(){
    //		ctrlPoints=new float[0];
    //		knotsU=new float[0];
    //		knotsV=new float[0];
    //		isSurfaceFinished=false;
    clear();
  }
	
  /**
   * Vrací instanci třídy (podle návrhového vzoru Singleton)
   * @return instance třídy Curve
   */
  public static Surface getInstance() {
    if (singleton == null)
      singleton = new Surface();
    return singleton;

  }
	
  /**
   * Vrací pole uzlového vektoru ve směru parametru U
   * @return pole hodnot uzlového vektoru ve směru parametru U
   */
  public float[] getKnotsU() {
    return this.knotsU;
  }

  /**
   * Vrací pole s hodnotami souřadnic řídících bodů
   * @return pole souřadnic řídících bodů
   */
  public float[] getCtrlPoints() {
    return this.ctrlPoints;
  }

  /**
   * Vrací stupeň NURBS plochy ve směru parametru U
   * @return stupeň NURBS plochy ve směru parametru U
   */
  public int getOrderU() {
    return this.orderU;
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
   * Vrací Z-ovou souadnici aktuálně vybraného řídícího bodu, přepočítává hodnotu z homogenních souřadnic
   * @return Z-ová souadnice aktuálně vybraného řídícího bodu
   */
  public float getActiveZ(){
    if(bodIndex>=0){
      return ctrlPoints[bodIndex*4+2]/ctrlPoints[bodIndex*4+3];
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
   * Nastavuje Z-ovou souadnici aktuálně vybraného řídícího bodu, přepočítává hodnotu do homogenních souřadnic
   * @param z Z-ová souřadnice aktuálně vybraného řídícího bodu
   */
	
  public void setActiveZ(float z){
    if(bodIndex>=0){
      ctrlPoints[bodIndex*4+2]=z*ctrlPoints[bodIndex*4+3];
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
        ctrlPoints[bodIndex*4+2]=ctrlPoints[bodIndex*4+2]/oldW*w;
      }
			
    }
  }

  /**
   * Nastavuje uzlový vektor ve směru parametru U
   * @param knots nový uzlový vektor ve směru parametru U
   */
  public void setKnotsU(float[] knots) {
    this.knotsU = knots;
  }

  /**
   * Vrací informaci o stavu dokončení definice křvky
   * @return true pokud je definice křivky kompletní, jinak false
   */
  public boolean isSurfaceFinished() {
    return isSurfaceFinished;
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
  public void setIsSurfaceFinished(boolean b) {
    isSurfaceFinished=b;
  }

  /**
   * Vrací vektor souřadnic řídích bodů pro serializaci
   * @return vektor souřadnic řídících bodů
   */
  private Vector<MyFloat> getCtrlVector() {
    return ctrlVector;
  }

  /**
   * Vrací vektor prvků uzlového vektoru ve směru parametru U pro serializaci
   * @return vektor prvků uzlového vektoru ve směru parametru U
   */
  private Vector<MyFloat> getKnotVectorU() {
    return knotVectorU;
  }

  /**
   * Vrací stupeň plochy ve směru parametru U
   * @param order stupeň plochy ve směru parametru U
   */
  public void setOrderU(int order) {
    this.orderU = order;
  }
  /**
   * Vrací pole uzlového vektoru ve směru parametru V
   * @return pole hodnot uzlového vektoru ve směru parametru V
   */
  public float[] getKnotsV() {
    return knotsV;
  }
  /**
   * Nastavuje uzlový vektor ve směru parametru V
   * @param knotsV nový uzlový vektor ve směru parametru V
   */
  public void setKnotsV(float[] knotsV) {
    this.knotsV = knotsV;
  }
  /**
   * Vrací stupeň plochy ve směru parametru V
   * @return stupeň plochy ve směru parametru V
   */
  public int getOrderV() {
    return orderV;
  }
  /**
   * Nastavuje stupeň NURBS plochy ve směru parametru V
   * @param orderV stupeň plochy ve směru parametru V
   */
  public void setOrderV(int orderV) {
    this.orderV = orderV;
  }
  /**
   * Vrací vektor prvků uzlového vektoru ve směru parametru V pro serializaci
   * @return vektor prvků uzlového vektoru ve směru parametru V
   */
  private Vector<MyFloat> getKnotVectorV() {
    return knotVectorV;
  }

  /**
   * Vrací počet řídících bodů ve směru parametru V (tj. počet sloupců)
   * @return počet řídících bodů ve směru parametru V
   */
  public int getPointsInV() {
    return pointsInV;
  }

  /**
   * Nastavuje počet řídících bodů ve směru parametru V
   * @param pointsInV počet řídících bodů ve směru parametru V
   */
  public void setPointsInV(int pointsInV) {
    this.pointsInV = pointsInV;
  }

  /**
   * Vrací počet řídících bodů ve směru parametru U (tj. počet řádků)
   * @return počet řídících bodů ve směru parametru U

  */
  public int getPointsInU() {
    return pointsInU;
  }

  /**
   * Nastavuje počet řídících bodů ve směru parametru U
   * @param pointsInU počet řídících bodů ve směru parametru U
   */
  public void setPointsInU(int pointsInU) {
    this.pointsInU = pointsInU;
  }
}
