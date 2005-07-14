package etomica.potential;

import etomica.AtomPair;
import etomica.AtomSet;
import etomica.AtomTypeLeaf;
import etomica.Debug;
import etomica.Default;
import etomica.EtomicaInfo;
import etomica.Space;
import etomica.space.CoordinatePairKinetic;
import etomica.space.ICoordinateKinetic;
import etomica.space.Tensor;
import etomica.space.Vector;

/**
 * Basic hard-(rod/disk/sphere) potential.
 * Energy is infinite if spheres overlap, and is zero otherwise.  Collision diameter describes
 * size of spheres.
 * Suitable for use in space of any dimension.
 *
 * @author David Kofke
 */
public class P2HardSphere extends Potential2HardSpherical {
    
   /**
    * Separation at which spheres first overlap
    */
   protected double collisionDiameter;
   
   /**
    * Square of collisionDiameter
    */
   protected double sig2;
   protected double lastCollisionVirial = 0.0;
   protected double lastCollisionVirialr2 = 0.0;
   protected final Vector dr;
   protected final Tensor lastCollisionVirialTensor;
   protected final CoordinatePairKinetic cPair;
    
    public P2HardSphere(Space space) {
        this(space, Default.ATOM_SIZE);
    }
    public P2HardSphere(Space space, double d) {
        super(space, new CoordinatePairKinetic(space));
        setCollisionDiameter(d);
        lastCollisionVirialTensor = space.makeTensor();
        dr = space.makeVector();
        cPair = (CoordinatePairKinetic)coordinatePair;
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Simple hard-sphere potential");
        return info;
    }
    
    public double getRange() {
    	return collisionDiameter;
    }

    /**
     * Time to collision of pair, assuming free-flight kinematics
     */
    public double collisionTime(AtomSet pair, double falseTime) {
        Vector dv = cPair.resetV((AtomPair)pair);
        dr.Ev1Pa1Tv2(cPair.reset(),falseTime,dv);
        double bij = dr.dot(dv);
        double time = Double.POSITIVE_INFINITY;

        if(bij < 0.0) {
        	if (Default.FIX_OVERLAP && dr.squared() < sig2) return falseTime;
            double v2 = dv.squared();
            double discriminant = bij*bij - v2 * ( dr.squared() - sig2 );
            if(discriminant > 0) {
                time = (-bij - Math.sqrt(discriminant))/v2;
            }
        }
        if (Debug.ON && Debug.DEBUG_NOW && (Debug.allAtoms(pair) || time < 0.0)) {
        	System.out.println("atoms "+pair+" r2 "+dr.squared()+" bij "+bij+" time "+time);
        	if (time < 0.0) throw new RuntimeException("negative collision time for hard spheres");
        }
        return time + falseTime;
    }
    
    /**
     * Implements collision dynamics and updates lastCollisionVirial
     */
    public void bump(AtomSet atoms, double falseTime) {
        AtomPair pair = (AtomPair)atoms;
        dr.E(cPair.reset(pair));
        Vector dv = cPair.resetV();
        dr.PEa1Tv1(falseTime,dv);
        double r2 = dr.squared();
        double bij = dr.dot(dv);
        double rm0 = ((AtomTypeLeaf)pair.atom0.type).rm();
        double rm1 = ((AtomTypeLeaf)pair.atom1.type).rm();
        double reducedMass = 2.0/(rm0 + rm1);
        lastCollisionVirial = reducedMass*bij;
        lastCollisionVirialr2 = lastCollisionVirial/r2;
        //dv is now change in velocity due to collision
        dv.Ea1Tv1(lastCollisionVirialr2,dr);
        ((ICoordinateKinetic)pair.atom0.coord).velocity().PEa1Tv1( rm0,dv);
        ((ICoordinateKinetic)pair.atom1.coord).velocity().PEa1Tv1(-rm1,dv);
        pair.atom0.coord.position().PEa1Tv1(-falseTime*rm0,dv);
        pair.atom1.coord.position().PEa1Tv1( falseTime*rm1,dv);
    }
    
    public double lastCollisionVirial() {
        return lastCollisionVirial;
    }
    
    public Tensor lastCollisionVirialTensor() {
        lastCollisionVirialTensor.E(dr, dr);
//        lastCollisionVirialTensor.DE(lastCollisionVirialr2);
        return lastCollisionVirialTensor;        
    }
    
    /**
     * Accessor method for collision diameter
     */
    public double getCollisionDiameter() {return collisionDiameter;}
    /**
     * Accessor method for collision diameter
     */
    public void setCollisionDiameter(double c) {
        collisionDiameter = c;
        sig2 = c*c;
    }
    public etomica.units.Dimension getCollisionDiameterDimension() {
        return etomica.units.Dimension.LENGTH;
    }
    
    /**
     * Interaction energy of the pair.
     * Zero if separation is greater than collision diameter, infinity otherwise
     */
    public double u(double r2) {
        return (r2 < sig2) ? Double.POSITIVE_INFINITY : 0.0;
    }
    
    public double energyChange() {return 0.0;}
    
}//end of P2HardSphere