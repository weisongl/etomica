// Source file generated by Etomica

package etomica.simulations;

import etomica.ConfigurationLinear;
import etomica.Default;
import etomica.Phase;
import etomica.Simulation;
import etomica.Space;
import etomica.Species;
import etomica.SpeciesSpheres;
import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomFactoryHomo;
import etomica.atom.iterator.ApiIntergroup;
import etomica.atom.iterator.AtomsetIteratorFiltered;
import etomica.data.DataSourceCOM;
import etomica.integrator.IntegratorHard;
import etomica.nbr.NeighborCriterion;
import etomica.nbr.NeighborCriterionSimple;
import etomica.nbr.NeighborCriterionWrapper;
import etomica.nbr.PotentialMasterNbr;
import etomica.potential.P1BondedHardSpheres;
import etomica.potential.P2HardSphere;
import etomica.potential.PotentialGroup;

public class ChainHSMD3D extends Simulation {

    public Phase phase;
    public IntegratorHard integrator;
    public SpeciesSpheres species;
    public P2HardSphere potential;
    
    public ChainHSMD3D() {
        this(new etomica.space3d.Space3D());
    }
    private ChainHSMD3D(Space space) {
//        super(space, new PotentialMaster(space));
        super(space, new PotentialMasterNbr(space));
        Default.FIX_OVERLAP = true;
        int numAtoms = 704;
        int chainLength = 4;
        double neighborRangeFac = 1.6;
        Default.makeLJDefaults();
        Default.ATOM_SIZE = 1.0;
        Default.BOX_SIZE = 14.4573*Math.pow((chainLength*numAtoms/2020.0),1.0/3.0);
        int nCells = (int)(Default.BOX_SIZE/neighborRangeFac);
        System.out.println("nCells: "+nCells);
        ((PotentialMasterNbr)potentialMaster).setNCells(nCells);
        ((PotentialMasterNbr)potentialMaster).setMaxNeighborRange(neighborRangeFac);
        ((PotentialMasterNbr)potentialMaster).setAtomPositionDefinition(new DataSourceCOM(space));

        integrator = new IntegratorHard(potentialMaster);
        integrator.setIsothermal(false);
        integrator.addIntervalListener(((PotentialMasterNbr)potentialMaster).getNeighborManager());
        integrator.setTimeStep(0.01);
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheres(space,((PotentialMasterNbr)potentialMaster).sequencerFactory(),numAtoms,chainLength);
        ((ConfigurationLinear)((AtomFactoryHomo)species.getFactory()).getConfiguration()).setBondLength(Default.ATOM_SIZE);
        ((ConfigurationLinear)((AtomFactoryHomo)species.getFactory()).getConfiguration()).setAngle(1,0.5);
        
        phase = new Phase(space);
        
        P1BondedHardSpheres p1Intra = new P1BondedHardSpheres(space);
        potentialMaster.setSpecies(p1Intra,new Species[]{species});
        
        PotentialGroup p2Inter = new PotentialGroup(2);
        potential = new P2HardSphere(space);
        NeighborCriterion criterion = new NeighborCriterionSimple(space,potential.getRange(),neighborRangeFac*potential.getRange());
        AtomsetIteratorFiltered interIterator = new AtomsetIteratorFiltered(new ApiIntergroup(),criterion);
        p2Inter.addPotential(potential,interIterator);
        NeighborCriterionWrapper moleculeCriterion = new NeighborCriterionWrapper(new NeighborCriterion[]{criterion});
        moleculeCriterion.setNeighborRange(3.45 + criterion.getNeighborRange());
        ((PotentialMasterNbr)potentialMaster).setSpecies(p2Inter,new Species[]{species,species},moleculeCriterion);
        ((PotentialMasterNbr)potentialMaster).getNeighborManager().addCriterion(criterion);
        
        //        Crystal crystal = new LatticeCubicFcc(space);
//        ConfigurationLattice configuration = new ConfigurationLattice(space, crystal);
//        phase.setConfiguration(configuration);
//        potential = new P2HardSphere(space);
//        this.potentialMaster.setSpecies(potential,new Species[]{species,species});

//        NeighborCriterion criterion = new NeighborCriterionSimple(space,potential.getRange(),neighborRangeFac*potential.getRange());
//        ((PotentialMasterNbr)potentialMaster).setSpecies(potential,new Species[]{species,species},criterion);

//      elementCoordinator.go();
        //explicit implementation of elementCoordinator activities
        phase.speciesMaster.addSpecies(species);
        integrator.addPhase(phase);
 //       integrator.addIntervalListener(new PhaseImposePbc(phase));
        
        //ColorSchemeByType.setColor(speciesSpheres0, java.awt.Color.blue);

 //       MeterPressureHard meterPressure = new MeterPressureHard(integrator);
 //       DataManager accumulatorManager = new DataManager(meterPressure);
        // 	DisplayBox box = new DisplayBox();
        // 	box.setDatumSource(meterPressure);
 //       phase.setDensity(0.7);
    } //end of constructor

}//end of class
