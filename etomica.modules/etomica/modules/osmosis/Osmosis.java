package etomica.modules.osmosis;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import etomica.action.SimulationRestart;
import etomica.atom.AtomTypeSphere;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverage;
import etomica.data.DataPump;
import etomica.data.DataSourceCountTime;
import etomica.data.meter.MeterLocalMoleFraction;
import etomica.data.meter.MeterTemperature;
import etomica.exception.ConfigurationOverlapException;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DeviceNSelector;
import etomica.graphics.DeviceSlider;
import etomica.graphics.DeviceThermoSelector;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayBoxesCAE;
import etomica.graphics.DisplayPhase;
import etomica.graphics.DisplayTimer;
import etomica.graphics.Drawable;
import etomica.graphics.SimulationGraphic;
import etomica.graphics.SimulationPanel;
import etomica.graphics.DisplayBox.LabelType;
import etomica.integrator.Integrator;
import etomica.integrator.IntervalActionAdapter;
import etomica.lattice.LatticeCubicSimple;
import etomica.math.geometry.Rectangle;
import etomica.modifier.Modifier;
import etomica.potential.P1HardBoundary;
import etomica.potential.P2HardSphere;
import etomica.space.IVector;
import etomica.space2d.Vector2D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Dimension;
import etomica.units.Kelvin;
import etomica.units.Length;
import etomica.units.Unit;
import etomica.util.Constants.CompassDirection;

/**
 * Osmosis module.
 * @author Jhumpa Adhikari
 * @author Andrew Schultz
 */

public class Osmosis extends SimulationGraphic {

	private final static String APP_NAME = "Osmosis";
	private final static int REPAINT_INTERVAL = 40;

    public DataSourceCountTime cycles;
    public DisplayBox displayCycles;
    public MeterOsmoticPressure osmosisPMeter;
    public MeterLocalMoleFraction moleFraction;
    public OsmosisSim sim;

