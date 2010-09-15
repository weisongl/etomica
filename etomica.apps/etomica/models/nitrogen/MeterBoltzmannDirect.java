package etomica.models.nitrogen;

import etomica.data.DataSourceScalar;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.integrator.IntegratorBox;
import etomica.units.Null;

/**
 * Meter used for overlap sampling in the target-sampled system.  The meter
 * measures the ratio of the Boltzmann factors for the reference and target
 * potentials.
 * 
 * @author Tai Boon Tan
 */
public class MeterBoltzmannDirect extends DataSourceScalar {
	
	public MeterBoltzmannDirect(IntegratorBox integrator, MeterPotentialEnergy meterPotentialEnergy) {
    	super("Scaled Unit",Null.DIMENSION);
         
    	meterEnergy = new MeterPotentialEnergyFromIntegrator(integrator);
        this.integrator = integrator;
        this.meterPotentialEnergy = meterPotentialEnergy;
    }
    
	public double getDataAsScalar() {
		double uSampled = meterEnergy.getDataAsScalar();
    	double uMeasured = meterPotentialEnergy.getDataAsScalar();
    	
    	//System.out.println(uSampled + " " + uMeasured);
    	return Math.exp(-(uMeasured - uSampled) / integrator.getTemperature());
	}
	
	private static final long serialVersionUID = 1L;
    protected final MeterPotentialEnergyFromIntegrator meterEnergy;
    protected final MeterPotentialEnergy meterPotentialEnergy;
    protected final IntegratorBox integrator;

}
