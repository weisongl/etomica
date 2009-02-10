package etomica.virial;

import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;
import etomica.api.IPotentialMolecular;

/**
 * @author kofke
 *
 * General Mayer function, which wraps the Mayer potential around an instance of
 * a Potential2 object.
 */
public class MayerEGeneral implements MayerFunction, java.io.Serializable {

	/**
	 * Constructor Mayer function using given potential.
	 */
	public MayerEGeneral(IPotentialMolecular potential) {
		this.potential = potential;
	}

	public double f(IMoleculeList pair, double r2, double beta) {
		return Math.exp(-beta*potential.energy(pair));
	}

	private final IPotentialMolecular potential;

	/* (non-Javadoc)
	 * @see etomica.virial.MayerFunction#getPotential()
	 */
	public IPotential getPotential() {
		// TODO Auto-generated method stub
		return potential;
	}
	
	public void setBox(IBox newBox) {
	    potential.setBox(newBox);
	}
}
