package etomica;
import etomica.units.Dimension;

/**
 * Meter for measurement of the temperature based on kinetic-energy equipartition
 */
 
 /* History of changes
  * 7/03/02 (DAK) Changes to tie in with function of kinetic-energy meter.
  */

public final class MeterTemperature extends MeterScalar implements EtomicaElement, MeterScalar.Atomic {
    
    private final MeterKineticEnergy meterKE;
    
    public MeterTemperature() {
        this(Simulation.instance);
    }
    public MeterTemperature(Simulation sim) {
        super(sim);
        setLabel("Temperature");
        meterKE = new MeterKineticEnergy(sim.space);
        meterKE.setActive(false);
    }
    public MeterTemperature(Space space) {
        super(space);
        setLabel("Temperature");
        meterKE = new MeterKineticEnergy(space);
        meterKE.setActive(false);
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Records temperature as given via kinetic energy");
        return info;
    }
    
    public void setPhase(Phase phase) {
        super.setPhase(phase);
        meterKE.setPhase(phase);
    }
        
    public double currentValue() {
        return (2./(double)(phase.atomCount()*phase.parentSimulation().space().D()))*meterKE.currentValue();
    }
    
    public double currentValue(Atom a) {
        return 2./(phase.atomCount()*phase.parentSimulation().space().D())*a.coord.kineticEnergy();
    }
    
	public Dimension getDimension() {return Dimension.TEMPERATURE;}

/**
 * Class method to compute the temperature of a phase from its total kinetic energy using equipartition
 */
 //commented out because MeterKineticEnergy static method is disabled 
 //iteration of atoms requires contstruction of iterator; not sure if want to do that
//    public static double currentValue(Phase phase) {
//        return (2./(double)(phase.atomCount()*phase.parentSimulation().space().D()))*MeterKineticEnergy.currentValue(phase);
//    }    
}