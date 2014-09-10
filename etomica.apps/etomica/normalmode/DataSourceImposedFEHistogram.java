package etomica.normalmode;

import etomica.data.DataSourceIndependent;
import etomica.data.DataTag;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataFunction;
import etomica.data.types.DataFunction.DataInfoFunction;
import etomica.integrator.mcmove.MCMoveInsertDeleteBiased;
import etomica.integrator.mcmove.MCMoveOverlapListener;
import etomica.units.Null;
import etomica.units.Quantity;

/**
 * This is a strange creature... it returns the histogram that would exist if
 * 1) the applied bias was not in effect
 * 2) the applied bias would yield a flat histogram
 * 
 * P(N) = exp(bias(N) + N*mu)
 * 
 * The output is most useful to compare against the output from
 * DataSourceFEHistogram.  With the bias applied, the simulation should spend
 * more time where this histogram is lower (than DataSourceFEHistogram). 
 * 
 * @author Andrew Schultz
 */
public class DataSourceImposedFEHistogram implements IEtomicaDataSource, DataSourceIndependent {

    protected final MCMoveOverlapListener mcMoveOverlapMeter;
    protected DataFunction data;
    protected DataInfoFunction dataInfo;
    protected DataDoubleArray xData;
    protected DataInfoDoubleArray xDataInfo;
    protected final DataTag tag, xTag;
    protected double mu;
    protected final MCMoveInsertDeleteBiased mcMoveID;
    
    public DataSourceImposedFEHistogram(MCMoveOverlapListener mcMoveOverlapMeter, MCMoveInsertDeleteBiased mcMoveID, double mu) {
        tag = new DataTag();
        xTag = new DataTag();
        this.mcMoveOverlapMeter = mcMoveOverlapMeter;
        this.mcMoveID = mcMoveID;
        xDataInfo = new DataInfoDoubleArray("bar", Null.DIMENSION, new int[]{0});
        data = new DataFunction(new int[]{0});
        dataInfo = new DataInfoFunction("foo", Null.DIMENSION, this);
        this.mu = mu;
    }
    
    public void setMu(double newMu) {
        mu = newMu;
    }

    public IData getData() {
        double[] ratios = mcMoveOverlapMeter.getRatios();
        if (ratios == null) return data;
        if (ratios.length != dataInfo.getLength()-1) {
            getDataInfo();
        }
        double p = 1;
        double tot = 0;
        int n0 = mcMoveOverlapMeter.getMinNumAtoms();
        for (int i=ratios.length-1; i>=0; i--) {
            tot += p;
            p /= Math.exp(mcMoveID.getLnBias(n0+i) - mcMoveID.getLnBias(n0+i+1))*Math.exp(mu);
        }
        tot += p;
        double[] y = data.getData();
        double p2 = 1;
        for (int i=ratios.length; i>=0; i--) {
            y[i] = p2 == 0 ? Double.NaN : p2/tot;
            if (i==0) break;
            p2 /= Math.exp(mcMoveID.getLnBias(n0+i-1) - mcMoveID.getLnBias(n0+i))*Math.exp(mu);
        }
        return data;
    }

    public DataTag getTag() {
        return tag;
    }

    public IEtomicaDataInfo getDataInfo() {
        double[] ratios = mcMoveOverlapMeter.getRatios();
        if (ratios == null) return dataInfo;
        if (ratios.length != dataInfo.getLength()-1) {
            xDataInfo = new DataInfoDoubleArray("N", Quantity.DIMENSION, new int[]{ratios.length+1});
            xDataInfo.addTag(tag);
            xData = new DataDoubleArray(ratios.length+1);
            double[] x = xData.getData();
            int n0 = mcMoveOverlapMeter.getMinNumAtoms();
            for (int i=0; i<=ratios.length; i++) {
                x[i] = n0+i;
            }
            
            dataInfo = new DataInfoFunction("FE Histogram", Null.DIMENSION, this);
            dataInfo.addTag(tag);
            data = new DataFunction(new int[]{ratios.length+1});
        }
        return dataInfo;
    }

    public DataDoubleArray getIndependentData(int i) {
        return xData;
    }

    public DataInfoDoubleArray getIndependentDataInfo(int i) {
        return xDataInfo;
    }

    public int getIndependentArrayDimension() {
        return 1;
    }

    public DataTag getIndependentTag() {
        return xTag;
    }

}
