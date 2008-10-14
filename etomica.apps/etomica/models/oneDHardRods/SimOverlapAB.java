package etomica.models.oneDHardRods;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.box.Box;
import etomica.data.DataPump;
import etomica.data.DataSource;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.IntegratorMC;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.nbr.list.PotentialMasterList;
import etomica.normalmode.CoordinateDefinitionLeaf;
import etomica.normalmode.NormalModes1DHR;
import etomica.normalmode.P2XOrder;
import etomica.normalmode.WaveVectorFactory;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential2;
import etomica.potential.Potential2HardSpherical;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Null;
import etomica.util.ParameterBase;
import etomica.util.ReadParameters;
import etomica.virial.overlap.AccumulatorVirialOverlapSingleAverage;
import etomica.virial.overlap.DataSourceVirialOverlap;
import etomica.virial.overlap.IntegratorOverlap;

public class SimOverlapAB extends Simulation {
    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "SimOverlapAB";
    Primitive primitive;
    int[] nCells;
    NormalModes1DHR nm;
    double bennettParam;       //adjustable parameter - Bennett's parameter
    public IntegratorOverlap integratorOverlap;
    public DataSourceVirialOverlap dsvo;
    ActivityIntegrate activityIntegrate;
    
    IntegratorMC[] integrators;
    public AccumulatorVirialOverlapSingleAverage[] accumulators;
    public DataPump[] accumulatorPumps;
    public DataSource[] meters;
    public IBox boxTarget, boxRef;
    public Boundary boundaryTarget, boundaryRef;
    MCMoveChangeMode changeMove;
    MCMoveConvertMode convertMove;
    MeterPotentialEnergy meterAinB, meterAinA;
    MeterConvertModeBrute meterBinA, meterBinB;
    MeterOverlap meterOverlapInA, meterOverlapInB;
    
