package etomica.potential;

import etomica.atom.AtomLeaf;
import etomica.atom.AtomSet;
import etomica.phase.Phase;
import etomica.space.IVectorRandom;
import etomica.space.NearestImageTransformer;
import etomica.space.Space;
import etomica.units.Angle;
import etomica.units.Dimension;
import etomica.units.Energy;

/**
 * Simple 3-body soft bond-angle potential 
 * @author andrew
 */
public class P3BondAngle extends Potential {

    public P3BondAngle(Space space) {
        super(3, space);
        dr12 = space.makeVector();
        dr23 = space.makeVector();
        setAngle(Math.PI);
    }

    public void setPhase(Phase phase) {
        nearestImageTransformer = phase.getBoundary();
    }

    public double energy(AtomSet atomSet) {
        AtomLeaf atom0 = (AtomLeaf)atomSet.getAtom(0);
        AtomLeaf atom1 = (AtomLeaf)atomSet.getAtom(1);
        AtomLeaf atom2 = (AtomLeaf)atomSet.getAtom(2);
        dr12.Ev1Mv2(atom1.getCoord().getPosition(),atom0.getCoord().getPosition());
        dr23.Ev1Mv2(atom2.getCoord().getPosition(),atom1.getCoord().getPosition());
        nearestImageTransformer.nearestImage(dr12);
        nearestImageTransformer.nearestImage(dr23);
        double costheta = -dr12.dot(dr23)/Math.sqrt(dr12.squared()*dr23.squared());
        double dtheta;
        // machine precision can give us numbers with magnitudes slightly greater than 1
        if (costheta > 1) {
            dtheta = 0;
        }
        else if (costheta < -1) {
            dtheta = Math.PI;
        }
        else {
            dtheta = Math.acos(costheta);
        }
        dtheta -= angle;
        return epsilon*dtheta*dtheta;
    }

    /**
     * Sets the nominal bond angle (in radians)
     */
    public void setAngle(double newAngle) {
        angle = newAngle;
    }
    
    /**
     * Returns the nominal bond angle (in radians)
     */
    public double getAngle() {
        return angle;
    }
    
    public Dimension getAngleDimension() {
        return Angle.DIMENSION;
    }

    /**
     * Sets the characteristic energy of the potential
     */
    public void setEpsilon(double newEpsilon) {
        epsilon = newEpsilon;
    }
    
    /**
     * Returns the characteristic energy of the potential
     */
    public double getEpsilon() {
        return epsilon;
    }
    
    public Dimension getEpsilonDimension() {
        return Energy.DIMENSION;
    }
    
    public double getRange() {
        return Double.POSITIVE_INFINITY;
    }

    protected final IVectorRandom dr12, dr23;
    protected NearestImageTransformer nearestImageTransformer;
    protected double angle;
    protected double epsilon;
    private static final long serialVersionUID = 1L;
}
