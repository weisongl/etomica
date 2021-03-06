package etomica.dielectric;

import java.awt.Color;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import etomica.action.BoxImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.DiameterHashByType;
import etomica.atom.DipoleSource;
import etomica.atom.IAtomOriented;
import etomica.atom.IAtomPositionDefinition;
import etomica.atom.IAtomTypeOriented;
import etomica.box.Box;
import etomica.chem.elements.ElementSimple;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageCovariance;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.DataPump;
import etomica.data.IData;
import etomica.data.meter.MeterDipoleSumSquared1site;
import etomica.data.meter.MeterDipoleSumSquaredMappedAverage;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayBoxCanvasG3DSys;
import etomica.graphics.DisplayBoxCanvasG3DSys.OrientedSite;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.integrator.mcmove.MCMoveRotate;
import etomica.lattice.LatticeCubicFcc;
import etomica.listener.IntegratorListenerAction;
import etomica.potential.P2HSDipole;
import etomica.potential.P2ReactionFieldDipole;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.ISpace;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresRotating;
import etomica.units.Pixel;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.util.RandomNumberGenerator;

/**
 * Canonical ensemble Monte Carlo simulation (NVT)
 * calculate dielectric constant for dipolar HS model
 * 
 * Electric field mapped average for dielectric constant
 * 
 * @author Shu & Weisong
 * May 2015
 */

public class DHS_NVT extends Simulation {
//	private static final long serialVersionUID = 1L;
	protected final SpeciesSpheresRotating species; 
	protected final IPotentialMaster potentialMaster;
	protected final IntegratorMC integrator;
	protected final MCMoveMolecule moveMolecule;//translation mc move
	protected final MCMoveRotate rotateMolecule;//atomic rotation mc move
	protected final IBox box;
	private final static String APP_NAME = "dipolar HS, dielectric constant";
	private static final int PIXEL_SIZE = 15;
	public final ActivityIntegrate activityIntegrate;
	public Controller controller; 

	public static class DipoleSourceDHS implements DipoleSource{//for potential reaction field
		protected final IVectorMutable dipoleVector;
		protected double dipoleStrength;
		public DipoleSourceDHS(ISpace space,double dipole){
			dipoleStrength=dipole;
			dipoleVector=space.makeVector();
		}
		public IVector getDipole(IMolecule molecule) {
			if(molecule.getChildList().getAtomCount()!=1){
				throw new RuntimeException("improper number of atom in the molecule");
			}															
			IAtomList atomList = molecule.getChildList();
			IAtomOriented atom = (IAtomOriented)atomList.getAtom(0);
			dipoleVector.E(atom.getOrientation().getDirection());
			dipoleVector.TE(dipoleStrength);
			return dipoleVector; 
		}
	}