    public SimOverlapAB(Space _space, int numAtoms, double density, double 
            temperature, String filename, double harmonicFudge, int awv){
        super(_space, true);
        
        //Set up some of the joint stuff
        SpeciesSpheresMono species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        
        integrators = new IntegratorMC[2];
        accumulatorPumps = new DataPump[2];
        meters = new DataSource[2];
        accumulators = new AccumulatorVirialOverlapSingleAverage[2];
        
        
        
        //Set up target system   -A
        PotentialMasterList potentialMasterTarget = new 
            PotentialMasterList(this, space);
        boxTarget = new Box(this, space);
        addBox(boxTarget);
        boxTarget.setNMolecules(species, numAtoms);
        
        Potential2 p2 = new P2HardSphere(space, 1.0, true);
        p2 = new P2XOrder(space, (Potential2HardSpherical)p2);
        p2.setBox(boxTarget);
        potentialMasterTarget.addPotential(p2, new IAtomTypeLeaf[]
                {species.getLeafType(), species.getLeafType()});
        
        primitive = new PrimitiveCubic(space, 1.0/density);
        boundaryTarget = new BoundaryRectangularPeriodic(space, getRandom(),
                numAtoms/density);
        nCells = new int[]{numAtoms};
        boxTarget.setBoundary(boundaryTarget);
        
        CoordinateDefinitionLeaf coordinateDefinitionTarget = new 
                CoordinateDefinitionLeaf(this, boxTarget, primitive, space);
        coordinateDefinitionTarget.initializeCoordinates(nCells);
        
        double neighborRange = 1.01/density;
        potentialMasterTarget.setRange(neighborRange);
        //find neighbors now.  Don't hook up NeighborListManager since the
        //  neighbors won't change
        potentialMasterTarget.getNeighborManager(boxTarget).reset();
        
        IntegratorMC integratorTarget = new IntegratorMC(potentialMasterTarget, 
                random, temperature);
        integrators[1] = integratorTarget;
        integratorTarget.setBox(boxTarget);
        activityIntegrate = new ActivityIntegrate(integratorTarget);
        getController().addAction(activityIntegrate);
        
        nm = new NormalModes1DHR(space.D());
        nm.setHarmonicFudge(harmonicFudge);
        nm.setTemperature(temperature);
        
        WaveVectorFactory waveVectorFactoryTarget = nm.getWaveVectorFactory();
        waveVectorFactoryTarget.makeWaveVectors(boxTarget);
        
        changeMove = new MCMoveChangeMode(potentialMasterTarget, random);
        integratorTarget.getMoveManager().addMCMove(changeMove);
        changeMove.setWaveVectors(waveVectorFactoryTarget.getWaveVectors());
        changeMove.setWaveVectorCoefficients(
                waveVectorFactoryTarget.getCoefficients());
        changeMove.setEigenVectors(nm.getEigenvectors(boxTarget));
        changeMove.setCoordinateDefinition(coordinateDefinitionTarget);
        changeMove.setBox((IBox)boxTarget);
        changeMove.setStepSizeMin(0.001);
        changeMove.setStepSize(0.01);
        
        meterAinA = new MeterPotentialEnergy(potentialMasterTarget);
        meterAinA.setBox(boxTarget);
        
        meterBinA = new MeterConvertModeBrute(potentialMasterTarget, 
                coordinateDefinitionTarget, boxTarget);
        meterBinA.setEigenVectors(nm.getEigenvectors(boxTarget));
        meterBinA.setOmegaSquared(nm.getOmegaSquared(boxTarget));
        meterBinA.setTemperature(temperature);
        meterBinA.setWaveVectorCoefficients(waveVectorFactoryTarget.getCoefficients());
        meterBinA.setWaveVectors(waveVectorFactoryTarget.getWaveVectors());
        
        meterOverlapInA = new MeterOverlap("MeterOverlapInA", Null.DIMENSION, 
                meterAinA, meterBinA, temperature);
        meters[0] = meterOverlapInA;
        
        integratorTarget.setBox(boxTarget);
        //nan do we need to reset the potentialmaster neighbormanager here?
        potentialMasterTarget.getNeighborManager(boxTarget).reset();

        
        
        
        
        //Set up reference system - Right now, SystemB - hybrid system
        PotentialMasterList potentialMasterRef = new 
            PotentialMasterList(this, space);
        boxRef = new Box(this, space);
        addBox(boxRef);
        boxRef.setNMolecules(species, numAtoms);
        
        p2 = new P2HardSphere(space, 1.0, true);
        p2 = new P2XOrder(space, (Potential2HardSpherical)p2);
        p2.setBox(boxRef);
        potentialMasterRef.addPotential(p2, new IAtomTypeLeaf[]
                {species.getLeafType(), species.getLeafType()});
        
        primitive = new PrimitiveCubic(space, 1.0/density);
        boundaryRef = new BoundaryRectangularPeriodic(space, getRandom(),
                numAtoms/density);
        nCells = new int[]{numAtoms};
        boxRef.setBoundary(boundaryRef);
        
        CoordinateDefinitionLeaf coordinateDefinitionRef = new 
                CoordinateDefinitionLeaf(this, boxRef, primitive, space);
        coordinateDefinitionRef.initializeCoordinates(nCells);
        
        neighborRange = 1.01/density;
        potentialMasterRef.setRange(neighborRange);
        //find neighbors now.  Don't hook up NeighborListManager since the
        //  neighbors won't change
        potentialMasterRef.getNeighborManager(boxRef).reset();
        
        IntegratorMC integratorRef = new IntegratorMC(potentialMasterRef, 
                random, temperature);
        integratorRef.setBox(boxRef);
        activityIntegrate = new ActivityIntegrate(integratorRef);
        getController().addAction(activityIntegrate);
        
        nm = new NormalModes1DHR(space.D());
        nm.setHarmonicFudge(harmonicFudge);
        nm.setTemperature(temperature);
        
        WaveVectorFactory waveVectorFactoryRef = nm.getWaveVectorFactory();
        waveVectorFactoryRef.makeWaveVectors(boxRef);
        
        
        convertMove = new MCMoveConvertMode(potentialMasterRef, 
                random);
        integratorRef.getMoveManager().addMCMove(convertMove);
        convertMove.setWaveVectors(waveVectorFactoryRef.getWaveVectors());
        convertMove.setWaveVectorCoefficients(waveVectorFactoryRef.getCoefficients());
        convertMove.setOmegaSquared(nm.getOmegaSquared(boxRef), 
                waveVectorFactoryRef.getCoefficients());
        convertMove.setEigenVectors(nm.getEigenvectors(boxRef));
        convertMove.setCoordinateDefinition(coordinateDefinitionRef);
        convertMove.setTemperature(temperature);
        convertMove.setBox((IBox)boxRef);
        convertMove.setStepSizeMin(0.001);
        convertMove.setStepSize(0.01);
        
        meterAinB = new MeterPotentialEnergy(potentialMasterRef);
        meterAinB.setBox(boxRef);
       
        meterBinB = new MeterConvertModeBrute(potentialMasterRef,
                coordinateDefinitionRef, boxRef);
        meterBinB.setCoordinateDefinition(coordinateDefinitionRef);
        meterBinB.setEigenVectors(nm.getEigenvectors(boxRef));
        meterBinB.setOmegaSquared(nm.getOmegaSquared(boxRef));
        meterBinB.setTemperature(temperature);
        meterBinB.setWaveVectorCoefficients(waveVectorFactoryRef.getCoefficients());
        meterBinB.setWaveVectors(waveVectorFactoryRef.getWaveVectors());
        
        
        meterOverlapInB = new MeterOverlap("MeterOverlapInB", Null.DIMENSION, 
                meterAinB, meterBinB, temperature);
        meters[1] = meterOverlapInB;
        
        integratorRef.setBox(boxRef);
        potentialMasterRef.getNeighborManager(boxRef).reset();
        
        
        
        //Set up the rest of the joint stuff
        setAffectedWaveVector(awv);
        
        integratorOverlap = new IntegratorOverlap(random, new 
                IntegratorMC[]{integratorRef, integratorTarget});
        
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, false), 0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(10, 11, true), 1);
        
        setBennettParameter(1.0, 30);
        
        activityIntegrate = new ActivityIntegrate(integratorOverlap);
        getController().addAction(activityIntegrate);
    }
    
    public void setBennettParameter(double benParamCenter, double span) {
        bennettParam = benParamCenter;
        accumulators[0].setBennetParam(benParamCenter,span);
        accumulators[1].setBennetParam(benParamCenter,span);
    }
    
    public void setBennettParameter(double newBennettParameter) {
        System.out.println("setting ref pref (explicitly) to "+
                newBennettParameter);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
        setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
        setBennettParameter(newBennettParameter,1);
    }
    
    public void initBennettParameter(String fileName, long initSteps) {
        // benParam = -1 indicates we are searching for an appropriate value
        bennettParam = -1.0;
        if (fileName != null) {
            try { 
                FileReader fileReader = new FileReader(fileName);
                BufferedReader bufReader = new BufferedReader(fileReader);
                String benParamString = bufReader.readLine();
                bennettParam = Double.parseDouble(benParamString);
                bufReader.close();
                fileReader.close();
                System.out.println("setting ref pref (from file) to "+bennettParam);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
                setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
                setBennettParameter(bennettParam,1);
            }
            catch (IOException e) {
                // file not there, which is ok.
            }
        }
        
        if (bennettParam == -1) {
            // equilibrate off the lattice to avoid anomolous contributions
            activityIntegrate.setMaxSteps(initSteps/2);
            getController().actionPerformed();
            getController().reset();

            setAccumulator(new AccumulatorVirialOverlapSingleAverage(41,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(41,false),1);
            setBennettParameter(1e40,40);
            activityIntegrate.setMaxSteps(initSteps);
            getController().actionPerformed();
            getController().reset();

            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            if (Double.isNaN(bennettParam) || bennettParam == 0 || Double.isInfinite(bennettParam)) {
                throw new RuntimeException("Simulation failed to find a valid ref pref");
            }
            System.out.println("setting ref pref to "+bennettParam);
            
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(11,false),1);
            setBennettParameter(bennettParam,5);

            // set benParam back to -1 so that later on we know that we've been looking for
            // the appropriate value
            bennettParam = -1;
            getController().reset();
        }

    }
    public void setAccumulator(AccumulatorVirialOverlapSingleAverage 
            newAccumulator, int iBox) {
        accumulators[iBox] = newAccumulator;
        if (accumulatorPumps[iBox] == null) {
            accumulatorPumps[iBox] = new DataPump(meters[iBox],newAccumulator);
            integrators[iBox].addIntervalAction(accumulatorPumps[iBox]);
        }
        else {
            accumulatorPumps[iBox].setDataSink(newAccumulator);
        }
        if (integratorOverlap != null && accumulators[0] != null && 
                accumulators[1] != null) {
            dsvo = new DataSourceVirialOverlap(accumulators[0],accumulators[1]);
            integratorOverlap.setDSVO(dsvo);
        }
    }
    
    public void equilibrate(String fileName, long initSteps) {
        // run a short simulation to get reasonable MC Move step sizes and
        // (if needed) narrow in on a reference preference
        activityIntegrate.setMaxSteps(initSteps);
        
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(true);
        }
        getController().actionPerformed();
        getController().reset();
        for (int i=0; i<2; i++) {
            if (integrators[i] instanceof IntegratorMC) ((IntegratorMC)integrators[i]).getMoveManager().setEquilibrating(false);
        }
        
        if (bennettParam == -1) {
            int newMinDiffLoc = dsvo.minDiffLocation();
            bennettParam = accumulators[0].getBennetAverage(newMinDiffLoc)
                /accumulators[1].getBennetAverage(newMinDiffLoc);
            System.out.println("setting ref pref to "+bennettParam+" ("+newMinDiffLoc+")");
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,true),0);
            setAccumulator(new AccumulatorVirialOverlapSingleAverage(1,false),1);
            setBennettParameter(bennettParam,1);
            if (fileName != null) {
                try {
                    FileWriter fileWriter = new FileWriter(fileName);
                    BufferedWriter bufWriter = new BufferedWriter(fileWriter);
                    bufWriter.write(String.valueOf(bennettParam)+"\n");
                    bufWriter.close();
                    fileWriter.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("couldn't write to Bennet parameter file");
                }
            }
        }
        else {
            dsvo.reset();
        }
    }
    
    
    public static void main(String args[]){
        SimOverlapABParam params = new SimOverlapABParam();
        String inputFilename = null;
        if(args.length > 0){
            inputFilename = args[0];
        }
        if(inputFilename != null){
            ReadParameters readParameters = new 
                ReadParameters(inputFilename, params);
            readParameters.readParameters();
        }
        double density = params.density;
        long numSteps = params.numSteps;
        int numMolecules = params.numAtoms;
        double harmonicFudge = params.harmonicFudge;
        double temperature = params.temperature;
        int D = params.D;
        int affectedWV = params.affectedWV;
        String filename = params.filename;
        if(filename.length() == 0){
            filename = "1DHR";
        }
        String refFileName = args.length > 0 ? filename+"_ref" : null;
        
        System.out.println("Running Nancy's 1DHR simulation");
        System.out.println(numMolecules+" atoms at density "+density);
        System.out.println("harmonic fudge: "+harmonicFudge);
        System.out.println((numSteps/1000)+" total steps of 1000");
        System.out.println("output data to "+filename);
        
        //instantiate simulations!
        SimOverlapAB sim = new SimOverlapAB(Space.getInstance(D), numMolecules,
                density, temperature, filename, harmonicFudge, affectedWV);
        
        //start simulation & equilibrate
        sim.integratorOverlap.setNumSubSteps(1000);
        numSteps /= 1000;
        sim.initBennettParameter(filename, numSteps/20);
        if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || Double.isInfinite(sim.bennettParam)){
//            System.out.println("bennet info:  " + )
            throw new RuntimeException("Simulation failed to find a valid Bennett parameter");
        }
        sim.equilibrate(refFileName, numSteps/10);
        if(Double.isNaN(sim.bennettParam) || sim.bennettParam == 0 || Double.isInfinite(sim.bennettParam)){
            
//            System.out.println("bennet info:  " + )
            throw new RuntimeException("Simulation failed to find a valid Bennett parameter");
        }
        System.out.println("equilibration finsihed.")
        
        sim.integratorOverlap.getMoveManager().setEquilibrating(false);
        sim.activityIntegrate.setMaxSteps(numSteps);
        sim.getController().actionPerformed();
        System.out.println("final reference optimal step frequency "+sim.integratorOverlap.getStepFreq0()+" (actual: "+sim.integratorOverlap.getActualStepFreq0()+")");
        
        
        
        
    }
    
    
    public void setAffectedWaveVector(int awv){
        convertMove.setConvertedWaveVector(awv);
        meterBinA.setConvertedWV(awv);
        meterBinB.setConvertedWV(awv);
    }
    public static class SimOverlapABParam extends ParameterBase {
        public int numAtoms = 32;
        public double density = 0.5;
        public int D = 1;
        public long numSteps = 100000;
        public double harmonicFudge = 1.0;
        public String filename = "HR1D_";
        public double temperature = 1.0;
        public int affectedWV = 16;
    }
    
}
