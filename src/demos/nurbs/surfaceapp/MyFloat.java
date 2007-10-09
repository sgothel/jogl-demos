package demos.nurbs.surfaceapp;

import simple.xml.Attribute;
import simple.xml.Root;

/**
 * Class for serializing decimal point number using SimpleXML
 * Třída umožňující serializaci desetinného čísla ve formátu plovoucí čárky (float)
 * @author Tomáš Hráský
 *
 */
@Root(name="floatval")
public class MyFloat {
  /**
   * Value
   * Hodnota
   */
  @Attribute(name="val")
    private float value;

  /**
   * Constructor, sets value to 0
   * Konstrktor, hodnota je defaultně 0 
   */
  public MyFloat(){
    value=0;
  }
	
  /**
   * Creates instance with specified value
   * Vytvoří instanci objektu s požadovanou hodnotou
   * @param f value
   */
  public MyFloat(float f) {
    value = f;
  }

  /**
   * Returns value of decimal number
   * Vrací hodnotu des. čísla
   * @return value
   */
  public float getValue() {
    return value;
  }

  /**
   * Sets value
   * Nastavuje hodnotu objektu
   * @param value value
   */
  public void setValue(float value) {
    this.value = value;
  }
}
