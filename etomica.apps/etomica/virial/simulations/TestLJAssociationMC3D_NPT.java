package etomica.virial.simulations;
import etomica.action.BoxInflate;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.association.AssociationManager;
import etomica.association.BiasVolumeCube;
import etomica.association.BiasVolumeSphere;
import etomica.association.MCMoveBiasUB;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.AccumulatorAverage.StatType;
import etomica.data.meter.MeterDensity;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveAtom;
import etomica.integrator.mcmove.MCMoveDimer;
import etomica.integrator.mcmove.MCMoveDimerRotate;
import etomica.integrator.mcmove.MCMoveRotate;
import etomica.integrator.mcmove.MCMoveStepTracker;
import etomica.integrator.mcmove.MCMoveVolume;
import etomica.lattice.LatticeCubicFcc;
import etomica.listener.IntegratorListenerAction;
import etomica.nbr.cell.PotentialMasterCell;
import etomica.potential.P2HardAssociationCone;
import etomica.simulation.Simulation;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresRotating;
import etomica.util.HistoryCollapsingAverage;
import etomica.util.ParameterBase;
import etomica.util.RandomNumberGenerator;

/**
 * Simple Lennard-Jones + S-W Association Monte Carlo simulation in 3D.
 * average density = N*<1/V>
 * Initial configurations at http://rheneas.eng.buffalo.edu/etomica/tests/
 */
public class TestLJAssociationMC3D_NPT extends Simulation {
    
    private static final long serialVersionUID = 1L;
    public IntegratorMC integrator;
    public MCMoveAtom mcMoveAtom;
    public MCMoveRotate mcMoveRotate;
    public SpeciesSpheresRotating species;
    public IBox box;
    public P2HardAssociationCone potential;
    double epsilon = 1.0;
    public MCMoveDimer mcMoveDimer;
    public MCMoveDimerRotate mcMoveDimerRotate;
    public MCMoveVolume mcMoveVolume;
    public ActivityIntegrate actionIntegrator;
    public MCMoveBiasUB mcMoveBiasUB;
        
    
    public TestLJAssociationMC3D_NPT(int numAtoms, double pressure, double density, double wellConstant, double temperature, long numSteps) {
        super(Space3D.getInstance(), false);
        PotentialMasterCell potentialMaster = new PotentialMasterCell(this, space);
              
        double sigma =1.0;
        //setRandom(new RandomNumberGenerator(3));
                   
        System.out.println("pressure = " +pressure);
        System.out.println("initial density = " +density);
        System.out.println("association strength = " +wellConstant+ "*epsilon");
        System.out.println("temperature = " +temperature);
        System.out.println("numSteps = " +numSteps);
	    integrator = new IntegratorMC(this, potentialMaster);
	    integrator.setTemperature(temperature);
	    mcMoveAtom = new MCMoveAtom(this, potentialMaster, space);//Standard Monte Carlo atom-displacement trial move
	    mcMoveRotate = new MCMoveRotate(potentialMaster, random, space);//Performs a rotation of an atom (not a molecule) that has an orientation coordinate
	    BiasVolumeSphere bv = new BiasVolumeSphere(space, random);
	    bv.setBiasSphereInnerRadius(0.86);
	    box = new Box(space);
        addBox(box);
        bv.setBox(box);
	    mcMoveBiasUB = new MCMoveBiasUB(potentialMaster, bv, random, space);
	    AssociationManager associationManager =new AssociationManager(box, potentialMaster, bv);
	    mcMoveBiasUB.setAssociationManager(associationManager);
	    
        //mcMoveAtom.setStepSize(0.2*sigma);
        ((MCMoveStepTracker)mcMoveAtom.getTracker()).setNoisyAdjustment(true);
        ((MCMoveStepTracker)mcMoveRotate.getTracker()).setNoisyAdjustment(true);
        integrator.getMoveManager().addMCMove(mcMoveAtom);
        integrator.getMoveManager().addMCMove(mcMoveRotate);
        integrator.getMoveManager().addMCMove(mcMoveBiasUB);
        integrator.getMoveEventManager().addListener(associationManager);
        integrator.getMoveManager().setEquilibrating(true);
        actionIntegrator = new ActivityIntegrate(integrator);
        //actionIntegrate.setSleepPeriod(1);
        actionIntegrator.setMaxSteps(numSteps);
        getController().addAction(actionIntegrator);
        species = new SpeciesSpheresRotating(this, space);
        getSpeciesManager().addSpecies(species);
        box.setNMolecules(species, numAtoms);
        BoxInflate inflater = new BoxInflate(box, space);//Performs actions that cause volume of system to expand or contract
        inflater.setTargetDensity(density);
        inflater.actionPerformed();
        
        double truncationRadius = 6.0*sigma;//truncation distance of potential default = 3.0*sigma
        System.out.println("truncation distance of potential = " +truncationRadius);
        if(truncationRadius > 0.5*box.getBoundary().getDimensions().x(0)) {
            throw new RuntimeException("Truncation radius too large.  Max allowed is"+0.5*box.getBoundary().getDimensions().x(0));
        }
        //P2SoftSphericalTruncated potentialTruncated = new P2SoftSphericalTruncated(space, potential, truncationRadius);
        potential = new P2HardAssociationCone(space, sigma, epsilon, truncationRadius, wellConstant);
        potentialMaster.setCellRange(3);
        potentialMaster.setRange(potential.getRange());
        mcMoveDimer = new MCMoveDimer(this, potentialMaster, space, potential);
        mcMoveDimerRotate = new MCMoveDimerRotate(this, potentialMaster,space, potential);
        mcMoveVolume = new MCMoveVolume(this, potentialMaster, space);
        mcMoveVolume.setPressure(pressure);
        
        IAtomType leafType = species.getLeafType();
        potentialMaster.addPotential(potential, new IAtomType[] {leafType, leafType});
        integrator.getMoveEventManager().addListener(potentialMaster.getNbrCellManager(box).makeMCMoveListener());
        integrator.getMoveManager().addMCMove(mcMoveDimer);
        integrator.getMoveManager().addMCMove(mcMoveDimerRotate);
        integrator.getMoveManager().addMCMove(mcMoveVolume);
        
        ConfigurationLattice config = new ConfigurationLattice(new LatticeCubicFcc(space), space);
        config.initializeCoordinates(box);
        integrator.setBox(box);
        potentialMaster.getNbrCellManager(box).assignCellAll();
//        WriteConfiguration writeConfig = new WriteConfiguration("LJMC3D"+Integer.toString(numAtoms),box,1);
//        integrator.addListener(writeConfig);
    }
 
