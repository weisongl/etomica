package etomica;
import etomica.units.*;

/**
 * A meter to compute the velocity component of the pressure tensor. 
 * Averages a tensor quantity formed from a dyad of the velocity of each atom. 
 * Specifically, the quantity averaged is 1/N * sum(pp/m), where p is the momentum,
 * m is the mass, and the sum is over all N atoms.
 * 
 * @author Rob Riggleman
 */

public class MeterTensorVelocity extends MeterTensor /*implements MeterTensor.Atomic*/ {
    /**
     * Iterator of atoms.
     */
    private final AtomIteratorPhaseDependent ai1 = new AtomIteratorLeafAtoms();
    /**
     * Tensor used to form velocity dyad for each atom, and returned by currentValue(atom) method.
     */
    private Space.Tensor velocity;
    /**
     * Tensor used to sum contributions to velocity dyad, and returned by currentValue() method.
     */
    private Space.Tensor velocityTensor;
    
    public MeterTensorVelocity(Space space) {
        super(space);
        velocity = space.makeTensor();
        velocityTensor = space.makeTensor();
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Velocity tensor, formed from averaging dyad of velocity vector for each atom");
        return info;
    }
       
    /**
     * Returns the dimension of the measured value, here given as energy
     */
    public Dimension getDimension() {return Dimension.ENERGY;}
    
    /**
     * Descriptive label
     *
     * @return "pp/m"
     */
    public String getLabel() {return "pp/m";}
    
    /**
     * Returns the velocity dyad (mass*vv) summed over all atoms, and divided by N
     */
    public Space.Tensor getDataAsTensor(Phase phase) {
        ai1.setPhase(phase);
        ai1.reset();
        velocityTensor.E(0.0);
        int count = 0;
        while(ai1.hasNext()) {
            Atom a = ai1.nextAtom();
            velocity.E(a.coord.momentum(), a.coord.momentum());
            velocity.TE(a.coord.rm());
            velocityTensor.PE(velocity);
            count++;
        }
        velocityTensor.TE(1.0/(double)count);
        return velocityTensor;
    }
    
    /**
     * Returns the velocity dyad (mass*vv) for the given atom.
     */
//    public Space.Tensor currentValue(Atom atom) {
//        velocity.E(atom.coord.momentum(), atom.coord.momentum());
//        velocity.TE(atom.coord.rm());
//        return velocity;
//    }
}