package etomica.integrator;

import etomica.atom.Atom;
import etomica.exception.ConfigurationOverlapException;
import etomica.phase.Phase;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;

/**
 * Integrator that generates atom trajectories from an analytic formula.
 * Takes an IntegratorAnalytic.AtomAction instance and performs action on all atoms in each
 * time step; intention is for the action to set the position of the atom for the
 * current time (but it could do anything).
 *
 * @author David Kofke
 * @author Nancy Cribbin
 */
 
 /* History of changes
  * 7/31/02 (DAK) new
  */
 
public class IntegratorAnalytic extends IntegratorMD {
    
    private AtomAction action;
    
    public IntegratorAnalytic(Simulation sim) {
        this(sim.potentialMaster,sim.getDefaults().timeStep);
    }
    
    public IntegratorAnalytic(PotentialMaster potentialMaster, double timeStep) {
        super(potentialMaster,timeStep,0);
    }
    
    public void doStep() {
        if(action == null) return;
        elapsedTime += getTimeStep();
        action.setTime(elapsedTime);
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            Atom atom = atomIterator.nextAtom();
            action.actionPerformed(atom);
        }
    }
    
    public void reset() throws ConfigurationOverlapException {
        elapsedTime = 0.0;
        super.reset();
    }
    
    public void setAction(AtomAction action) {this.action = action;}
    
    public AtomAction getAction() {return action;}
    
	/**
	 * Overrides superclass method to instantiate iterators when iteratorFactory in phase is changed.
	 * Called by Integrator.addPhase and Integrator.iteratorFactorObserver.
	 */
	public boolean addPhase(Phase p) {
	    if(!super.addPhase(p)) return false;
        atomIterator.setPhase(p);
        return true;
    }
    
    private double elapsedTime = 0.0;
    
    /**
     * Extends AtomAction class to add a method to set the time.
     */
    public static abstract class AtomAction extends etomica.action.AtomActionAdapter {
        protected double time;
        public void setTime(double t) {time = t;}
    }
    
 }//end of IntegratorAnalytic
 