    public static void main(String[] args) {
    	VirialAssociatingFluidParam params = new VirialAssociatingFluidParam();
    	
    	int numAtoms = params.numAtoms;
    	double pressure = params.pressure;
    	double density = params.density;
    	double wellConstant = params.wellConstant;
        double temperature = params.temperature;
        long numSteps = params.numSteps;
        if (args.length > 0) {
            numAtoms = Integer.parseInt(args[0]);;
            pressure = Double.parseDouble(args[1]);
            density = Double.parseDouble(args[2]);
            wellConstant = Double.parseDouble(args[3]);
            temperature = Double.parseDouble(args[4]);
            numSteps = Long.parseLong(args[5]);
            
        }
        TestLJAssociationMC3D_NPT sim = new TestLJAssociationMC3D_NPT(numAtoms, pressure, density, wellConstant, temperature, numSteps);

        sim.actionIntegrator.setMaxSteps(numSteps/10);//equilibrium period
        System.out.println("equilibrium period = " +numSteps/10);
        sim.getController().actionPerformed();
        sim.getController().reset();
        
        sim.actionIntegrator.setMaxSteps(numSteps);
        MeterDensity rhoMeter = new MeterDensity(sim.space);//Meter for measurement of the total molecule number density((number of molecules)/(volume of box)) in a box 
        rhoMeter.setBox(sim.box);
        AccumulatorAverage rhoAccumulator = new AccumulatorAverageFixed(10);//Accumulator that keeps statistics for averaging and error analysis
        DataPump rhoPump = new DataPump(rhoMeter,rhoAccumulator);
        IntegratorListenerAction listener = new IntegratorListenerAction(rhoPump);
        listener.setInterval(2*numAtoms);
        sim.integrator.getEventManager().addListener(listener);
        MeterPotentialEnergyFromIntegrator energyMeter = new MeterPotentialEnergyFromIntegrator(sim.integrator);
        AccumulatorAverage energyAccumulator = new AccumulatorAverageFixed(10);
        DataPump energyManager = new DataPump(energyMeter, energyAccumulator);
        energyAccumulator.setBlockSize(50);
        IntegratorListenerAction energyListener = new IntegratorListenerAction(energyManager);
        sim.integrator.getEventManager().addListener(energyListener);
        
        if (false) {
        	SimulationGraphic graphic = new SimulationGraphic(sim,SimulationGraphic.TABBED_PANE, sim.space,sim.getController());
        	AccumulatorHistory densityHistory = new AccumulatorHistory(new HistoryCollapsingAverage()); 
        	rhoAccumulator.addDataSink(densityHistory, new StatType[]{StatType.MOST_RECENT});
        	DisplayPlot rhoPlot = new DisplayPlot();
        	densityHistory.setDataSink(rhoPlot.getDataSet().makeDataSink());
        	graphic.add(rhoPlot);
        	graphic.makeAndDisplayFrame();
        	sim.actionIntegrator.setMaxSteps(2000000);
        	return;
        }
        
        sim.getController().actionPerformed();
        
        System.out.println("numAtom=" +numAtoms);
        double avgDensity = ((DataDouble)((DataGroup)rhoAccumulator.getData()).getData(StatType.AVERAGE.index)).x;//average density
        System.out.println("average density=" +avgDensity);
        double Z = pressure/(avgDensity*sim.integrator.getTemperature());
        double avgPE = ((DataDouble)((DataGroup)energyAccumulator.getData()).getData(StatType.AVERAGE.index)).x;
        avgPE /= numAtoms;
        System.out.println("Z="+Z);
        System.out.println("PE/epsilon="+avgPE);
        double temp = sim.integrator.getTemperature();
        double Cv = ((DataDouble)((DataGroup)energyAccumulator.getData()).getData(StatType.STANDARD_DEVIATION.index)).x;
        Cv /= temp;
        Cv *= Cv/numAtoms;
        System.out.println("Cv/k="+Cv);
        
        if (Double.isNaN(Z) || Math.abs(Z+0.25) > 0.15) {
            System.exit(1);
        }
        if (Double.isNaN(avgPE) || Math.abs(avgPE+4.56) > 0.03) {
            System.exit(1);
        }
        if (Double.isNaN(Cv) || Math.abs(Cv-0.61) > 0.45) {  // actual average seems to be 0.51
            System.exit(1);
        }
          
    }
    public static class VirialAssociatingFluidParam extends ParameterBase {
		public int numAtoms = 512;
		public double pressure = 0.2275;
		public double density = 0.1;
		public double wellConstant = 16.0;
		public double temperature = 2.0;	
		public long numSteps = 2000000;
	}

}