package etomica.species;
import java.lang.reflect.Constructor;

import etomica.EtomicaInfo;
import etomica.atom.AtomFactoryMono;
import etomica.atom.AtomFactoryMonoDynamic;
import etomica.atom.AtomFactoryTree;
import etomica.atom.AtomTypeGroup;
import etomica.atom.AtomTypeSphere;
import etomica.simulation.ISimulation;

/**
 * Species in which molecules are formed as an arbitrarily shaped tree.
 * 
 * @author David Kofke
 */

public class SpeciesTree extends Species {

    /**
     * Constructs with nA = {1}, such that each molecule is a group
     * containing just one atom (which is not the same as SpeciesSpheresMono,
     * for which each molecule is a single atom, not organized under a group).
     */
    public SpeciesTree(ISimulation sim) {
        this(sim, new int[] {1});
    }

    /**
     * Constructor specifing tree structure through array of integers.
     * Each element of array indicates the number of atoms at the corresponding
     * level.  For example, nA = {2,4} will define a species in which each
     * molecule has 2 subgroups, each with 4 atoms (such as ethane, which
     * can be organized as CH3 + CH3)
     */
    //TODO extend to permit specification of Conformation[], perhaps AtomSequencerFactory[]
    public SpeciesTree(ISimulation sim, int[] nA) {
        super(new AtomFactoryTree(sim.getSpace(), null, nA));
        AtomTypeSphere atomType = new AtomTypeSphere(sim);
        //getLeafType will return the an AtomTypeGroup because leaf factory is not yet set
        atomType.setParentType((AtomTypeGroup)((AtomFactoryTree)factory).getLeafType());
        ((AtomFactoryTree)factory).setLeafFactory(sim.isDynamic() ?
                    new AtomFactoryMonoDynamic(sim.getSpace(), atomType) :
                    new AtomFactoryMono(sim.getSpace(), atomType));
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Species with molecules formed as an arbitrarily specified tree");
        return info;
    }
    
    public SpeciesSignature getSpeciesSignature() {
        Constructor constructor = null;
        try {
            constructor = this.getClass().getConstructor(new Class[]{ISimulation.class});
        }
        catch(NoSuchMethodException e) {
            System.err.println("you have no constructor.  be afraid");
        }
        return new SpeciesSignature(constructor,new Object[]{new Integer(1)});
    }
    
    private static final long serialVersionUID = 1L;
}


