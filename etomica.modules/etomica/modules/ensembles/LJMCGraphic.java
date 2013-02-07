package etomica.modules.ensembles;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import etomica.action.IAction;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorHistory;
import etomica.data.DataFork;
import etomica.data.DataPipe;
import etomica.data.DataProcessor;
import etomica.data.DataPump;
import etomica.data.DataPumpListener;
import etomica.data.DataSourceCountSteps;
import etomica.data.IData;
import etomica.data.IDataSink;
import etomica.data.IEtomicaDataInfo;
import etomica.data.meter.MeterDensity;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.meter.MeterPressure;
import etomica.data.types.DataDouble;
import etomica.data.types.DataTensor;
import etomica.graphics.ActionConfigWindow;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DeviceButton;
import etomica.graphics.DeviceCheckBox;
import etomica.graphics.DeviceSlider;
import etomica.graphics.DeviceThermoSlider;
import etomica.graphics.DisplayPlot;
import etomica.graphics.DisplayTextBoxesCAE;
import etomica.graphics.SimulationGraphic;
import etomica.graphics.SimulationPanel;
import etomica.modifier.ModifierBoolean;
import etomica.space.Space;
import etomica.space2d.Space2D;
import etomica.space3d.Space3D;
import etomica.util.HistoryCollapsingAverage;

public class LJMCGraphic extends SimulationGraphic {

    private final static String APP_NAME = "Lennard-Jones Molecular Dynamics";
    private final static int REPAINT_INTERVAL = 100;
    private DeviceThermoSlider temperatureSelect;
    protected LJMC sim;
    
    private boolean showConfig = false;
    protected boolean volumeChanges = false;
    protected boolean constMu = false;

