
package etomica.paracetamol;

import etomica.action.PDBWriter;
import etomica.data.AccumulatorAverage;
import etomica.data.DataLogger;
import etomica.data.DataPump;
import etomica.data.DataTableWriter;
import etomica.data.AccumulatorAverage.StatType;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.integrator.IntervalActionAdapter;
import etomica.units.Kelvin;

/**
 * 
 * Three-dimensional soft-sphere MC simulation for paracetamol molecule
 * 
 * Orthorhombic Crystal
 * 
 * @author Tai Tan
 *
 */
public class MCParacetamolOrthorhombicforCluster {

    public static void main(String[] args) {
    	
        MCParacetamolOrthorhombic sim = new MCParacetamolOrthorhombic();
        double temperature = sim.integrator.getTemperature();
        
        if (args.length > 0){
    		temperature = Double.parseDouble(args[0]);
    		sim.integrator.setTemperature(Kelvin.UNIT.toSim(temperature));
    	}
 
        sim.actionIntegrate.setMaxSteps(100000000);
        sim.getController().actionPerformed();
        
        /*****************************************************************************/    
        
             MeterPotentialEnergy meterPE = new MeterPotentialEnergy(sim.getPotentialMaster());
             meterPE.setPhase(sim.phase);
             AccumulatorAverage accumulatorAveragePE = new AccumulatorAverage(1); //Average over 1
             DataPump PEpump = new DataPump(meterPE, accumulatorAveragePE);
             
             DataLogger dataLoggerPE = new DataLogger();
             dataLoggerPE.setWriteInterval(1);
             dataLoggerPE.setFileName("Paracetamol_Form_II_@_" + Kelvin.UNIT.fromSim(temperature));
             dataLoggerPE.setAppending(true);
             dataLoggerPE.setDataSink(new DataTableWriter());
             
             sim.getController().getEventManager().addListener(dataLoggerPE);
             accumulatorAveragePE.addDataSink(dataLoggerPE, new StatType[]{StatType.MOST_RECENT});
             accumulatorAveragePE.setPushInterval(1);

             IntervalActionAdapter IAA = new IntervalActionAdapter(PEpump, sim.integrator);
             IAA.setActionInterval(200);
             
             sim.getController().reset();
             sim.getController().actionPerformed();
      /**********************************************************************/   
             
             PDBWriter pdbWriter = new PDBWriter(sim.phase);
             pdbWriter.setFileName("Paracetamol_FormI_T_"+ Kelvin.UNIT.fromSim(temperature) +"K.pdb");
             pdbWriter.actionPerformed();
             
             double Energy = ((DataDouble)((DataGroup)accumulatorAveragePE.getData()).
             		getData(AccumulatorAverage.StatType.AVERAGE.index)).x;
             System.out.println("The potential Energy is "+ Energy );
             
       
        
    }//end of main
    
}//end of class
