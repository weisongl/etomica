package etomica.chem.models;
import etomica.api.IPotentialAtomic;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.atom.iterator.AtomsetIteratorBasisDependent;

/**
 * Top-level class for a molecular model.
 * @author Andrew Schultz
 */
public abstract class Model implements java.io.Serializable {
	
    public Model(boolean isDynamic) {
        this.isDynamic = isDynamic;
    }
    
    /**
     * Returns the species associated with this Model, if it has already been
     * created.  If the species has not been made yet, getSpecies returns null.
	 */
    public ISpecies getSpecies() {
        return species;
    }
    
    /**
     * Creates a species in the given simulation and returns it.
     */
	public final ISpecies makeSpecies(ISimulation sim) {
        if (species == null) {
            species = makeSpeciesInternal(sim);
            sim.getSpeciesManager().addSpecies(species);
            initPotentials(sim);
        }
        return species;
    }

    /**
     * Internal method to be implemented by subclasses to create the actual
     * Species object for the given Simulation.
     */
    protected abstract ISpecies makeSpeciesInternal(ISimulation sim);
	
    /**
     * Internal method to be implemented by subclasses to initialize the
     * intramolecular potentials associated with this model.  Potential
     * objects might be created earlier, but this method gives the subclass
     * an opportunity to create the potentials after the species.  This method
     * will only be called after the species has been created.
     */
    protected abstract void initPotentials(ISimulation sim);
    
    /**
     * Returns an array of objects wrapping bonding Potentials and the 
     * AtomsetIteratorBasisDependents that return atoms appropriate for those
     * potentials.
     */
	public abstract PotentialAndIterator[] getPotentials();
    
    private static final long serialVersionUID = 1L;
    private ISpecies species;
    protected final boolean isDynamic;
    protected PotentialAndIterator[] potentialsAndIterators;

    /**
     * Wrapper class for a Potential and an AtomsetIteratorBasisDependent that
     * is associated with the potential.
     */
    public static class PotentialAndIterator {
        protected PotentialAndIterator(IPotentialAtomic potential, AtomsetIteratorBasisDependent iterator) {
            this.potential = potential;
            this.iterator = iterator;
        }
        
        public IPotentialAtomic getPotential() {
            return potential;
        }
        
        public AtomsetIteratorBasisDependent getIterator() {
            return iterator;
        }
        
        private final IPotentialAtomic potential;
        private final AtomsetIteratorBasisDependent iterator;
    }
}