    public LJMCGraphic(final LJMC simulation, Space _space) {

    	super(simulation, TABBED_PANE, APP_NAME, REPAINT_INTERVAL, _space, simulation.getController());

        ArrayList<DataPump> dataStreamPumps = getController().getDataStreamPumps();
        
    	this.sim = simulation;

       
	    //display of box, timer
        ColorSchemeByType colorScheme = new ColorSchemeByType(sim);
        colorScheme.setColor(sim.species.getLeafType(),java.awt.Color.red);
        getDisplayBox(sim.box).setColorScheme(new ColorSchemeByType(sim));

        DataSourceCountSteps timeCounter = new DataSourceCountSteps(sim.integrator);

		// Number density box
	    MeterDensity densityMeter = new MeterDensity(sim.getSpace());
        densityMeter.setBox(sim.box);
        AccumulatorHistory dHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
        dHistory.setTimeDataSource(timeCounter);
        final AccumulatorAverageCollapsing dAccumulator = new AccumulatorAverageCollapsing();
        dAccumulator.setPushInterval(10);
        DataFork dFork = new DataFork(new IDataSink[]{dHistory, dAccumulator});
        final DataPumpListener dPump = new DataPumpListener(densityMeter, dFork, 100);
        sim.integrator.getEventManager().addListener(dPump);
        dHistory.setPushInterval(1);
        dataStreamPumps.add(dPump);
	    
        DisplayPlot dPlot = new DisplayPlot();
        dHistory.setDataSink(dPlot.getDataSet().makeDataSink());
        dPlot.setDoLegend(false);
        dPlot.setLabel("Density");

        
        MeterPotentialEnergyFromIntegrator peMeter = new MeterPotentialEnergyFromIntegrator(sim.integrator);
        AccumulatorHistory peHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
        peHistory.setTimeDataSource(timeCounter);
        final AccumulatorAverageCollapsing peAccumulator = new AccumulatorAverageCollapsing();
        peAccumulator.setPushInterval(10);
        DataFork peFork = new DataFork(new IDataSink[]{peHistory, peAccumulator});
        final DataPumpListener pePump = new DataPumpListener(peMeter, peFork, 100);
        sim.integrator.getEventManager().addListener(pePump);
        peHistory.setPushInterval(1);
        dataStreamPumps.add(pePump);
		
        DisplayPlot ePlot = new DisplayPlot();
        peHistory.setDataSink(ePlot.getDataSet().makeDataSink());
		ePlot.setDoLegend(false);
		ePlot.setLabel("Energy");
		
        MeterPressure pMeter = new MeterPressure(space);
        pMeter.setIntegrator(sim.integrator);
        pMeter.setBox(sim.box);
        final AccumulatorAverageCollapsing pAccumulator = new AccumulatorAverageCollapsing();
        AccumulatorHistory pHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
        pHistory.setTimeDataSource(timeCounter);
        DataFork pFork = new DataFork(new IDataSink[]{pHistory, pAccumulator});
        final DataPumpListener pPump = new DataPumpListener(pMeter, pFork, 1000);
        sim.integrator.getEventManager().addListener(pPump);
        pAccumulator.setPushInterval(1);
        dataStreamPumps.add(pPump);
        
        DisplayPlot pPlot = new DisplayPlot();
        pHistory.setDataSink(pPlot.getDataSet().makeDataSink());
        pPlot.setDoLegend(false);

        pPlot.setLabel("Pressure");


        final DisplayTextBoxesCAE dDisplay = new DisplayTextBoxesCAE();
        dDisplay.setAccumulator(dAccumulator);
        final DisplayTextBoxesCAE pDisplay = new DisplayTextBoxesCAE();
        pDisplay.setAccumulator(pAccumulator);
        final DisplayTextBoxesCAE peDisplay = new DisplayTextBoxesCAE();
        peDisplay.setAccumulator(peAccumulator);

        //************* Lay out components ****************//

        GridBagConstraints vertGBC = SimulationPanel.getVertGBC();

//        getDisplayBox(sim.box).setScale(0.7);

        //temperature selector
        temperatureSelect = new DeviceThermoSlider(sim.getController(), sim.integrator);
        temperatureSelect.setPrecision(1);
        temperatureSelect.setMinimum(0.0);
        temperatureSelect.setMaximum(10.0);
        temperatureSelect.setSliderMajorValues(4);
	    temperatureSelect.setIsothermalButtonsVisibility(false);

        // show config button
        DeviceButton configButton = new DeviceButton(sim.getController());
        configButton.setLabel("Show Config");
        configButton.setAction(new ActionConfigWindow(sim.box));

        IAction resetAction = new IAction() {
        	public void actionPerformed() {
                // Reset density (Density is set and won't change, but
        		// do this anyway)
        		dPump.actionPerformed();
        		dDisplay.putData(dAccumulator.getData());
                dDisplay.repaint();

                // IS THIS WORKING?
                pPump.actionPerformed();
                pDisplay.putData(pAccumulator.getData());
                pDisplay.repaint();
                pePump.actionPerformed();
                peDisplay.putData(peAccumulator.getData());
                peDisplay.repaint();

        		getDisplayBox(sim.box).graphic().repaint();
        	}
        };

        this.getController().getReinitButton().setPostAction(resetAction);
        this.getController().getResetAveragesButton().setPostAction(resetAction);

        getPanel().controlPanel.add(temperatureSelect.graphic(), vertGBC);

        JPanel pPanel = new JPanel(new GridBagLayout());
        
        final DeviceSlider pSlider = new DeviceSlider(sim.getController(), sim.mcMoveVolume, "pressure");
        pSlider.setEnabled(volumeChanges);
        pSlider.setMaximum(10);
        pSlider.setPrecision(1);
        pSlider.setNMajor(4);
        pSlider.setShowValues(true);
        pSlider.setEditValues(true);
        pSlider.setShowBorder(true);
        pSlider.setLabel("Pressure");
        
        DeviceCheckBox pCheckbox = new DeviceCheckBox("Volume changes", new ModifierBoolean() {
            
            public void setBoolean(boolean b) {
                if (b == volumeChanges) return;
                if (b) {
                    sim.integrator.getMoveManager().addMCMove(sim.mcMoveVolume);
                    pSlider.setEnabled(true);
                }
                else {
                    sim.integrator.getMoveManager().removeMCMove(sim.mcMoveVolume);
                    pSlider.setEnabled(false);
                }
                volumeChanges = b;
            }
            
            public boolean getBoolean() {
                return volumeChanges;
            }
        });
        
        pPanel.add(pCheckbox.graphic(), vertGBC);
        pPanel.add(pSlider.graphic(), vertGBC);
        getPanel().controlPanel.add(pPanel, vertGBC);
        

        JPanel muPanel = new JPanel(new GridBagLayout());
        
        final DeviceSlider muSlider = new DeviceSlider(sim.getController(), sim.mcMoveID, "mu");
        muSlider.setEnabled(constMu);
        muSlider.setMinimum(-10);
        muSlider.setMaximum(10);
        muSlider.setPrecision(1);
        muSlider.setNMajor(4);
        muSlider.setShowValues(true);
        muSlider.setEditValues(true);
        muSlider.setShowBorder(true);
        muSlider.setLabel("Chemical Potential");
        
        DeviceCheckBox muCheckbox = new DeviceCheckBox("Insert/Delete", new ModifierBoolean() {
            
            public void setBoolean(boolean b) {
                if (b == constMu) return;
                if (b) {
                    sim.integrator.getMoveManager().addMCMove(sim.mcMoveID);
                    muSlider.setEnabled(true);
                }
                else {
                    sim.integrator.getMoveManager().removeMCMove(sim.mcMoveID);
                    muSlider.setEnabled(false);
                }
                constMu = b;
            }
            
            public boolean getBoolean() {
                return constMu;
            }
        });
        
        muPanel.add(muCheckbox.graphic(), vertGBC);
        muPanel.add(muSlider.graphic(), vertGBC);
        getPanel().controlPanel.add(muPanel, vertGBC);

        
        if(showConfig == true) {
            add(configButton);
        }

        add(dPlot);
    	add(ePlot);
        add(pPlot);
    	add(dDisplay);
    	add(pDisplay);
    	add(peDisplay);

    }

