package simulate;
import java.awt.Container;
import java.awt.Graphics;

public abstract class Phase extends Container {
    
    public Phase() {
        setLayout(null);
        setSize(300,300);
        atomCount = moleculeCount = 0;
        gravity = new Gravity(0.0);
        noGravity = true;
        add(new ConfigurationSequential());  //default configuration
        potentialEnergy = new MeterPotentialEnergy();
        kineticEnergy = new MeterKineticEnergy();
        add(potentialEnergy);
        add(kineticEnergy);
    }

    public abstract simulate.AtomPair makeAtomPair(Atom a1, Atom a2);
    public abstract AtomPair.Iterator.A makePairIteratorFull(Atom iF, Atom iL, Atom oF, Atom oL);
    public abstract AtomPair.Iterator.A makePairIteratorHalf(Atom iL, Atom oF, Atom oL);
    public abstract AtomPair.Iterator.A makePairIteratorFull();
    public abstract AtomPair.Iterator.A makePairIteratorHalf();
    
    public abstract double volume();
    public abstract Space.Vector dimensions();
    
    public abstract Space.Boundary boundary();

  public final Atom firstAtom() {
     Molecule m = firstMolecule();
     return (m != null) ? m.firstAtom() : null;
  }
  public final Atom lastAtom() {
    Molecule m = lastMolecule();
    return (m != null) ? m.lastAtom() : null;
  }
  public final Molecule firstMolecule() {
    for(Species.Agent s=firstSpecies; s!=null; s=s.nextSpecies()) {
        Molecule m = s.firstMolecule();
        if(m != null) {return m;}
    }
    return null;
  }
  public final Molecule lastMolecule() {
    for(Species.Agent s=lastSpecies; s!=null; s=s.previousSpecies()) {
        Molecule m = s.lastMolecule();
        if(m != null) {return m;}
    }
    return null;
  }
  public final Species.Agent firstSpecies() {return firstSpecies;}
  public final Species.Agent lastSpecies() {return lastSpecies;}
  public final Phase nextPhase() {return nextPhase;}
  public final Phase previousPhase() {return previousPhase;}
 /**
  * Sets the phase following this one in the linked list of phases.
  * Does not link species/molecule/atoms of the Phases
  *
  * @param p the phase to be designated as this phase nextPhase
  */
  public final void setNextPhase(Phase p) {
    this.nextPhase = p;
    p.previousPhase = this;
  }
  
  public final double getG() {return gravity.getG();}
  public void setG(double g) {
    gravity.setG(g);
    noGravity = (g == 0.0);
  }
  
 /**
  * Returns the temperature (in Kelvin) of this phase as computed via the equipartition
  * theorem from the kinetic energy summed over all (atomic) degrees of freedom
  */  
  public double kineticTemperature() {
    return (2./(double)(atomCount*parentSimulation.space.D()))*kineticEnergy.currentValue()*Constants.KE2T;
  }

    public void add(Configuration c){
        c.parentPhase = this;
        configuration = c;
        for(Species.Agent s=firstSpecies; s!=null; s=s.nextSpecies()) {
            configuration.add(s);
        }
    }
    
	public void add(Meter m) {
	    if(lastMeter != null) {lastMeter.setNextMeter(m);}
	    else {firstMeter = m;}
	    lastMeter = m;
	    meterCount++;
	    m.phase = this;
	    m.initialize();
	    if(parentSimulation != null && parentSimulation.haveIntegrator()) {
	        parentSimulation.controller.integrator.addIntegrationIntervalListener(m);
	    }
	}
	
    public void add(Species.Agent species) {
//        species.configurationMolecule.initializeCoordinates();
        configuration.add(species);
        if(lastSpecies != null) {lastSpecies.setNextSpecies(species);}
        else {firstSpecies = species;}
        lastSpecies = species;
        for(Molecule m=species.firstMolecule(); m!=null; m=m.nextMolecule()) {moleculeCount++;}
        for(Atom a=species.firstAtom(); a!=null; a=a.nextAtom()) {atomCount++;}
    }
    
    public void add(Species s) {  //add species to phase if it doesn't appear in another phase
        s.parentSimulation = this.parentSimulation;
        Species.Agent agent = s.makeAgent(this);
        agent.setNMolecules(20);
        add(agent);
    }
                
	// Returns ith meter in linked list of meters, with i=0 being the first meter
	public Meter getMeter(int i) {
	    if(i >= meterCount) {return null;}
	    Meter m = firstMeter;
        for(int j=i; --j>=0; ) {m = m.nextMeter();}  //get ith meter in list
        return m;
    }

    public abstract void inflate(double scale);

    
    public abstract void paint(Graphics g, int[] origin, double scale); 
    
  
 /**
  * Object used to describe presence and magnitude of constant gravitational acceleration
  */
  public Gravity gravity;
  public boolean noGravity = true;
    
 /**
  * First species in the linked list of species in this phase.
  */
   private Species.Agent firstSpecies;
 
 /**
  * Last species in the linked list of species in this phase.
  */
  Species.Agent lastSpecies;
  
 /**
  * Total number of atoms in this phase
  */
  public int atomCount;
 
 /**
  * Total number of molecules in this phase
  *
  * @see Species#addMolecule
  * @see Species#deleteMolecule
  */
  public int moleculeCount;
     
  Simulation parentSimulation;
  Meter firstMeter, lastMeter;
  private int meterCount = 0;
  
  public Configuration configuration;
  
  private Phase nextPhase;
  private Phase previousPhase;
  
  public Integrator integrator;
  
  public MeterPotentialEnergy potentialEnergy;
  public MeterKineticEnergy kineticEnergy;
    
}
    