package etomica;

/**
 * Constant NVT Molecular Dynamics Integrator-Constraint Method
 * 
 * Uses a modified version of the Leap Frog Algorithm. 
 * ke is calculated from the unconstrained velocities at time T.
 * A ratio of the setTemperature to the unconstrained temp (as solved from ke),
 * is used to calculate the new constrained velocities at T+Dt/2.  
 * The positions at T+Dt are solved for from the constrained velocities calculated at T+Dt/2.
 *  
 * @author Chris Iacovella
 * @author David Kofke
 */
public final class IntegratorConNVT extends IntegratorMD implements EtomicaElement {

    private final AtomIteratorList atomIterator = new AtomIteratorList();
    
    public final PotentialCalculationForceSum forceSum;
    private final IteratorDirective allAtoms = new IteratorDirective();
    
    Space.Vector work, work1, work2, work3, work4;
    double halfTime, mass;
                
    public IntegratorConNVT() {
        this(Simulation.instance);
    }
    public IntegratorConNVT(final Simulation sim) {
        super(sim);
        forceSum = new PotentialCalculationForceSum(sim.space());
       
        work = sim.space().makeVector();
        work1 = sim.space().makeVector();
        work2 = sim.space().makeVector();
        work3 = sim.space().makeVector();
       	work4 = sim.space().makeVector();
       	
        setTimeStep(etomica.units.systems.LJ.SYSTEM.time().toSim(2.0));
    }

	
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("NVT-MD Integrator using modified Leap-Frog algorithm");
        return info;
    }

	/**
	 * Overrides superclass method to instantiate iterators when iteratorFactory in phase is changed.
	 * Called by Integrator.addPhase and Integrator.iteratorFactorObserver.
	 */
	private Phase phase;
	
	public boolean addPhase(Phase p) {
	    if(!super.addPhase(p)) return false;
        atomIterator.setList(p.speciesMaster.atomList);
        phase = p;
        return true;
    }
    
        
  	public final void setTimeStep(double t) {
    	super.setTimeStep(t);
    	halfTime = timeStep/2.0;
  	}
  	
  	private double Temper;
	public void setTemp(double temperature) {
		   Temper=temperature;
	   }
          
//--------------------------------------------------------------
// steps all particles across time interval tStep

    public void doStep() {
		double dim = space.D();  //get the dimension
		
        //Compute forces on each atom
        atomIterator.reset();
        while(atomIterator.hasNext()) {   //zero forces on all atoms
            ((Agent)atomIterator.nextAtom().ia).force.E(0.0);
        }
        potential.calculate(firstPhase, allAtoms, forceSum);
	
		//MoveA
		//Advance velocities from T-Dt/2 to T without constraint
		atomIterator.reset();	
		double Free=0.0;
		//degrees of freedom
		Free=(double)((phase.moleculeCount()-1)*dim); 
		
		double k=0.0;
        double chi;
		while(atomIterator.hasNext()) {
			Atom a = atomIterator.nextAtom();
			Agent agent = (Agent)a.ia;
			//Space.Vector r = a.coord.position();
			Space.Vector p = a.coord.momentum();
			mass = a.coord.mass();
            
			work1.E(p); //work1 = p
			work1.TE(a.coord.rm());  //work1 = p/m = v
			work2.E(agent.force);	//work2=F
			work1.PEa1Tv1(halfTime*a.coord.rm(),work2); //work1= p/m + F*Dt2/m = v + F*Dt2/m
            
        	 k+=work1.squared();
 
		}   
    	//calculate scaling factor chi
		
		chi= Math.sqrt(Temper*Free/(mass*k));
		
		//calculate constrained velocities at T+Dt/2
		atomIterator.reset();
		while(atomIterator.hasNext()) {
			Atom a = atomIterator.nextAtom();
			Agent agent = (Agent)a.ia;
			Space.Vector p = a.coord.momentum();
			double divmass = a.coord.rm();
		
			double scale = (2.0*chi-1.0)*divmass; 
			work3.Ea1Tv1(scale,p); 
			work4.Ea1Tv1(chi*divmass,agent.force);
			work4.TE(timeStep);
			work3.PE(work4);
			work3.TE(a.coord.mass());
			p.E(work3);
				
		} 

		atomIterator.reset();
		while(atomIterator.hasNext()) {
			Atom a = atomIterator.nextAtom();
			Agent agent = (Agent)a.ia;
			Space.Vector r = a.coord.position();
			Space.Vector p = a.coord.momentum();
            
			work.E(p);
			work.TE(timeStep*a.coord.rm());
			work.PE(r);
			r.E(work);
		}

        
    }//end of doStep
    

    protected void doReset() {
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            Atom a = atomIterator.nextAtom();
            Agent agent = (Agent)a.ia;
          
        }
    }
              
//--------------------------------------------------------------

    public final Object makeAgent(Atom a) {
        return new Agent(simulation(),a);
    }
            
	public final static class Agent implements Integrator.Forcible {  //need public so to use with instanceof
        public Atom atom;
        public Space.Vector force;
    

        public Agent(Simulation sim, Atom a) {
            atom = a;
            force = sim.space().makeVector();
        }
        
        public Space.Vector force() {return force;}
    }//end of Agent
    
/*    public static void main(String[] args) {
        
	    IntegratorConNVT integrator = new IntegratorConNVT();
	    SpeciesSpheres species = new SpeciesSpheres();
	    Phase phase = new Phase();
	    P2LennardJones potential = new P2LennardJones();
	    Controller controller = new Controller();
	    DisplayPhase display = new DisplayPhase();
	    IntegratorMD.Timer timer = integrator.new Timer(integrator.chronoMeter());
	    timer.setUpdateInterval(10);
		integrator.setTemp(Default.TEMPERATURE);
		
		
		integrator.setSleepPeriod(2);
		
		Simulation.instance.elementCoordinator.go();
        Simulation.makeAndDisplayFrame(Simulation.instance);

    }//end of main 
    */
}//end of IntegratorConNVT

