package etomica.modules.chainequilibrium;
import etomica.atom.Atom;
import etomica.atom.AtomLeaf;
import etomica.atom.AtomPair;
import etomica.atom.AtomSet;
import etomica.atom.AtomTypeLeaf;
import etomica.phase.Phase;
import etomica.potential.P2SquareWell;
import etomica.space.ICoordinateKinetic;
import etomica.space.Space;


/**
 * Similar to square-well potential, but considers and alters bonding states
 * with collisions. Each atom may bind, in the form of the square-well
 * attraction, to one other atom. Two atoms approaching each other with this
 * potential may interact as follows:
 * <ul>
 * <li>If neither is bound, or if they are bound to each other, they will
 * interact as square-well atoms.
 * <li>If one or both is bound, each to another atom, they will act as hard
 * spheres of diameter equal to the well diameter.
 * </ul>
 * The potential is similar to P2SquareWellBondedBarrier, but there is no
 * accounting for a barrier, and no possibility for one atom to dislodge the
 * bonding partner of another directly in a single collision.
 * 
 * @author David Kofke
 */
public class P2SquareWellBonded extends P2SquareWell {

    private static final long serialVersionUID = 1L;
    private ReactionEquilibrium agentSource;
	private Atom[][] agents;
    private Phase phase;

	public P2SquareWellBonded(Space space, ReactionEquilibrium sim, double coreDiameter,double lambda, double epsilon) {
		super(space, coreDiameter, lambda, epsilon, true);
		agentSource = sim;
	}

    public void setPhase(Phase newPhase){
        phase = newPhase;
        super.setPhase(phase);
    }
    
	/**
     * This function will tell the user, if passed an atom weither or not that atom can bond
	 */
	protected boolean full(Atom a) {
		int j = agents[a.getGlobalIndex()].length;	//check INDEXING
		for(int i=0; i != j; ++i){
			if (agents[a.getGlobalIndex()][i] == null) {
				return false;
			}
		}
		return true; 
	}
	
	/**
     * This will tell you what the lowest open space is in atom a
	 */
	protected int lowest(Atom a){
		int j = agents[a.getGlobalIndex()].length;	//check INDEXING
		for(int i=0; i != j; ++i){
			if (agents[a.getGlobalIndex()][i] == null) {
				return i;
			}
		}
		return j; 
	}
	
	/**
     * This function tells you if two atoms are bonded
     * This could probably be public, although a public version would
     * need to first re-retrieve agents
	 */
	protected boolean areBonded(Atom a, Atom b){
		int j = agents[a.getGlobalIndex()].length;	//check INDEXING
		for(int i=0; i != j; ++i){
			if (agents[a.getGlobalIndex()][i] == b){		
				return true;
			}
		}
		return false; 	
	}

	/**
     * this function will bond atoms a & b together
	 */
	protected void bond(Atom a, Atom b){
		if (areBonded(a,b)){			// Error Checking, what about double bonds?
			return;
		}
		int i = lowest(a);		// (0 is the First Space) 
		int j = lowest(b);
		agents[a.getGlobalIndex()][i] = b;
		agents[b.getGlobalIndex()][j] = a;
	}
	
	/**
     * this function unbonds two atoms
	 */
	protected void unbond(Atom a, Atom b){
		if (!areBonded(a,b)){		// Error Checking
			return;
		}
        boolean success = false;
		// Unbonding the Atom, Atom A's side
		int j = agents[a.getGlobalIndex()].length;	//check INDEXING
		for(int i=0; i != j; ++i){
			if (agents[a.getGlobalIndex()][i] == b){	// double bonds???
				agents[a.getGlobalIndex()][i] = null;
                success = true;
			}
		}
        if (!success) {
            throw new RuntimeException("oops #1 "+b+" not in "+a+" list");
        }
        success = false;
		// Unbonding the Atom, Atom B's side
		j = agents[b.getGlobalIndex()].length;	//check INDEXING
		for(int i=0; i != j; ++i){
			if (agents[b.getGlobalIndex()][i] == a){	// double bonds???
				agents[b.getGlobalIndex()][i] = null;
                success = true;
			}
		}
        if (!success) {
            throw new RuntimeException("oops #2 "+b+" not in "+a+" list");
        }
	}
	
