package etomica.virial;

import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.space.ISpace;
import etomica.space.IVectorRandom;


/**
 * @author kofke
 *
 * Extension of MCMoveAtom that does trial in which several atom positions are
 * perturbed.  However, position of first atom is never altered.  
 */
public class MCMoveClusterMoleculeMulti extends MCMoveMolecule {

    private static final long serialVersionUID = 1L;
    private IVectorRandom[] translationVectors;
    protected int[] constraintMap;

    public MCMoveClusterMoleculeMulti(ISimulation sim, IPotentialMaster potentialMaster,
    		                          ISpace _space) {
    	this(potentialMaster, sim.getRandom(), _space, 1.0);
    }
    
    /**
     * Constructor for MCMoveAtomMulti.
     * @param parentIntegrator
     * @param nAtoms number of atoms to move in a trial.  Number of atoms in
     * box should be at least one greater than this value (greater
     * because first atom is never moved)
     */
    public MCMoveClusterMoleculeMulti(IPotentialMaster potentialMaster,
            IRandom random, ISpace _space, double stepSize) {
        super(potentialMaster, random, _space, stepSize, Double.POSITIVE_INFINITY);
    }

    public void setBox(IBox p) {
        super.setBox(p);
        translationVectors = new IVectorRandom[box.getMoleculeList().getMoleculeCount()];
        for (int i=0; i<box.getMoleculeList().getMoleculeCount(); i++) {
            translationVectors[i] = (IVectorRandom)space.makeVector();
        }
        if (constraintMap == null) {
            constraintMap = new int[box.getMoleculeList().getMoleculeCount()];
            for (int i=0; i<constraintMap.length; i++) {
                constraintMap[i] = i;
            }
        }
    }
    
    public void setConstraintMap(int[] newConstraintMap) {
        constraintMap = newConstraintMap;
    }
    
    //note that total energy is calculated
    public boolean doTrial() {
        uOld = ((BoxCluster)box).getSampleCluster().value((BoxCluster)box);
//        if (uOld == 0) {
//            throw new RuntimeException("oops, initial configuration unhappy");
//        }
        IMoleculeList moleculeList = box.getMoleculeList();
        for(int i=1; i<moleculeList.getMoleculeCount(); i++) {
            int tv = constraintMap[i];
            if (tv == i) {
                translationVectors[tv].setRandomCube(random);
                translationVectors[tv].TE(stepSize);
            }
            groupTranslationVector.E(translationVectors[tv]);
            moveMoleculeAction.actionPerformed(moleculeList.getMolecule(i));
        }
        ((BoxCluster)box).trialNotify();
        uNew = ((BoxCluster)box).getSampleCluster().value((BoxCluster)box);
        return true;
    }
	
    public void rejectNotify() {
        IMoleculeList moleculeList = box.getMoleculeList();
        for(int i=1; i<moleculeList.getMoleculeCount(); i++) {
            groupTranslationVector.Ea1Tv1(-1,translationVectors[constraintMap[i]]);
            moveMoleculeAction.actionPerformed(moleculeList.getMolecule(i));
        }
        ((BoxCluster)box).rejectNotify();
        if (((BoxCluster)box).getSampleCluster().value((BoxCluster)box) == 0) {
            throw new RuntimeException("oops oops, reverted to illegal configuration");
        }
    }

    public void acceptNotify() {
//        if (uNew == 0) {
//            throw new RuntimeException("oops, accepted illegal configuration");
//        }
        ((BoxCluster)box).acceptNotify();
    }
    
    public double getB() {
        return 0.0;
    }
    
    public double getA() {
        return uNew/uOld;
    }
	
}
