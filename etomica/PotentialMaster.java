package etomica;

/**
 * Master potential that oversees all other potentials in the Hamiltonian.
 * Most calls to compute the energy or other potential calculations begin
 * with the calculate method of this class.  It then passes the calculation 
 * on to the contained potentials.
 *
 * @author David Kofke
 */
 
 /* History of changes
  * 08/13/02 (DAK) added removePotential method
  * 01/27/03 (DAK) many revisions as part of redesign of Potential
  * 06/15/03 (DAK) in makeBasis, for 2-species case, added check for whether the
  * two species are the same; if so uses one of them as basis, rather than
  * making a pair from them.
  * 08/31/04 (group) complete overhaul with revamping of potentials, etc.
  */
public class PotentialMaster extends PotentialGroup {
    
    public PotentialMaster(Space space) {
        super(-1, space);
    } 
    
	/**
	 * Returns the potential group that oversees the long-range
	 * correction zero-body potentials.
	 */
	 public PotentialGroupLrc lrcMaster() {
		if(lrcMaster == null) lrcMaster = new PotentialGroupLrc(this);
		return lrcMaster;
	 }

     /**
      * Inherited from PotentialGroup, but not used by PotentialMaster class and
      * throws RuntimeException if called.
      */
	 public void calculate(AtomsetIterator iterator, IteratorDirective id, PotentialCalculation pc) {
	 	throw new RuntimeException("Method inappropriate for PotentialMaster class");
	 }
	 
	//should build on this to do more filtering of potentials based on directive
     /**
      * Performs the given PotentialCalculation on the atoms of the given Phase.
      * Sets the phase for all molecule iterators and potentials, sets target
      * and direction for iterators as specified by given IteratorDirective,
      * and applies doCalculation of given PotentialCalculation with the iterators
      * and potentials.
      */
    public void calculate(Phase phase, IteratorDirective id, PotentialCalculation pc) {
    	if(!enabled) return;
    	Atom[] targetAtoms = id.getTargetAtoms();
    	boolean phaseChanged = (phase != mostRecentPhase);
    	mostRecentPhase = phase;
    	for(PotentialLinker link=first; link!=null; link=link.next) {
			if(phaseChanged) {
				((AtomsetIteratorPhaseDependent)link.iterator).setPhase(phase);
				link.potential.setPhase(phase);
			}
			((AtomsetIteratorTargetable)link.iterator).setTarget(targetAtoms);
			((AtomsetIteratorDirectable)link.iterator).setDirection(id.direction());
        	pc.doCalculation(link.iterator, id, link.potential);
        }//end for
    }//end calculate
    
    /**
     * Indicates to the PotentialMaster that the given potential should apply to 
     * the specified species.  Exception is thrown if the potential.nBody() value
     * is different from the length of the species array.  Thus, for example, if
     * giving a 2-body potential, then the array should contain exactly
     * two species; the species may refer to the same instance (appropriate for an 
     * intra-species potential, defining the iteractions between molecules of the
     * same species).
     */
    public void setSpecies(Potential potential, Species[] species) {
    	if (potential.nBody() == 0) {
    		addPotential(potential, new AtomsetIteratorSpeciesAgent(species));
    		return;
    	}
    	if (species.length == 0 || potential.nBody() != species.length) {
    		throw new IllegalArgumentException("Illegal species length");
    	}
    	AtomsetIteratorMolecule iterator = new AtomsetIteratorMolecule(species);
    	addPotential(potential, iterator);
    }
 
    /**
     * Performs no action.
     */
    public void setSimulation(Simulation sim) {
    }
    
    public AtomSequencer.Factory sequencerFactory() {return AtomSequencerSimple.FACTORY;}

	protected PotentialGroupLrc lrcMaster;
	protected Phase mostRecentPhase = null;

	
}//end of PotentialMaster
    