
package etomica.models.water;

import etomica.space.ISpace;
import etomica.units.Electron;
import etomica.units.Kelvin;

/** 
 * TIP4P potential for water.  All the real work happens in P2Water4P.
 */
public class P2WaterTIP4P extends P2Water4P {

    public P2WaterTIP4P(ISpace space) {
	    super(space, 3.1540, Kelvin.UNIT.toSim(78.02), Electron.UNIT.toSim(-1.04),
	            Electron.UNIT.toSim(0.52));
    }

    private static final long serialVersionUID = 1L;
}