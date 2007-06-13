package etomica.graphics2;



import etomica.atom.IAtom;
/**
* Simplest color scheme - colors all atoms with baseColor. 
* @author Henrique
*/
public class ColorSchemeSimple implements ColorScheme, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    public ColorSchemeSimple(Color color) { setBaseColor(color); }
    public int atomColor(IAtom a) {return 0;}
    
    public final void setBaseColor(Color c) {baseColor = c;}
    public final Color getBaseColor() {return baseColor;}
    protected Color baseColor;
	/**
	 * @see etomica.graphics2.ColorScheme#getNumColors()
	 */
	public int getNumColors() {
		return 1;
	}
	/* (non-Javadoc)
	 * @see etomica.graphics2.ColorScheme#getColor(int)
	 */
	public Color getColor(int index) {
		// TODO Auto-generated method stub
		return baseColor;
	}
}//end of Simple