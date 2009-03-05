package etomica.models.oneDHardRods;

import etomica.api.IAtomPositioned;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.mcmove.MCMoveBoxStep;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinition.BasisCell;

/**
 * A Monte Carlo move which compares one hard rod normal mode to a harmonic 
 * normal mode and explores the phase space defined by the remaining normal modes.
 * 
 * comparedWV is the wavevector that will be compared.  It cannot be selected
 * by the "change a random mode" section of the doTrial() method
 * 
 * @author cribbin
 *
 */
public class MCMoveCompareM2Right extends MCMoveBoxStep{

    private static final long serialVersionUID = 1L;
    protected CoordinateDefinition coordinateDefinition;
    private final AtomIteratorLeafAtoms iterator;
    protected double[][] uOld;
    protected double[] deltaU;
    protected final IRandom random;
    protected double energyOld, energyNew, energyEvenLater; /*, latticeEnergy*/;
    protected final MeterPotentialEnergy energyMeter;
    private double[][][] eigenVectors;
    private IVectorMutable[] waveVectors;
    int comparedWV;
    private double[] gaussian;
    protected double temperature;
    private double[][] stdDev;
    private double[] rRand, iRand, realT, imagT;
    private double[] waveVectorCoefficients;
    double[] uNow;
    
    
    
//    int count;
    
    public MCMoveCompareM2Right(IPotentialMaster potentialMaster, IRandom random) {
        super(potentialMaster);
        
        this.random = random;
        iterator = new AtomIteratorLeafAtoms();
        energyMeter = new MeterPotentialEnergy(potentialMaster);
        gaussian = new double[2];
//        count = 0;
        comparedWV = 13;
    }