	//************************************* constructor ********************************************//
	public DHS_NVT(Space space, int numberMolecules,final double HSDiameter, double mu, 
			double dielectricOutside, double boxSize, double temperature,double truncation){
		super(space);		
//		setRandom(new RandomNumberGenerator(1)); //debug only  TODO remember its still setrandom be to 1
		
		
		species = new SpeciesSpheresRotating(space, new ElementSimple("A"));         
		addSpecies(species);
		box = new Box(space);
		addBox(box);
		box.setNMolecules(species, numberMolecules);
		box.getBoundary().setBoxSize(space.makeVector(new double[]{boxSize,boxSize,boxSize}));

		IAtomPositionDefinition positionDefinition = new IAtomPositionDefinition() {
			public IVector position(IMolecule molecule) {
				return molecule.getChildList().getAtom(0).getPosition();
			}
		};

		// potential part
//		boolean 
//		if(){
//			
//		}
		P2HSDipole pTarget = new P2HSDipole(space,HSDiameter, mu,truncation);			
		DipoleSourceDHS dipoleDHS = new DipoleSourceDHS(space,mu);// add reaction field potential
//		System.out.println("in main class, magnitude of dipole:"+dipoleDHS.dipoleStrength);
		P2ReactionFieldDipole pRF = new P2ReactionFieldDipole(space,positionDefinition);  
		pRF.setDipoleSource(dipoleDHS);
		pRF.setRange(truncation);
		pRF.setDielectric(dielectricOutside);

		potentialMaster = new PotentialMaster();     
		
		
		potentialMaster.addPotential(pRF, new ISpecies[]{species, species});
//		add reaction filed potential  to potential masterm TODO
		
		potentialMaster.lrcMaster().addPotential(pRF.makeP0());
		// add P0ReactionField potential to potential master
		// for u(HS)+u(dd)
		potentialMaster.addPotential(pTarget, new ISpecies[]{species,species});

		
		
		
		// integrator from potential master
		integrator = new IntegratorMC(this,potentialMaster);
		// add mc move
		moveMolecule = new MCMoveMolecule(this,potentialMaster, space);		// stepSize:1.0, stepSizeMax:15.0 
		rotateMolecule = new MCMoveRotate(potentialMaster,random,space);

		activityIntegrate = new ActivityIntegrate(integrator);
		getController().addAction(activityIntegrate);

		//******************************** periodic boundary condition ******************************** //
		BoxImposePbc imposePbc = new BoxImposePbc(box, space);				
		imposePbc.setApplyToMolecules(true);
		//**************************** integrator ****************************** //
		integrator.setTemperature(temperature);
		integrator.setBox(box);
		integrator.getMoveManager().addMCMove(moveMolecule);  //TODO
		integrator.getMoveManager().addMCMove(rotateMolecule);
		integrator.getEventManager().addListener(new IntegratorListenerAction(imposePbc));

		//******************************** initial configuration ******************************** //
		LatticeCubicFcc lattice = new LatticeCubicFcc(space); 					
		ConfigurationLattice configuration = new ConfigurationLattice(lattice, space);
		configuration.initializeCoordinates(box);
		
		//TODO
//		IVectorMutable p0 =  box.getLeafList().getAtom(0).getPosition();
//		IVectorMutable p1 = box.getLeafList().getAtom(1).getPosition();
//		p0.E(0);
//		p1.E(0.7);
//		IVectorMutable p2 = box.getLeafList().getAtom(2).getPosition();
//		p2.E(0);
//		p2.setX(0, 1.1);
		
	}

	// **************************** simulation part **************************** //
	public static void main (String[] args){
		Param params = new Param();
		if (args.length > 0) {
			ParseArgs.doParseArgs(params, args);
		} else {

		}															
		final long startTime = System.currentTimeMillis();
		DateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		System.out.println("startTime : " + date.format(cal.getTime()));
		
		Space space = Space3D.getInstance();
		int steps = params.steps;
		boolean isGraphic = params.isGraphic;
		double temperature = params.temperature;					
		int numberMolecules = params.numberMolecules;
		double density = params.density;
		double HSDiameter=params.HSDiameter;
		double dipoleStrength = Math.sqrt(params.dipoleStrength2);
		double dielectricOutside = params.dielectricOutside;
		//double densitySim = density * Constants.AVOGADRO * 1e-27;  // ?????? convert density to sim unit; in 1/(A)^3
		//System.out.println("Constants.AVOGADRO * 1e-27: "+Constants.AVOGADRO * 1e-27);
		//boolean flag = true;
		//if(flag){
		double densitySim = density;
		//}
		double boxSize = Math.pow(numberMolecules/densitySim,(1.0/3.0));	
		double truncation = boxSize * 0.49;											
//		System.out.println("******************* dipolar HS, dielectric constant, NVT********************");
		System.out.println("steps = "+steps);
		System.out.println("number of molecules= "+numberMolecules);
		System.out.println("temperature(sim) = "+temperature);
//		System.out.println("density(sim)1/angstrom^3 = "+densitySim);
		System.out.println("box size(angstrom): "+boxSize);
		System.out.println("truncation= "+truncation);
//		System.out.println("HSDiameter= "+HSDiameter);
		System.out.println("dipoleStrength = "+dipoleStrength);
//		System.out.println("dielectricOutside = "+dielectricOutside);

		final DHS_NVT sim = new DHS_NVT(space,numberMolecules,HSDiameter,dipoleStrength,dielectricOutside,boxSize,temperature,truncation);

		if (isGraphic){
			SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, space, sim.getController());
			simGraphic.getDisplayBox(sim.box).setPixelUnit(new Pixel(PIXEL_SIZE));
			simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));	        
			((DiameterHashByType)((DisplayBox)simGraphic.displayList().getFirst()).getDiameterHash()).setDiameter(sim.species.getAtomType(0),1);
			ColorSchemeByType colorScheme = (ColorSchemeByType)simGraphic.getDisplayBox(sim.box).getColorScheme();
			colorScheme.setColor(sim.getSpecies(0).getAtomType(0), Color.red);
			OrientedSite[] sites = new OrientedSite[1];
			sites[0] = new OrientedSite(0.5, Color.BLUE, 0.2);
			((DisplayBoxCanvasG3DSys)simGraphic.getDisplayBox(sim.box).canvas).setOrientationSites(
					(IAtomTypeOriented)sim.getSpecies(0).getAtomType(0), sites);
			simGraphic.makeAndDisplayFrame(APP_NAME);
			simGraphic.getDisplayBox(sim.box).repaint();
			return ;
		}
		sim.activityIntegrate.setMaxSteps(steps/5);// equilibration period
		sim.getController().actionPerformed();		
		sim.getController().reset();
		
		//TODO
		sim.integrator.getMoveManager().setEquilibrating(false);        //set the stepsize