    public Osmosis(OsmosisSim simulation) {

    	super(simulation, GRAPHIC_ONLY, APP_NAME, REPAINT_INTERVAL);

    	sim = simulation;

        final int thickness = 4;

        Unit tUnit = Kelvin.UNIT;

        ConfigurationLattice config = new ConfigurationLattice(new LatticeCubicSimple(2, 1.0));

        SimulationRestart simRestart = getController().getSimRestart();
        
        simRestart.setConfiguration(config);
        getController().setShape("VERTICAL"); //three choices "HORIZONTAL","AUTOMATIC"           

	    //display of phase
        final DisplayPhase displayPhase = getDisplayPhase(sim.phase);
        ColorSchemeByType colorScheme = new ColorSchemeByType();

        colorScheme.setColor(sim.speciesA.getMoleculeType(), Color.blue);
        colorScheme.setColor(sim.speciesB.getMoleculeType(), Color.red);
        displayPhase.setColorScheme(colorScheme);
        displayPhase.setAlign(1,DisplayPhase.BOTTOM);
        displayPhase.getOriginShift()[0] = thickness;
        displayPhase.getOriginShift()[1] = -thickness;
        displayPhase.addDrawable(new MyWall());


        cycles = new DataSourceCountTime();
        displayCycles = new DisplayTimer(sim.integrator);
        displayCycles.setLabelType(LabelType.BORDER);
        displayCycles.setLabel("Time");
	    displayCycles.setPrecision(6);	

        osmosisPMeter = new MeterOsmoticPressure(sim.getSpace(), new P1HardBoundary[]{sim.boundaryHardLeftA}, 
                new P1HardBoundary[]{sim.boundaryHardRightA, sim.boundaryHardB});
        osmosisPMeter.setIntegrator(sim.integrator);
        AccumulatorAverage osmosisPMeterAvg = new AccumulatorAverage(sim);
        DataPump pump = new DataPump(osmosisPMeter, osmosisPMeterAvg);
        sim.register(osmosisPMeter, pump);
        IntervalActionAdapter adapter = new IntervalActionAdapter(pump);
        adapter.setActionInterval(40);
        sim.integrator.addListener(adapter);
        DisplayBoxesCAE dBox = new DisplayBoxesCAE();
        dBox.setAccumulator(osmosisPMeterAvg);
        dBox.setPrecision(6);

        //
        // temperature panel
        //

	    DeviceThermoSelector tSelect = new DeviceThermoSelector(sim, sim.integrator);
	    tSelect.setTemperatures(new double[] {50.,100.,300.,600.,1000.});
	    tSelect.setUnit(tUnit);
	    tSelect.setSelected(0); //sets adiabatic as selected temperature
		MeterTemperature thermometer = new MeterTemperature();
		thermometer.setPhase(sim.phase);
		DisplayBox tBox = new DisplayBox();
        pump = new DataPump(thermometer, tBox);
        adapter = new IntervalActionAdapter(pump);
        sim.integrator.addListener(adapter);
		tBox.setUnit(tUnit);
		tBox.setLabel("Measured value");
		tBox.setLabelPosition(CompassDirection.NORTH);
	    tSelect.getLabel().setText("Set value");

        JPanel temperaturePanel = new JPanel(new GridBagLayout());

        temperaturePanel.setBorder(new TitledBorder(null, "Temperature (K)", TitledBorder.CENTER, TitledBorder.TOP));
        temperaturePanel.add(tSelect.graphic(null),SimulationPanel.getHorizGBC());
        temperaturePanel.add(tBox.graphic(null),SimulationPanel.getHorizGBC());

        moleFraction = new MeterLocalMoleFraction();
        moleFraction.setPhase(sim.phase);
        IVector dimensions = sim.phase.getBoundary().getDimensions();
        moleFraction.setShape(new Rectangle(sim.getSpace(), dimensions.x(0)*0.5, dimensions.x(1)));
        moleFraction.setShapeOrigin(new Vector2D(dimensions.x(0)*0.25, 0));
        moleFraction.setSpecies(sim.speciesB);
        AccumulatorAverage moleFractionAvg = new AccumulatorAverage(sim);
        pump = new DataPump(moleFraction, moleFractionAvg);
        sim.register(moleFraction, pump);
        adapter = new IntervalActionAdapter(pump);
        sim.integrator.addListener(adapter);
        DisplayBoxesCAE mfBox = new DisplayBoxesCAE();
        mfBox.setAccumulator(moleFractionAvg);
        mfBox.setPrecision(8);
	    

        DeviceNSelector nASelector = new DeviceNSelector(sim.getController());
        nASelector.setResetAction(simRestart);
        nASelector.setSpeciesAgent(sim.phase.getAgent(sim.speciesA));
        nASelector.getSlider().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
            	displayPhase.repaint();
            }
        });
        nASelector.setMaximum(30);
        
        DeviceNSelector nBSelector = new DeviceNSelector(sim.getController());
        nBSelector.setResetAction(simRestart);
        nBSelector.setSpeciesAgent(sim.phase.getAgent(sim.speciesB));
        nBSelector.getSlider().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
            	displayPhase.repaint();
            }
        });
        nBSelector.setMaximum(10);
	    
        DiameterModifier diameterModifier = new DiameterModifier(sim.potentialAA,sim.potentialBB,sim.potentialAB,
                                                            sim.boundarySemiB,
                                                            sim.boundaryHardTopBottomA,sim.boundaryHardLeftA,sim.boundaryHardRightA,
                                                            sim.boundaryHardB,
                                                            sim.speciesA,sim.speciesB);
        diameterModifier.setDisplay(getDisplayPhase(sim.phase));
        DeviceSlider sliderDiameter = new DeviceSlider(sim.getController());
        sliderDiameter.setModifier(diameterModifier);

		sliderDiameter.setPrecision(2);
		sliderDiameter.setMaximum(4);
		sliderDiameter.setMinimum(0);
		sliderDiameter.setValue(3);
		sliderDiameter.setNMajor(4);

        //panel for the temperature control/display
        JPanel cyclesPanel = new JPanel(new FlowLayout());
        cyclesPanel.setBorder(new TitledBorder(null, "Cycles", TitledBorder.CENTER, TitledBorder.TOP));
        cyclesPanel.add(displayCycles.graphic(null));

        

        //panel for the meter displays

        JPanel osmoticPanel = new JPanel(new FlowLayout());
        osmoticPanel.setBorder(new TitledBorder(null, "Osmotic Pressure (PV/Nk)", TitledBorder.CENTER, TitledBorder.TOP));
        osmoticPanel.add(dBox.graphic(null));

        JPanel moleFractionPanel = new JPanel(new FlowLayout());
        TitledBorder titleBorder = new TitledBorder(null, "Mole Fraction (nSolute/nSolution)", TitledBorder.CENTER, TitledBorder.TOP);
        moleFractionPanel.setBorder(titleBorder);
        moleFractionPanel.add(mfBox.graphic(null));
        
        JTabbedPane tabPaneMeter = new JTabbedPane();
        tabPaneMeter.addTab("Osmotic Pressure", osmoticPanel);
        tabPaneMeter.addTab("Mole Fraction", moleFractionPanel);
        tabPaneMeter.addTab("Cycles", cyclesPanel);

        //panel for sliders

        JPanel sliderPanelA = new JPanel(new GridLayout(0,1));
        nASelector.setShowBorder(false);
        sliderPanelA.add(nASelector.graphic(null));
        sliderPanelA.setBorder(new TitledBorder
           (null, "Set "+nASelector.getLabel(), TitledBorder.CENTER, TitledBorder.TOP));
        
        JPanel sliderPanelB = new JPanel(new GridLayout(0,1));
        nBSelector.setShowBorder(false);
        sliderPanelB.add(nBSelector.graphic(null));
        sliderPanelB.setBorder(new TitledBorder
           (null, "Set "+nBSelector.getLabel(), TitledBorder.CENTER, TitledBorder.TOP));

        JPanel sliderDiaPanel = new JPanel(new GridLayout(0,1));
        sliderDiameter.setShowBorder(false);
        sliderDiaPanel.add(sliderDiameter.graphic(null));
        sliderDiaPanel.setBorder(new TitledBorder
           (null, "Set Diameter", TitledBorder.CENTER, TitledBorder.TOP));

        JTabbedPane tabPaneSliders = new JTabbedPane();
        tabPaneSliders.addTab(nASelector.getLabel(), sliderPanelA);
        tabPaneSliders.addTab(nBSelector.getLabel(), sliderPanelB);
        tabPaneSliders.addTab("Diameter", sliderDiaPanel);


        //panel for all the controls

        GridBagConstraints vertGBC = SimulationPanel.getVertGBC();

        JTabbedPane tabPane = new JTabbedPane();
        tabPane.addTab("Meters", tabPaneMeter);
        tabPane.addTab("Sliders", tabPaneSliders);
        
        getPanel().controlPanel.add(temperaturePanel, vertGBC);
        getPanel().controlPanel.add(tabPane, vertGBC);

        getController().getReinitButton().setPostAction(getDisplayPhasePaintAction(sim.phase));

    }

	//drawable that puts a line down the middle of the box, where the
	//semipermeable membrane potential acts
    protected class MyWall implements Drawable {
    	public void draw(Graphics g, int[] origin, double scale) {
    		int x1 = origin[0]+(int)(0.5*scale*sim.phase.getBoundary().getDimensions().x(0));
    		int y1 = origin[1];
			int h = (int)(scale*sim.phase.getBoundary().getDimensions().x(1));
			int w = 4;
			g.setColor(Color.green);
    		g.fillRect(x1-w, y1, w, h);
    	}
    }

    

    public static void main(String[] args) {

    	OsmosisSim sim = new OsmosisSim();
        sim.activityIntegrate.setDoSleep(true);
        sim.activityIntegrate.setSleepPeriod(1);
        sim.register(sim.integrator);

        Osmosis osmosis = new Osmosis(sim);
        SimulationGraphic.makeAndDisplayFrame(osmosis.getPanel(), APP_NAME);
        System.out.println("Event intreval : " + sim.integrator.getEventInterval());
    }

    
    public class DiameterModifier implements Modifier {
        
        P2HardSphere potentialAA,potentialBB,potentialAB;
        P1HardWall membraneB;
        P1HardBoundary boundaryHard1A,boundaryHard2A,boundaryHard3A;
        P1HardBoundary boundaryHardB;
        SpeciesSpheresMono speciesA,speciesB;
        DisplayPhase display;
        Integrator integrator;
        
        public DiameterModifier(P2HardSphere potentialAA ,P2HardSphere potentialBB ,P2HardSphere potentialAB ,
                          P1HardWall membraneB,
                          P1HardBoundary boundaryHard1A, P1HardBoundary boundaryHard2A, P1HardBoundary boundaryHard3A, 
                          P1HardBoundary boundaryHardB,
                          SpeciesSpheresMono speciesA ,SpeciesSpheresMono speciesB) {
            this.potentialAA = potentialAA;
            this.potentialBB = potentialBB;
            this.potentialAB = potentialAB;
            this.membraneB = membraneB;
            this.boundaryHard1A = boundaryHard1A;
            this.boundaryHard2A = boundaryHard2A;
            this.boundaryHard3A = boundaryHard3A;
            this.boundaryHardB = boundaryHardB;
            this.speciesA = speciesA;
            this.speciesB = speciesB;
        }
        
        public String getLabel() {
            return "a label";
        }
        
        public Dimension getDimension() {
            return Length.DIMENSION;
        }
        
        public void setValue(double d) {
            potentialAA.setCollisionDiameter(d);
            potentialBB.setCollisionDiameter(d);
            potentialAB.setCollisionDiameter(d);
            boundaryHard1A.setCollisionRadius(0.5*d);
            boundaryHard2A.setCollisionRadius(0.5*d);
            boundaryHard3A.setCollisionRadius(0.5*d);
            membraneB.setCollisionRadius(0.5*d);
            boundaryHardB.setCollisionRadius(0.5*d);
            ((AtomTypeSphere)speciesA.getMoleculeType()).setDiameter(d);
            ((AtomTypeSphere)speciesB.getMoleculeType()).setDiameter(d);
            if (display != null) {
                display.repaint();
            }
            if (integrator != null) {
                try {
                    integrator.reset();
                }
                catch (ConfigurationOverlapException e) {
                    //overlaps are likely after increasing the diameter
                }
            }
        }
        
        public double getValue() {
            return ((AtomTypeSphere)speciesA.getMoleculeType()).getDiameter();
        }
        
        public void setDisplay(DisplayPhase newDisplay){
            display = newDisplay;
        }
        
        public void setIntegrator(Integrator newIntegrator) {
            integrator = newIntegrator;
        }
    }

    public static class Applet extends javax.swing.JApplet {
	    public void init() {
	    	OsmosisSim sim = new OsmosisSim();
	        sim.activityIntegrate.setDoSleep(true);
	        sim.activityIntegrate.setSleepPeriod(1);
	        sim.register(sim.integrator);

		    getContentPane().add(new Osmosis(sim).getPanel());
	    }

        private static final long serialVersionUID = 1L;
    }

}