    public boolean doTrial() {
        
        System.out.println("NRGStart " + energyMeter.getDataAsScalar());
        int coordinateDim = coordinateDefinition.getCoordinateDim();
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        rRand = new double[coordinateDim];
        iRand = new double[coordinateDim];
        realT = new double[coordinateDim];
        imagT = new double[coordinateDim];
        
        //nan These lines make it a single atom-per-molecule class.
        BasisCell cell = cells[0];
        uOld = new double[cells.length][coordinateDim];
        double normalization = 1/Math.sqrt(cells.length);

        //Get normal mode coordinate information
        coordinateDefinition.calcT(waveVectors[comparedWV], realT, imagT);
//        System.out.println("Real:  "+ realT[0]);
//        System.out.println("Imag:  "+ imagT[0]);
        
        
//ZERO OUT NORMAL MODES.)
        for(int iCell = 0; iCell < cells.length; iCell++){
            //store old positions.
            uNow = coordinateDefinition.calcU(cells[iCell].molecules);
            System.arraycopy(uNow, 0, uOld[iCell], 0, coordinateDim);
            cell = cells[iCell];
            //rezero deltaU
            for(int j = 0; j < coordinateDim; j++){
                deltaU[j] = 0.0;
//                System.out.println(uNow[j]);
            }
        }
        for(int countWV= comparedWV; countWV <comparedWV+2; countWV++){
            System.out.println("Chunk 1 count " + countWV);
            
            for(int iCell = 0; iCell < cells.length; iCell++){
                
   
                //Calculate the contributions to the current position of the zeroed
                //mode, and subtract it from the overall position
                double kR = waveVectors[countWV].dot(cell.cellPosition);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
                for(int i = 0; i < coordinateDim; i++){
                    //Calculate the current coordinate:
                    double realCoord = 0, imagCoord = 0;
                    for (int j=0; j<coordinateDim; j++) {
                        realCoord += eigenVectors[countWV][i][j] * realT[j];
    //                    System.out.println("realcoord " + realCoord);
                        imagCoord += eigenVectors[countWV][i][j] * imagT[j];
    //                    System.out.println("imagcoord " + imagCoord);
                    }
                    for(int j = 0; j < coordinateDim; j++){
                        deltaU[j] -= waveVectorCoefficients[countWV]*eigenVectors[countWV][i][j] * 2.0 *
                            (realCoord*coskR - imagCoord*sinkR);
    //                    System.out.println("delta 1 " + deltaU[j]);
                    }
                }
                for(int i = 0; i < coordinateDim; i++){
                    deltaU[i] *= normalization;
                }
                
                for(int i = 0; i < coordinateDim; i++) {
                    uNow[i] += deltaU[i];
                    System.out.println("1-unow " + uNow[i]);
                }
                coordinateDefinition.setToU(cells[iCell].molecules, uNow);
                
            }
            
        }
        
        energyOld = energyMeter.getDataAsScalar();
        System.out.println("NRGOld: " + energyOld);
        if(energyOld != 0.0){
//            for(int k = 0; k < waveVectors.length; k++){
//                System.out.println(k + " " +((IAtomPositioned)coordinateDefinition.getBox().getLeafList().getAtom(k)).getPosition());
//            }
            throw new IllegalStateException("Overlap after the removal of a mode!");
        }
        
//MOVE A RANDOM (N-1) MODE, AND MEASURE energyNew
        //equivalent to MCMoveChangeMode
        if(comparedWV != 1) {
            //Select the wave vector whose eigenvectors will be changed.
            //The zero wavevector is center of mass motion, and is rejected as a 
            //possibility, as is the compared wavevector and any wavevector
            //number higher than it.
            int changedWV = random.nextInt(comparedWV-1);
            changedWV += 1;
            
            //calculate the new positions of the atoms.
            //loop over cells
            double delta1 = (2*random.nextDouble()-1) * stepSize;
            double delta2 = (2*random.nextDouble()-1) * stepSize;
//            delta1 = 0.0; delta2 = 0.5;  //nork
            for(int iCell = 0; iCell < cells.length; iCell++){
                uNow = coordinateDefinition.calcU(cells[iCell].molecules);
                cell = cells[iCell];
                //rezero deltaU
                for(int j = 0; j < coordinateDim; j++){
                    deltaU[j] = 0.0;
//                    System.out.println(uNow[j]);
                }
                //loop over the wavevectors, and sum contribution of each to the
                //generalized coordinates.  Change the selected wavevectors eigen
                //-vectors at the same time!
                double kR = waveVectors[changedWV].dot(cell.cellPosition);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
//                System.out.println("icell " +iCell);
//                System.out.println("sinkR " + sinkR);
//                System.out.println("coskR "+  coskR);
                for(int i = 0; i < coordinateDim; i++){
                    for(int j = 0; j < coordinateDim; j++){
                         deltaU[j] += waveVectorCoefficients[changedWV] * 
                             eigenVectors[changedWV][i][j] * 2.0 * (delta1*coskR
                             - delta2*sinkR);
                    }
                }
                for(int i = 0; i < coordinateDim; i++){
                    deltaU[i] *= normalization;
//                      System.out.println(deltaU[i]);
                }
                for(int i = 0; i < coordinateDim; i++) {
                    uNow[i] += deltaU[i];
                    System.out.println("2-unow " + uNow[i]);
                }
                coordinateDefinition.setToU(cells[iCell].molecules, uNow);
            }
        }
        
        energyNew = energyMeter.getDataAsScalar();
        System.out.println("youNou " + energyNew);

//        for(int k = 0; k < 32; k++){
//            System.out.println(k + " " +((IAtomPositioned)coordinateDefinition.getBox().getLeafList().getAtom(k)).getPosition());
//        }
        
        
//MOVE THE NORMAL MODE THAT WAS ZEROED OUT.
        //set up the gaussian values
        double sqrtT = Math.sqrt(temperature);

        for(int countWV = comparedWV; countWV < comparedWV+2; countWV++){
            System.out.println("Chunk 3 count " + countWV);
            for (int j=0; j<coordinateDim; j++) {
                if (stdDev[countWV][j] == 0) continue;
                //generate real and imaginary parts of random normal-mode coordinate Q
                double realGauss = random.nextGaussian() * sqrtT;
                double imagGauss = random.nextGaussian() * sqrtT;
                
    //            realGauss = 0.6;  //nork
    //            imagGauss = 0.3;  //nork
                
                //XXX we know that if c(k) = 0.5, one of the gaussians will be ignored, but
                // it's hard to know which.  So long as we don't put an atom at the origin
                // (which is true for 1D if c(k)=0.5), it's the real part that will be ignored.
                if (waveVectorCoefficients[countWV] == 0.5) imagGauss = 0;
                rRand[j] = realGauss * stdDev[countWV][j];
                iRand[j] = imagGauss * stdDev[countWV][j];
                gaussian[0] = realGauss;
                gaussian[1] = imagGauss;
                
            }

            //calculate the new positions of the atoms.
            for(int iCell = 0; iCell < cells.length; iCell++){
                uNow = coordinateDefinition.calcU(cells[iCell].molecules);
                cell = cells[iCell];
                //rezero deltaU
                for(int j = 0; j < coordinateDim; j++){
                    deltaU[j] = 0.0;
                }
                // Calculate the change in position due to the substitution of a 
                //  Gaussian.
                double kR = waveVectors[countWV].dot(cell.cellPosition);
                double coskR = Math.cos(kR);
                double sinkR = Math.sin(kR);
                for(int i = 0; i < coordinateDim; i++){
                    for(int j = 0; j < coordinateDim; j++){
                        deltaU[j] += waveVectorCoefficients[countWV]*eigenVectors[countWV][i][j] * 2.0 *
                            (rRand[i]*coskR - iRand[i]*sinkR);
                    }
                }
                for(int i = 0; i < coordinateDim; i++){
                    deltaU[i] *= normalization;
                }
                
                for(int i = 0; i < coordinateDim; i++) {
                    uNow[i] += deltaU[i];
                    System.out.println("3-unow " + uNow[i]);
                }
                coordinateDefinition.setToU(cells[iCell].molecules, uNow);
            }
        }
        energyEvenLater = energyMeter.getDataAsScalar();
        System.out.println("NRGevenl8r: " + energyEvenLater);
        
        return true;
    }
    
