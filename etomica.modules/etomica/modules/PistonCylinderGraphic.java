package etomica.modules.pistoncylinder;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import etomica.Action;
import etomica.Constants;
import etomica.Default;
import etomica.Phase;
import etomica.Species;
import etomica.action.IntegratorReset;
import etomica.data.DataSourceCountSteps;
import etomica.data.meter.MeterDensity;
import etomica.data.meter.MeterTemperature;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DeviceSlider;
import etomica.graphics.DeviceThermoSelector;
import etomica.graphics.DeviceToggleButton;
import etomica.graphics.DeviceTrioControllerButton;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayCanvasInterface;
import etomica.graphics.DisplayPhase;
import etomica.graphics.SimulationGraphic;
import etomica.modifier.ModifierBoolean;
import etomica.modifier.ModifierFunctionWrapper;
import etomica.potential.P2HardSphere;
import etomica.potential.P2SquareWell;
import etomica.potential.PotentialGroup;
import etomica.simulations.PistonCylinder;
import etomica.units.Bar;
import etomica.units.BaseUnit;
import etomica.units.BaseUnitPseudo3D;
import etomica.units.Kelvin;
import etomica.units.Liter;
import etomica.units.Mole;
import etomica.units.Prefix;
import etomica.units.PrefixedUnit;
import etomica.units.Unit;
import etomica.units.UnitRatio;


public class PistonCylinderGraphic {
    
    public JPanel panel, displayPhasePanel;
    public PistonCylinder pc;
    public P2HardSphere potentialHS;
    public P2SquareWell potentialSW;
    public PotentialGroup potentialGroupHS, potentialGroupSW;
    public DataSourceCountSteps meterCycles;
    public DisplayBox displayCycles, tBox, dBoxAvg, dBoxCur;
    public MeterTemperature thermometer;
    public DisplayPhase displayPhase;
    public DeviceTrioControllerButton controlButtons;
    public DeviceThermoSelector tSelect;
    public ItemListener potentialChooserListener;
    public JComboBox potentialChooser;
    public DeviceSlider scaleSlider, pressureSlider;
    public MeterDensity densityMeter;
    public DeviceToggleButton fixPistonButton;
    
