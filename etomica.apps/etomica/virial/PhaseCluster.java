package etomica.virial;

import etomica.*;

/**
 * @author kofke
 *
 * Extension of Phase that forms and holds a PairSet instance for all of the
 * atoms in the phase.  Also instantiates phase with a NONE boundary type.
 */
public class PhaseCluster extends Phase {

	/**
	 * Constructor for PhaseCluster.
	 */
	public PhaseCluster() {
		this(Simulation.instance);
	}

	/**
	 * Constructor for PhaseCluster.
	 * @param parent
	 */
	public PhaseCluster(SimulationElement parent) {
		super(parent);
		setBoundary(space.makeBoundary(etomica.Space3D.Boundary.NONE));	
	}
	
	public PairSet pairSet() {
		if(pairSet == null && speciesMaster.atomList.size() > 0) {
			pairSet = new PairSet(speciesMaster.atomList);
		}
		return pairSet;
	}

	private PairSet pairSet;
}