    public static void main(String[] args) {
        Space sp = null;
        if(args.length != 0) {
            try {
                int D = Integer.parseInt(args[0]);
                if (D == 3) {
                    sp = Space3D.getInstance();
                }
                else {
                	sp = Space2D.getInstance();
                }
            } catch(NumberFormatException e) {}
        }
        else {
        	sp = Space3D.getInstance();
        }

        LJMCGraphic ljmdGraphic = new LJMCGraphic(new LJMC(sp), sp);
		SimulationGraphic.makeAndDisplayFrame
		        (ljmdGraphic.getPanel(), APP_NAME);
    }
    
    public static class Applet extends javax.swing.JApplet {

        public void init() {
	        getRootPane().putClientProperty(
	                        "defeatSystemEventQueueCheck", Boolean.TRUE);
	        Space sp = Space2D.getInstance();
            LJMCGraphic ljmdGraphic = new LJMCGraphic(new LJMC(sp), sp);

		    getContentPane().add(ljmdGraphic.getPanel());
	    }

        private static final long serialVersionUID = 1L;
    }
    
    /**
     * Inner class to find the total pressure of the system from the pressure
     * tensor.
     */
    public static class DataProcessorTensorTrace extends DataProcessor {

        public DataProcessorTensorTrace() {
            data = new DataDouble();
        }
        
        protected IData processData(IData inputData) {
            // take the trace and divide by the dimensionality
            data.x = ((DataTensor)inputData).x.trace()/((DataTensor)inputData).x.D();
            return data;
        }

        protected IEtomicaDataInfo processDataInfo(IEtomicaDataInfo inputDataInfo) {
            dataInfo = new DataDouble.DataInfoDouble(inputDataInfo.getLabel(), inputDataInfo.getDimension());
            return dataInfo;
        }

        public DataPipe getDataCaster(IEtomicaDataInfo inputDataInfo) {
            if (!(inputDataInfo instanceof DataTensor.DataInfoTensor)) {
                throw new IllegalArgumentException("Gotta be a DataInfoTensor");
            }
            return null;
        }

        protected final DataDouble data;
    }

}