//		System.out.println("equilibration finished");
		//TODO
		
		int blockNumber = 100;
		int sampleAtInterval = numberMolecules;
		int samplePerBlock = steps/sampleAtInterval/blockNumber;					
//		System.out.println("number of blocks is : " + blockNumber);
//		System.out.println("sample per block is : " + samplePerBlock);

		// dipoleSumSquared
		MeterDipoleSumSquared1site dipoleSumSquaredMeter = new MeterDipoleSumSquared1site(space,sim.box,dipoleStrength);  	
		AccumulatorAverage dipoleSumSquaredAccumulator = new AccumulatorAverageFixed(samplePerBlock);
		DataPump dipolePump = new DataPump(dipoleSumSquaredMeter,dipoleSumSquaredAccumulator);
		IntegratorListenerAction dipoleListener = new IntegratorListenerAction(dipolePump);
		dipoleListener.setInterval(sampleAtInterval);
		sim.integrator.getEventManager().addListener(dipoleListener);
		
		// energy
		MeterPotentialEnergyFromIntegrator energyMeter = new MeterPotentialEnergyFromIntegrator(sim.integrator);
		AccumulatorAverage energyAccumulator = new AccumulatorAverageFixed(10);
		DataPump energyPump = new DataPump(energyMeter, energyAccumulator);
		energyAccumulator.setBlockSize(50);											
		IntegratorListenerAction energyListener = new IntegratorListenerAction(energyPump);
		sim.integrator.getEventManager().addListener(energyListener);
		
		//AEE  
		DipoleSourceDHS dipoleDHS = new DipoleSourceDHS(space,dipoleStrength);// add reaction field potential
		MeterDipoleSumSquaredMappedAverage AEEMeter = new MeterDipoleSumSquaredMappedAverage(space, sim.box,sim, dipoleStrength,temperature,sim.potentialMaster);
		AEEMeter.setDipoleSource(dipoleDHS);
		AccumulatorAverageCovariance AEEAccumulator = new AccumulatorAverageCovariance(samplePerBlock,true);
		DataPump AEEPump = new DataPump(AEEMeter,AEEAccumulator);
		IntegratorListenerAction AEEListener = new IntegratorListenerAction(AEEPump);
		
		
		//TODO
		AEEListener.setInterval(sampleAtInterval);//TODO
