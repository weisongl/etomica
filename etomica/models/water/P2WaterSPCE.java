
package etomica.models.water;

import etomica.atom.AtomPair;
import etomica.atom.AtomSet;
import etomica.phase.Phase;
import etomica.potential.Potential2;
import etomica.space.IVectorRandom;
import etomica.space.IVector;
import etomica.space.Space;
import etomica.units.Electron;
import etomica.units.Kelvin;

/** 
 * 
 * @author kofke
 *
 * SPC/E potential for water.  Requires the molecule node be an
 * AtomTreeNodeWater.  Does not apply periodic boundary conditions.
 */
public class P2WaterSPCE extends Potential2 {

    public P2WaterSPCE(Space space) {
	    super(space);
	    setSigma(3.1670);
	    setEpsilon(Kelvin.UNIT.toSim(78.21));
	    setCharges();
    }   

    public double energy(AtomSet atoms){
        double sum = 0.0;
        double r2 = 0.0;

        AtomPair pair = (AtomPair)atoms;
        AtomTreeNodeWater3P node1 = (AtomTreeNodeWater3P)pair.atom0.getNode();
        AtomTreeNodeWater3P node2 = (AtomTreeNodeWater3P)pair.atom1.getNode();

        IVectorRandom O1r = node1.O.getCoord().getPosition();
        IVector O2r = node2.O.getCoord().getPosition();
        IVectorRandom H11r = node1.H1.getCoord().getPosition();
        IVectorRandom H12r = node1.H2.getCoord().getPosition();
        IVector H21r = node2.H1.getCoord().getPosition();
        IVector H22r = node2.H2.getCoord().getPosition();

        final double core = 0.1;

        //compute O-O distance to consider truncation   
        r2 = O1r.Mv1Squared(O2r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeOO/Math.sqrt(r2);
        double s2 = sigma2/(r2);
        double s6 = s2*s2*s2;
        sum += epsilon4*s6*(s6 - 1.0);

        r2 = O1r.Mv1Squared(H21r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeOH/Math.sqrt(r2);

        r2 = O1r.Mv1Squared(H22r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeOH/Math.sqrt(r2);

        r2 = H11r.Mv1Squared(O2r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeOH/Math.sqrt(r2);

        r2 = H11r.Mv1Squared(H21r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeHH/Math.sqrt(r2);

        r2 = H11r.Mv1Squared(H22r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeHH/Math.sqrt(r2);

        r2 = H12r.Mv1Squared(O2r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeOH/Math.sqrt(r2);

        r2 = H12r.Mv1Squared(H21r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeHH/Math.sqrt(r2);

        r2 = H12r.Mv1Squared(H22r);
        if(r2<core) return Double.POSITIVE_INFINITY;
        sum += chargeHH/Math.sqrt(r2);

        return sum;																					        
    }//end of energy

    public double getSigma() {return sigma;}

    private final void setSigma(double s) {
        sigma = s;
        sigma2 = s*s;
    }

    public final double getRange() {
        return Double.POSITIVE_INFINITY;
    }
    
    public double getEpsilon() {return epsilon;}
    
    private final void setEpsilon(double eps) {
        epsilon = eps;
        epsilon4 = 4*epsilon;
    }
    private final void setCharges() {
        chargeOO = chargeO * chargeO;
        chargeOH = chargeO * chargeH;
        chargeHH = chargeH * chargeH;
    }

    public void setPhase(Phase phase) {
    }

    private static final long serialVersionUID = 1L;
    public double sigma , sigma2;
    public double epsilon, epsilon4;
    private double chargeH = Electron.UNIT.toSim(0.4238);
    private double chargeO = Electron.UNIT.toSim(-0.8476);
    private double chargeOO, chargeOH, chargeHH;
}