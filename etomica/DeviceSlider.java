//This class includes a main method to demonstrate its use
package etomica;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import etomica.units.*;
import etomica.utility.StringUtility;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Device the changes a property using a graphical slider, via a Modulator.
 *
 * @see Modulator
 */
public class DeviceSlider extends Device implements EtomicaElement {
    
    public String getVersion() {return "DeviceSlider:01.05.29/"+Device.VERSION;}

    /**
     * Descriptive text label to be displayed with the value
     * No longer used -- instead apply label via a title border on the slider itself
     */
    private String label;
    /**
     * Modulator connecting the slider to the property
     */
    protected ModulatorAbstract modulator;
    /**
     * Swing slider displayed to screen
     */
    protected JSlider slider;
    /**
     * Object with property being modulated
     */
    protected Object component;
    /**
     * Property being modulated
     */
    protected String property;
    
    private int minimum, maximum;
    
    public DeviceSlider() {
        this(Simulation.instance);
    }
    public DeviceSlider(Simulation sim) {
        super(sim);
        slider = new JSlider();
        slider.setSize(200,40);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setValue(300);
        setMinimum(100);
        setMaximum(500);
        slider.setMajorTickSpacing(100);
//        slider.setMinorTickSpacing(50);
        slider.addChangeListener(new SliderListener());  //SliderListener is an inner class defined below
        
/*        label.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0) {//right-click
                    Device editor = new DeviceUnitEditor(DeviceSlider.this);
                 //   editor.graphic(null).setLocation(evt.getX(), evt.getY());
                 //   panel.getParent().add(editor.graphic(null));
                    parentSimulation().add(editor.graphic(null));
                    parentSimulation().validate();
                    parentSimulation().repaint();
                }
            }
        });*/
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo();
        info.setDescription("Slider-type device for changing a property");
        return info;
    }

    /**
     * Override superclass setUnit method to update label when unit is changed
     */
    public void setUnit(Unit u) {
        super.setUnit(u);
        setLabelDefault();
    }
    
    /**
     * Constructs a slider connected to the given property of the given object
     */
    public DeviceSlider(Object object, String property) {
        this(new Modulator(object, property));
        component = object;
        this.property = property;
    }
    /**
     * Constructs a slider connected to the get/set Value methods of the given Modulator
     */
    public DeviceSlider(ModulatorAbstract m) {
        this();
        //set component and property in some way
        setModulator(m);
    }
    
    public final void setModulator(ModulatorAbstract m) {
        modulator = m;
        unit = modulator.getDimension().defaultIOUnit();
        setLabelDefault();
        slider.setValue((int)unit.fromSim(modulator.getValue()));
        setMinimum(getMinimum());
        setMaximum(getMaximum());
    }
    public final ModulatorAbstract getModulator() {return modulator;}
    
    public String getProperty() {return property;}
    public void setProperty(String s) {
        property = s;
        if(component != null) setModulator(new Modulator(component, property));
    }
    
    public Object getComponent() {return component;}
    public void setComponent(Object obj) {
        component = obj;
        if(property != null) setModulator(new Modulator(component,property));
    }
    
    public void setMinimum(int min) {
        minimum = min;
        slider.setMinimum(min);
        slider.setInverted(maximum < minimum);
        setTicks();
    }
    public int getMinimum() {return minimum;}
    
    public void setMaximum(int max) {
        maximum = max;
        slider.setMaximum(max);
        slider.setInverted(maximum < minimum);
        setTicks();
    }
    public int getMaximum() {return maximum;}
    
    private void setTicks() {
        int spacing = (getMaximum()-getMinimum())/4;
        if(spacing <= 0) return;
        slider.setMajorTickSpacing(spacing);
        slider.setMinorTickSpacing(slider.getMajorTickSpacing()/2);
        //need to do the following because JSlider does not automatically
        //reset labels if they have been set before
        slider.setLabelTable(slider.createStandardLabels(spacing));
    }
    
    /**
     * Returns the GUI element for display in the simulation.
     */
    public java.awt.Component graphic(Object obj) {
        return slider;
    }
    
    
    /**
     * Sets the value of a descriptive label using the meter's label and the unit's symbol (abbreviation).
     */
    private void setLabelDefault() {
        String suffix = (unit.symbol().length() > 0) ? " ("+unit.symbol()+")" : "";
        if(modulator != null) 
            setLabel(StringUtility.capitalize(modulator.getLabel())+suffix);
    }

    /**
     * Sets the value of a descriptive label using the given string.
     */
    public void setLabel(String text) {
        label = text;
        slider.setBorder(new javax.swing.border.TitledBorder(text));
    }
    /**
     * @return the current instance of the descriptive label.
     */
    public String getLabel() {return label;}
    
    /**
     * @return a handle to the JSlider instance used by this slider device
     */
    public JSlider getSlider() {return slider;}
    
    /**
     * Performs no action (slider can not be accessed for setting).
     * Method exists so that slider and its properties (which can be set) will
     * show up on the property sheet.
     */
     public void setSlider(JSlider s) {}
    
    /**
     * Slider listener, which relays the slider change events to the modulator
     */
    private class SliderListener implements ChangeListener {
        public void stateChanged(ChangeEvent evt) {
            if(modulator!=null) modulator.setValue(unit.toSim(slider.getValue()));
       }
    }
    
    /**
     * Method to demonstrate and test the use of this class.  
     * Slider is used to control the temperature of a hard-disk MD simulation
     */
    public static void main(String[] args) {
        Frame f = new Frame();   //create a window
        f.setSize(600,350);
        
        etomica.simulations.HSMD2D sim = new etomica.simulations.HSMD2D();
        Simulation.instance = sim;
        
        //here's the part unique to this class
        Integrator integrator = sim.integrator;
        DeviceSlider mySlider = new DeviceSlider(integrator,"temperature");
        mySlider.setUnit(new etomica.units.Unit(etomica.units.Kelvin.UNIT));
        integrator.setIsothermal(true);
        //end of unique part
 
        Simulation.instance.elementCoordinator.go();
        f.add(Simulation.instance);         //access the static instance of the simulation to
                                            //display the graphical components
        f.pack();
        f.show();
        f.addWindowListener(new WindowAdapter() {   //anonymous class to handle window closing
            public void windowClosing(WindowEvent e) {System.exit(0);}
        });
    }
}