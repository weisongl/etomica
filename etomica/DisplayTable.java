package etomica;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.*;
import javax.swing.Box;
import javax.swing.JScrollPane;
import etomica.units.Unit;

/**
 * Presents data in a tabular form.  Data is obtained from DatumSource objects.
 * Sources may be identified to the Display by passing an array of DatumSource objects (in
 * the setDatumSources method, or one at a time via the addDatumSource method).
 */
public class DisplayTable extends DisplayDatumSources implements EtomicaElement
{
    public String getVersion() {return "DisplayTable:01.05.29/"+super.getVersion();}

    public JTable table;
  //  Box panel;
    javax.swing.JPanel panel;
    
    private boolean showLabels = true;
        
    public DisplayTable() {
        this(Simulation.instance);
    }
    public DisplayTable(Simulation sim)  {
        super(sim);
        setupDisplay();
        setLabel("Data");
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Tabular display of data from several sources");
        return info;
    }
    
    /**
     * Implementation of parent's abstract method to set up table whenever 
     * data sources are added or changed.
     */
    protected void setupDisplay() {
   //     if(panel == null) panel = Box.createVerticalBox();
        if(panel == null) panel = new javax.swing.JPanel(new java.awt.FlowLayout());
        else panel.removeAll();
        panel.setSize(100,150);
        if(ySource == null || ySource.length == 0) return;
        MyTableData tableSource = new MyTableData();   //inner class, defined below
        table = new JTable(tableSource);
        panel.add(new JScrollPane(table));
        doUpdate();
        
    }
        
    /**
     * Mutator method for whether table should include a column of "x" values.
     */
    public void setShowLabels(boolean b) {
        showLabels = b;
        setupDisplay();
    }
    /**
     * Accessor method for flag indicating if table should include a column of "x" values.
     */
    public boolean isShowLabels() {return showLabels;}
            
    public void repaint() {table.repaint();}

    public Component graphic(Object obj) {return panel;}

    private class MyTableData extends AbstractTableModel {
        
        String[] columnNames;
        Class[] columnClasses;
        int nColumns;
        int y0;  //index of first y column (1 if showing x, 0 otherwise)
        
        MyTableData() {
            nColumns = whichValues.length;
            y0 = showLabels ? 1 : 0;
            nColumns += y0;
            columnNames = new String[nColumns];
            columnClasses = new Class[nColumns];
            if(showLabels) {
                columnNames[0] = "Property";
                columnClasses[0] = String.class;
            }
            for(int i=y0; i<nColumns; i++) {
                columnNames[i] = (whichValues[i-y0]!=null) ? whichValues[i-y0].toString() : "";
                columnClasses[i] = Double.class;
            }
        }
        
            //x-y values are updated in update() method
            //because we don't want to call currentValue
            //or average for each function entry
        public Object getValueAt(int row, int column) {
            if(showLabels && column == 0) return labels[row];
            else return new Double(y[row][column-y0]);
        }
        
        public int getRowCount() {return ySource.length;}
        public int getColumnCount() {return y0 + whichValues.length;}
                
        public String getColumnName(int column) {return columnNames[column];}
        public Class getColumnClass(int column) {return columnClasses[column];}
    }
    
    /**
     * Demonstrates how this class is implemented.
     */
    public static void main(String[] args) {
        javax.swing.JFrame f = new javax.swing.JFrame();   //create a window
        f.setSize(600,350);
        Simulation.makeSimpleSimulation();  //for more general simulations, replace this call with
                                            //construction of the desired pieces of the simulation
        //part that is unique to this demonstration
        Default.BLOCK_SIZE = 20;
        MeterPressureHard pMeter = new MeterPressureHard();
        MeterNMolecules nMeter = new MeterNMolecules();
        Phase phase = Simulation.instance.phase(0);
        DisplayTable table = new DisplayTable();
        //end of unique part
                                            
		Simulation.instance.elementCoordinator.go(); //invoke this method only after all elements are in place
		                                    //calling it a second time has no effect
		                                    
        table.setDatumSources(pMeter);
        table.addDatumSources(nMeter);
        table.setWhichValues(new MeterAbstract.ValueType[] {MeterAbstract.CURRENT, MeterAbstract.AVERAGE});
        

        f.getContentPane().add(Simulation.instance);         //access the static instance of the simulation to
                                            //display the graphical components
        f.pack();
        f.show();
        f.addWindowListener(new java.awt.event.WindowAdapter() {   //anonymous class to handle window closing
            public void windowClosing(java.awt.event.WindowEvent e) {System.exit(0);}
        });
    }

}