    public PistonCylinderGraphic() {
        displayPhase = new DisplayPhase(null);
        displayPhase.setColorScheme(new ColorSchemeByType());

        displayCycles = new DisplayBox();

        Unit pUnit = new BaseUnitPseudo3D.Pressure(Bar.UNIT);
        Unit dUnit = new UnitRatio(Mole.UNIT, 
                                           new BaseUnitPseudo3D.Volume(Liter.UNIT));
        Unit dadUnit = new UnitRatio(new PrefixedUnit(Prefix.DECI, Mole.UNIT), 
                                   new PrefixedUnit(new BaseUnitPseudo3D.Volume(Liter.UNIT)));
        Unit tUnit = Kelvin.UNIT;
        
        Default.ATOM_SIZE = 0.8;
        
        final int p0 = 500;
        
        //restart action and button
        controlButtons = new DeviceTrioControllerButton();
        
        //temperature selector
        tSelect = new DeviceThermoSelector();
	    tSelect.setTemperatures(new double[] {5.,10.,50.,100.,200.,500.});
	    tSelect.setUnit(tUnit);
	    tSelect.setSelected(0); //sets adiabatic as selected temperature
	    tSelect.getLabel().setText("Set value");
	    

	    //combo box to select potentials
//	    final AtomPairIterator iterator = potentialDisks.iterator();
	    potentialChooser = new javax.swing.JComboBox(new String[] {
	        "Ideal gas", "Repulsion only", "Repulsion and attraction"});
	    potentialChooser.setSelectedIndex(0);

//      displayPhase.setAlign(1,DisplayPhase.BOTTOM);
//        displayPhase.canvas.setDrawBoundary(DisplayCanvasInterface.DRAW_BOUNDARY_NONE);
//        displayPhase.getOriginShift()[0] = thickness;
//        displayPhase.getOriginShift()[1] = -thickness;
	    
	    //slider for scale of display
	    ModifierFunctionWrapper scaleModulator = new ModifierFunctionWrapper(displayPhase, "scale");
	    scaleModulator.setFunction(new etomica.utility.Function.Linear(0.01, 0.0));
	    scaleSlider = new DeviceSlider(null, scaleModulator);
	    JPanel scaleSliderPanel = new JPanel(new java.awt.GridLayout(0,1));
	    scaleSliderPanel.add(scaleSlider.graphic());	    
//        scaleSliderPanel.setBorder(new javax.swing.border.TitledBorder("Scale (%)"));
	    scaleSlider.getSlider().addChangeListener(new javax.swing.event.ChangeListener() {
	        public void stateChanged(javax.swing.event.ChangeEvent evt) {
	            if(displayPhase.graphic() != null) displayPhase.graphic().repaint();
	        }
	    });
	    scaleSlider.setMinimum(10);
	    scaleSlider.setMaximum(100);
//	    scaleSlider.getSlider().setSnapToTicks(true);
	    scaleSlider.getSlider().setValue(100);
	    scaleSlider.getSlider().setMajorTickSpacing(10);
	    scaleSlider.getSlider().setMinorTickSpacing(5);
	    scaleSlider.getSlider().setOrientation(1);
	    scaleSlider.getSlider().setLabelTable(scaleSlider.getSlider().createStandardLabels(10));
		
		//add meter and display for current kinetic temperature
		Default.HISTORY_PERIOD = 1000;

		thermometer = new MeterTemperature();
//		thermometer.setHistorying(true);
		tBox = new DisplayBox();
        tBox.setUpdateInterval(100);
		tBox.setDataSource(thermometer);
//		tBox.setWhichValue(MeterAbstract.CURRENT);
		tBox.setUnit(tUnit);
		tBox.setLabel("Measured value");
		tBox.setLabelPosition(Constants.NORTH);

        //meter and display for density
        densityMeter = new MeterDensity();
 //       densityMeter.setHistorying(true);
//        densityMeter.setFunction(new etomica.utility.Function() {
//            public double f(double x) {return x/((1-(sigma+t)/w) + (sigma+t)*((sigma+t)-w)*x/N);}
//            public double inverse(double x) {return 0.0;}//not needed
//            public double dfdx(double x) {return 0.0;}//not needed
//        });
        dBoxAvg = new DisplayBox();
        dBoxAvg.setUpdateInterval(100);
        dBoxAvg.setDataSource(densityMeter);
        dBoxAvg.setUnit(dUnit);
//        dBoxAvg.setWhichValue(MeterAbstract.AVERAGE);
        dBoxAvg.setLabelType(DisplayBox.BORDER);
        dBoxAvg.setLabel("Average");
        dBoxAvg.setPrecision(6);
        dBoxCur = new DisplayBox();
        dBoxCur.setUpdateInterval(100);
        dBoxCur.setDataSource(densityMeter);
        dBoxCur.setUnit(dUnit);
//        dBoxCur.setWhichValue(MeterAbstract.MOST_RECENT);
        dBoxCur.setLabelType(DisplayBox.BORDER);
        dBoxCur.setLabel("Current");
        dBoxCur.setPrecision(6);
        
        //plot of temperature and density histories
//		History densityHistory = densityMeter.getHistory();
//		History temperatureHistory = thermometer.getHistory();
//		densityHistory.setLabel("Density ("+dadUnit.symbol()+")");
//		temperatureHistory.setLabel("Temperature ("+tUnit.symbol()+")");
/*		DisplayPlot plotD = new DisplayPlot(this);
		DisplayPlot plotT = new DisplayPlot(this);
		plotD.setDataSources(densityHistory);
        plotD.setUnit(dUnit);
		plotT.setDataSources(temperatureHistory);
		plotT.setUnit(tUnit);
		plotD.setLabel("Density");
		plotT.setLabel("Temperature");
		plotT.getPlot().setYRange(0.0,1500.);
*/		
		//display of averages
/*		DisplayTable table = new DisplayTable(this);
		table.setUpdateInterval(20);
		table.setWhichValues(new MeterAbstract.ValueType[] {
		                MeterAbstract.CURRENT, MeterAbstract.AVERAGE, MeterAbstract.ERROR});
        this.mediator().addMediatorPair(new Mediator.DisplayMeter.NoAction(this.mediator()));
*/				
		
		//pressure device
//        sliderModulator.setFunction(pressureRescale);
        pressureSlider = new DeviceSlider(null);
        pressureSlider.setShowValues(true);
        pressureSlider.setEditValues(true);
        pressureSlider.setUnit(pUnit);
        pressureSlider.setMinimum(00);
        pressureSlider.setMaximum(1000);
	    pressureSlider.getSlider().setMajorTickSpacing(200);
	    pressureSlider.getSlider().setMinorTickSpacing(50);
	    pressureSlider.getSlider().setLabelTable(
	        pressureSlider.getSlider().createStandardLabels(200,100));
	    pressureSlider.getSlider().setValue(p0);
        
        //set-pressure history
//        etomica.MeterScalar pressureSetting = new MeterDatumSourceWrapper(pressureSlider.getModulator());
//        pressureSetting.setHistorying(true);
//        History pHistory = pressureSetting.getHistory();
//        pHistory.setLabel("Set Pressure");
/*        DisplayPlot plotP = new DisplayPlot(this);
        plotP.setDataSources(pHistory);
        plotP.setUnit(pUnit);
        plotP.setLabel("Pressure"); 
        plotP.getPlot().setYRange(0.0, 1500.);
*/        
        //measured pressure on piston
        //wrap it in a MeterDatumSource wrapper because we want to average pressure
        //over a longer interval than is used by other meters.  By wrapping it
        //we can still have history synchronized with others
//        Atom piston = ((SpeciesAgent)pistonCylinder.getAgent(phase)).node.firstLeafAtom();
//        piston.coord.setMass(pistonMass);
//        etomica.MeterScalar pressureMeter = ((AtomType.Wall)piston.type).new MeterPressure(this);
//        pressureMeter.setUpdateInterval(10);
//        pressureMeter.setFunction(pressureScale);
//        etomica.MeterDatumSourceWrapper pressureMeterWrapper = new MeterDatumSourceWrapper(pressureMeter);
////        pressureMeterWrapper.setFunction(pressureRescale);
//        pressureMeterWrapper.setWhichValue(MeterAbstract.MOST_RECENT);
//        pressureMeterWrapper.setHistorying(true);
//        History pMeterHistory = pressureMeterWrapper.getHistory();
//        pMeterHistory.setLabel("Pressure ("+pUnit.symbol()+")");
        
//        Configuration pcConfig = (Configuration)((SpeciesAgent)pistonCylinder.getAgent(phase)).type.creator().getConfiguration();
//        pcConfig.initialPosition = piston.coord.position().x(1);
       
//        DisplayPlot plot = new DisplayPlot(this);
//        plot.setDataSources(new DataSource[] {
//                densityHistory, temperatureHistory, pMeterHistory, pHistory});
//        plot.setLabel("History");
//        plot.getPlot().setYLabel("");
//        plot.getPlot().setYRange(0.0, 1500.);
//        plot.setYUnit(new Unit[] {dadUnit, tUnit, pUnit, pUnit});
        
        fixPistonButton = new DeviceToggleButton(null);


        //************* Lay out components ****************//
        
        panel = new JPanel();
        panel.setLayout(new java.awt.BorderLayout());      

        //tabbed pane for the big displays
    	final javax.swing.JTabbedPane displayPanel = new javax.swing.JTabbedPane();
    	displayPhasePanel = new javax.swing.JPanel(new java.awt.BorderLayout());
//    	displayPhasePanel.add(scaleSlider.getSlider(),java.awt.BorderLayout.EAST);
    	displayPhasePanel.add(scaleSliderPanel,java.awt.BorderLayout.EAST);
    	displayPanel.add(displayPhase.getLabel(), displayPhasePanel);
//        displayPanel.add(displayPhase.getLabel(),displayPhase.graphic(null));
//        displayPanel.add("Averages", table.graphic(null));
//        displayPanel.add(plotD.getLabel(), plotD.graphic());
//        displayPanel.add(plotT.getLabel(), plotT.graphic());
//        displayPanel.add(plotP.getLabel(), plotP.graphic());
//        displayPanel.add(plot.getLabel(), plot.graphic());
        
/*        JPanel plotPanel = new JPanel(new java.awt.GridLayout(0,1));
        plotPanel.add(plotD.graphic());
        plotPanel.add(plotT.graphic());
        plotPanel.add(plotP.graphic());
        displayPanel.add("All",new javax.swing.JScrollPane(plotPanel));
 */       //workaround for JTabbedPane bug in JDK 1.2
        displayPanel.addChangeListener(
            new javax.swing.event.ChangeListener() {
                public void stateChanged(javax.swing.event.ChangeEvent event) {
                    displayPanel.invalidate();
                    displayPanel.validate();
                }
        });
        
        //panel for the start buttons
//        JPanel startPanel = new JPanel(new java.awt.FlowLayout());
//        DeviceTrioControllerButton control = new DeviceTrioControllerButton(this, controller1);
//        JPanel startPanel = (JPanel)control.graphic();
        JPanel startPanel = (JPanel)controlButtons.graphic();
        java.awt.GridBagConstraints gbc0 = new java.awt.GridBagConstraints();
        startPanel.setBorder(new javax.swing.border.TitledBorder("Control"));
        gbc0.gridx = 0; gbc0.gridy = 0;
        gbc0.gridx = 0; gbc0.gridy = 2; gbc0.gridwidth = 2;
        startPanel.add(fixPistonButton.graphic(null), gbc0);

        //panel for the temperature control/display
        JPanel temperaturePanel = new JPanel(new java.awt.GridBagLayout());
        temperaturePanel.setBorder(new javax.swing.border.TitledBorder("Temperature (K)"));
        java.awt.GridBagConstraints gbc1 = new java.awt.GridBagConstraints();
        gbc1.gridx = 0;  gbc1.gridy = 1;
        gbc1.gridwidth = 1;
        temperaturePanel.add(tSelect.graphic(null),gbc1);
        gbc1.gridx = 1;  gbc1.gridy = 1;
        temperaturePanel.add(tBox.graphic(null),gbc1);
        
        //panel for the density displays
        JPanel densityPanel = new JPanel(new java.awt.FlowLayout());
        densityPanel.setBorder(new javax.swing.border.TitledBorder("Density (mol/l)"));
        densityPanel.add(dBoxCur.graphic(null));
        densityPanel.add(dBoxAvg.graphic(null));
        
        //panel for pressure slider
        JPanel sliderPanel = new JPanel(new java.awt.GridLayout(0,1));
        pressureSlider.setShowBorder(false);
        sliderPanel.add(pressureSlider.graphic(null));
        sliderPanel.setBorder(new javax.swing.border.TitledBorder("Set pressure ("+pUnit.toString()+")"));
        

        //panel for all the controls
        JPanel dimensionPanel = new JPanel(new GridLayout(1,0));
        ButtonGroup dimensionGroup = new ButtonGroup();
        final JRadioButton button2D = new JRadioButton("2D");
        JRadioButton button3D = new JRadioButton("3D (not yet)");
        button2D.setSelected(true);
        dimensionGroup.add(button2D);
        dimensionGroup.add(button3D);
        dimensionPanel.add(button2D);
        dimensionPanel.add(button3D);
        button2D.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent evt) {
               if(button2D.isSelected()) {
                   setSimulation(new PistonCylinder(2));
               } else {
                   setSimulation(new PistonCylinder(2));//not ready for 3d yet
               }
           }
        });
        