    public double getA() {
        return 1;
    }

    public double getB() {
        return -(energyNew - energyOld);
    }
    
    public void acceptNotify() {
        System.out.println("  accept old: " +energyOld +" new: "+energyNew +" later " + energyEvenLater);
//        count ++;
    }

    public double energyChange() {
        return energyNew - energyOld;
    }

    public void rejectNotify() {
//        System.out.println("reject " + energyNew);
        // Set all the atoms back to the old values of u
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        for (int iCell = 0; iCell<cells.length; iCell++) {
            BasisCell cell = cells[iCell];
            coordinateDefinition.setToU(cell.molecules, uOld[iCell]);
        }
    }

    public void setBox(IBox newBox) {
        super.setBox(newBox);
        iterator.setBox(newBox);
        energyMeter.setBox(newBox);
    }

    public AtomIterator affectedAtoms() {return iterator;}
    
    public void setCoordinateDefinition(CoordinateDefinition newCD) {
        coordinateDefinition = newCD;
        deltaU = new double[coordinateDefinition.getCoordinateDim()];
        uOld = null;
        realT = new double[coordinateDefinition.getCoordinateDim()];
        imagT = new double[coordinateDefinition.getCoordinateDim()];
    }
    
    public CoordinateDefinition getCoordinateDefinition() {
        return coordinateDefinition;
    }

    /**
     * Set the wave vectors accessible to the move.
     * 
     * @param wv
     */
    public void setWaveVectors(IVectorMutable[] wv){
        waveVectors = new IVectorMutable[wv.length];
        waveVectors = wv;
    }    
    public void setWaveVectorCoefficients(double[] newWaveVectorCoefficients) {
        waveVectorCoefficients = newWaveVectorCoefficients;
    }
    /**
     * Informs the move of the eigenvectors 
     */
    public void setEigenVectors(double[][][] newEigenVectors) {
        eigenVectors = newEigenVectors;
    }
    
    public void setOmegaSquared(double[][] omega2, double[] coeff) {
        stdDev = new double[omega2.length][omega2[0].length];
        for (int i=0; i<stdDev.length; i++) {
            for (int j=0; j<stdDev[i].length; j++) {
                stdDev[i][j] = Math.sqrt(1.0/(2.0*omega2[i][j]*coeff[i]));
            }
        }
    }
    public void setTemperature(double newTemperature) {
        temperature = newTemperature;
    }
    public double[] getGaussian(){
        return gaussian;
    }

    public double[] getLastGaussian(){
        return gaussian;
    }

    public void setComparedWV(int wv){
        comparedWV = wv;
    }
}