/*
 * History
 * Created on Nov 18, 2004 by kofke
 */
package etomica.action;

import etomica.Atom;
import etomica.Space;
import etomica.Space.Vector;


public class AtomTranslate extends AtomActionAdapter {
    protected Space.Vector displacement;
        
    public AtomTranslate(Space space) {
        super();
        displacement = space.makeVector();
    }
        
    public final void actionPerformed(Atom a) {a.coord.position().PE(displacement);}
    public void actionPerformed(Atom a, Space.Vector d) {a.coord.position().PE(d);}
    public final void setDisplacement(Space.Vector d) {displacement.E(d);}
}