	/**
	 * Computes next time of collision of the two atoms, assuming free-flight
	 * kinematics.
	 */
	public double collisionTime(AtomSet atoms, double falseTime) {
        agents = agentSource.getAgents(phase);
	
// ************ This gets run all the time!! More than Bump Method
		//System.out.println("P2SquaredWell: ran Collision Time");	
		if (ignoreOverlap) {
			
            // ** Makes 2 things, and atomPair pair, 
            AtomPair pair = (AtomPair) atoms;
            
            ICoordinateKinetic coord0 = (ICoordinateKinetic)((AtomLeaf)pair.atom0).getCoord();
            ICoordinateKinetic coord1 = (ICoordinateKinetic)((AtomLeaf)pair.atom1).getCoord();
            dv.Ev1Mv2(coord1.velocity(), coord0.velocity());
            
            dr.Ev1Mv2(coord1.position(), coord0.position());
            dr.PEa1Tv1(falseTime,dv);
            nearestImageTransformer.nearestImage(dr);

			double r2 = dr.squared();
			double bij = dr.dot(dv);
			boolean areBonded = areBonded(pair.atom0,pair.atom1);
			//inside well but not mutually bonded; collide now if approaching
            if (!areBonded && r2 < wellDiameterSquared) {
                return (bij < 0) ? falseTime : Double.POSITIVE_INFINITY;
            }
		}
		//mutually bonded, or outside well; collide as SW
		double time = super.collisionTime(atoms, falseTime);
//		if(!Double.isInfinite(time)) System.out.println("Collision time: "+time+" for "+atoms.toString());
		return time;
	}

	
	public void bump(AtomSet pair, double falseTime) {

        AtomLeaf atom0 = (AtomLeaf)((AtomPair)pair).atom0;
        AtomLeaf atom1 = (AtomLeaf)((AtomPair)pair).atom1;
        ICoordinateKinetic coord0 = (ICoordinateKinetic)atom0.getCoord();
        ICoordinateKinetic coord1 = (ICoordinateKinetic)atom1.getCoord();
        dv.Ev1Mv2(coord1.velocity(), coord0.velocity());
        
        dr.Ev1Mv2(coord1.position(), coord0.position());
        dr.PEa1Tv1(falseTime,dv);
        nearestImageTransformer.nearestImage(dr);

		double r2 = dr.squared();
		double bij = dr.dot(dv);
		double nudge = 0;
		double eps = 1.0e-10;
		
		// ke is kinetic energy due to components of velocity
		
		double reduced_m = 2.0 / (((AtomTypeLeaf)atom0.getType()).rm() + ((AtomTypeLeaf)atom1.getType()).rm());
		double ke = bij * bij * reduced_m / (4.0 * r2);
		
		
		if (areBonded(atom0,atom1)) {		//atoms are bonded to each
			if (2 * r2 < (coreDiameterSquared + wellDiameterSquared)) { // Hard-core collision															
				lastCollisionVirial = reduced_m * bij;
	
			} else { 				// Well collision assume separating because mutually bonded
				if (ke < epsilon) 		// Not enough kinetic energy to escape
				{ 
					lastCollisionVirial = reduced_m * bij;
					nudge = -eps;
				} 
				else{ 	
				    lastCollisionVirial = 0.5 * reduced_m * bij- Math.sqrt(reduced_m * r2 * (ke - epsilon));
					unbond(atom1,atom0);
					nudge = eps;
				}

			}
        }
		else { 	//not bonded to each other
			//well collision; decide whether to bond or have hard repulsion
			if (full(atom0) || full(atom1)) { 
				lastCollisionVirial = reduced_m * bij;
				nudge = eps;
			} else { //neither is taken; bond to each other
                lastCollisionVirial = 0.5* reduced_m* (bij + Math.sqrt(bij * bij + 4.0 * r2 * epsilon/ reduced_m));
				bond(atom0,atom1);
				nudge = -eps;
			}
		} 

		lastCollisionVirialr2 = lastCollisionVirial / r2;
		dv.Ea1Tv1(lastCollisionVirialr2, dr);
		coord0.velocity().PEa1Tv1(((AtomTypeLeaf)atom0.getType()).rm(), dv);
		coord1.velocity().PEa1Tv1(-((AtomTypeLeaf)atom1.getType()).rm(), dv);
		coord0.position().PEa1Tv1(-falseTime * ((AtomTypeLeaf)atom0.getType()).rm(), dv);
		coord1.position().PEa1Tv1(falseTime * ((AtomTypeLeaf)atom1.getType()).rm(), dv);
		
		if (nudge != 0) 
		{
			coord0.position().PEa1Tv1(-nudge, dr);
			coord1.position().PEa1Tv1(nudge, dr);
		}
	}//end of bump
}//end of class