//		AEEListener.setInterval(1);
		//TODO
		
		
		sim.integrator.getEventManager().addListener(AEEListener);//TODO
		sim.activityIntegrate.setMaxSteps(steps);	
		
		sim.getController().actionPerformed();

		//calculate dipoleSumSquared average
		double dipoleSumSquared = ((DataDouble)((DataGroup)dipoleSumSquaredAccumulator.getData()).getData(dipoleSumSquaredAccumulator.AVERAGE.index)).x;
		double dipoleSumSquaredERR = ((DataDouble)((DataGroup)dipoleSumSquaredAccumulator.getData()).getData(dipoleSumSquaredAccumulator.ERROR.index)).x;
		//TODO
		double sum0 =  ((DataGroup)AEEAccumulator.getData()).getData(AEEAccumulator.AVERAGE.index).getValue(0); 
		double ERsum0 = ((DataGroup)AEEAccumulator.getData()).getData(AEEAccumulator.ERROR.index).getValue(0);
		double sum1 = ((DataGroup)AEEAccumulator.getData()).getData(AEEAccumulator.AVERAGE.index).getValue(1); 
		double ERsum1= ((DataGroup)AEEAccumulator.getData()).getData(AEEAccumulator.ERROR.index).getValue(1);
		
		IData covariance = ((DataGroup)AEEAccumulator.getData()).getData(AEEAccumulator.BLOCK_COVARIANCE.index);
		covariance.getValue(1);
//		double AEE = sum0 + sum1*sum1;
//		double AEEER = Math.sqrt(ERsum0*ERsum0 + 4*sum1*sum1*ERsum1*ERsum1 - 
//				2*ERsum0*sum1*2*ERsum1*covariance.getValue(1)/Math.sqrt(covariance.getValue(0)*covariance.getValue(3)));
		
		//TODO
		double AEE = sum0;
		double AEEER = ERsum0;
		//TODO
		
		
		double volume = sim.box.getBoundary().volume();
		double dipoleFac = 4 * Math.PI * dipoleSumSquared/9.0/volume/temperature;
		double dielectricOutsideFac = 2*(dielectricOutside-1)/(2*dielectricOutside+1);
		double x1 =  dipoleSumSquared;
		double B  = dielectricOutsideFac;
		double D  = 4 * Math.PI/9.0/volume/temperature;
//		System.out.println("B:  "+B);
//		System.out.println("D:  "+D);
//		System.out.println("x1:  "+x1);

		double dEpsilondx_1=(B*D+2*D)/(B*D*x1-D*x1+1);
		double dEpsilondx_2=(B*D-D)*(B*D*x1+2D*x1+1)/Math.pow((B*D*x1-D*x1+1),2);
		double dEpsilondx =dEpsilondx_1-dEpsilondx_2;
		double epsilon = (1+B*D*x1+2*D*x1)/(1+B*D*x1-D*x1);
		double epsilonERR_alt = dEpsilondx*dipoleSumSquaredERR;
		double epsilonERR = 3 * D / Math.pow((B*D*x1-D*x1+1),2)*dipoleSumSquaredERR;

//		System.out.println("epsilonERR_alt: "+epsilonERR_alt);

		System.out.println("-<M^2>*bt*bt:\t"+ (-dipoleSumSquared/temperature/temperature) + " -<M^2>*bt*bt_err:\t"+(dipoleSumSquaredERR/temperature/temperature) );
		System.out.println("AEE_new:\t"+ AEE + " AEE_err:\t" + AEEER );
//		System.out.println("Epsilon="+epsilon + "   with err: "+epsilonERR);
//		System.out.println("========================");
		double C = dipoleFac;
		double A = C/(1+B*C);
		double dielectricConstant = (1+2*A)/(1-A);
//		System.out.println("(epsilon-1)/(epsilon+2): "+A);
//		System.out.println("dielectric constant is:  "+dielectricConstant);

		double avgPE = ((DataDouble)((DataGroup)energyAccumulator.getData()).getData(energyAccumulator.AVERAGE.index)).x;
		avgPE /= numberMolecules;
//		System.out.println("PE/epsilon:"+avgPE);
        long endTime = System.currentTimeMillis();
        System.out.println("endTime : " + date.format(cal.getTime()));
        System.out.println(  "Time taken (in mins): " + (endTime - startTime)/(1000.0*60.0)); 
	}

	// ******************* parameters **********************// 
	public static class Param extends ParameterBase {
		public boolean isGraphic = false;
		public double temperature = 1;				
		public int numberMolecules = 10;
		public double density = 0.05;									
		public double dipoleStrength2 = 1;
		public double HSDiameter = 1.0;
		public double dielectricOutside = 1.0E11;
		public int steps = 1000000;
	}
}