//        JPanel controlPanel = new JPanel(new java.awt.GridLayout(0,1));
        JPanel controlPanel = new JPanel(new java.awt.GridBagLayout());
        java.awt.GridBagConstraints gbc2 = new java.awt.GridBagConstraints();
        gbc2.gridx = 0;
        gbc2.gridy = java.awt.GridBagConstraints.RELATIVE;
        controlPanel.add(dimensionPanel);
        controlPanel.add(startPanel,gbc2);
        java.awt.GridBagConstraints gbc3 = new java.awt.GridBagConstraints();
        gbc3.gridx = java.awt.GridBagConstraints.RELATIVE;
        gbc3.gridy = 0;
        controlPanel.add(displayCycles.graphic(),gbc2);
        controlPanel.add(temperaturePanel, gbc2);
        controlPanel.add(densityPanel, gbc2);
        controlPanel.add(sliderPanel, gbc2);
        JPanel potentialPanel = new JPanel();
        potentialPanel.add(potentialChooser);
	    potentialPanel.setBorder(new javax.swing.border.TitledBorder("Potential selection"));
        controlPanel.add(potentialPanel, gbc2);
        
        panel.add(controlPanel, java.awt.BorderLayout.WEST);
        panel.add(displayPanel, java.awt.BorderLayout.EAST);

        setSimulation(new PistonCylinder(2));
        System.out.println(pc.phase.atomCount());
    }
    
    public void setSimulation(PistonCylinder sim) {
        pc = sim;

        BaseUnit.Length.Sim.TO_PIXELS = 800/pc.phase.boundary().dimensions().x(1);
        pc.ai.setDoSleep(true);
        pc.ai.setSleepPeriod(1);

        pc.integrator.setThermostatInterval(1000);
        pc.integrator.setTimeStep(0.5);
        pc.integrator.clearIntervalListeners();

        pc.wallPotential.setLongWall(0,true,true);  // left wall
        pc.wallPotential.setLongWall(0,false,true); // right wall
        // skip top wall
        pc.wallPotential.setLongWall(1,false,false);// bottom wall
        pc.wallPotential.setPhase(pc.phase);  // so it has a boundary
        
        displayPhase.setPhase(pc.phase);
        displayPhase.canvas.setDrawBoundary(DisplayCanvasInterface.DRAW_BOUNDARY_NONE);
        displayPhase.getDrawables().clear();
        displayPhase.addDrawable(pc.pistonPotential);
        displayPhase.addDrawable(pc.wallPotential);
        displayPhasePanel.add(displayPhase.graphic(),java.awt.BorderLayout.WEST);
        pc.integrator.addIntervalListener(displayPhase);
                
        meterCycles = new DataSourceCountSteps(pc.integrator);
        displayCycles.setDataSource(meterCycles);
        pc.integrator.addIntervalListener(displayCycles);
        controlButtons.setSimulation(pc);
        
        tSelect.setController(pc.controller);
        tSelect.setIntegrator(pc.integrator);

        //initialize for ideal gas
        potentialSW = pc.potential;
        potentialHS = new P2HardSphere();
        pc.potentialMaster.setSpecies(potentialHS, new Species[] {pc.species, pc.species});
        pc.potentialMaster.setEnabled(potentialHS, false);
        pc.potentialMaster.setEnabled(potentialSW, false);
        
        if(potentialChooserListener != null) potentialChooser.removeItemListener(potentialChooserListener);
        
        potentialChooserListener = new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                String selected = (String)evt.getItem();
                if(evt.getStateChange() == java.awt.event.ItemEvent.DESELECTED) return;
                //System.out.println("changing potential");
                final boolean HS = selected.equals("Repulsion only"); 
                final boolean SW = selected.equals("Repulsion and attraction"); 
                System.out.println("PistonCylinderGraphic, HS, SW "+HS+" "+SW);
                pc.controller.doActionNow( new Action() {
                    public void actionPerformed() {
                        pc.potentialMaster.setEnabled(potentialHS, HS);
                        pc.potentialMaster.setEnabled(potentialSW, SW);
                        pc.integrator.reset();
                    }
                    public String getLabel() {return "";}
                });
            }
        };
        potentialChooser.addItemListener(potentialChooserListener);
        thermometer.setPhase(new Phase[] {pc.phase});
        densityMeter.setPhase(new Phase[] {pc.phase});
        pc.integrator.addIntervalListener(tBox);
        scaleSlider.setController(pc.controller);
        pc.integrator.addIntervalListener(dBoxAvg);
        pc.integrator.addIntervalListener(dBoxCur);
        ModifierFunctionWrapper sliderModulator = new ModifierFunctionWrapper(pc.pistonPotential, "pressure");
        pressureSlider = new DeviceSlider(pc.controller, sliderModulator);

        ModifierBoolean fixPistonModulator = new ModifierBoolean() {
            public void setBoolean(boolean b) {
                pc.pistonPotential.setStationary(b);
            }
            public boolean getBoolean() {
                return pc.pistonPotential.isStationary();
            }
        };
        fixPistonButton.setController(pc.controller);
        fixPistonButton.setModifier(fixPistonModulator, "Release piston", "Hold piston");
        fixPistonButton.setPostAction(new IntegratorReset(pc.integrator));
        fixPistonButton.setState(true);

    }
    
    
    public static void main(String[] args) {
        PistonCylinderGraphic sim = new PistonCylinderGraphic();
		SimulationGraphic.makeAndDisplayFrame(sim.panel);
//		sim.phase.reset();
 //       sim.controller1.start();
    }//end of main
    
//    public static class Configuration extends ConfigurationSequential {
//        Atom disks, piston;
//        double initialPosition;
//        ConfigurationSequential diskConfiguration;
//        public Configuration(Simulation sim, Atom disks, Atom piston) {
//            super(sim); 
//            this.disks = disks;
//            this.piston = piston;
//            diskConfiguration = new ConfigurationSequential(sim);
//        }
//        public void initializePositions(AtomIterator[] dummy) {
//            piston.node.parentSpecies().moleculeFactory().getConfiguration().initializeCoordinates(piston.node.parentGroup());
//            piston.coord.position().setX(1,initialPosition);
//			piston.coord.setStationary(true);
//			piston.coord.momentum().E(0.0);
//            diskConfiguration.initializeCoordinates(disks);
//        }
//        public void setDimensions(Space.Vector dimensions) {
//            diskConfiguration.setDimensions(dimensions);
//        }
//        public void setDimensions(double[] dimensions) {
//            diskConfiguration.setDimensions(dimensions);
//        }
//    }

    public static class Applet extends javax.swing.JApplet {

	    public void init() {
		    getContentPane().add(new PistonCylinderGraphic().panel);
	    }
    }//end of Applet
}//end of PistonCylinderGraphic class


