package etomica.data.meter;

import etomica.Data;
import etomica.DataInfo;
import etomica.DataSource;
import etomica.data.DataDouble;
import etomica.utility.NameMaker;

/**
 * Meter for recording and averaging a simple scalar of type double.
 */
 
public abstract class DataSourceScalar implements DataSource {
    
    public DataSourceScalar(DataInfo dataInfo) {
        data = new DataDouble(dataInfo);
        setName(NameMaker.makeName(this.getClass()));
    }
    
    public DataInfo getDataInfo() {
        return data.getDataInfo();
    }
    
    /**
     * Returns a single scalar value as the measurement for the given phase.
     * Subclasses define this method to specify the measurement they make.
     */
	public abstract double getDataAsScalar();
	
    /**
     * Causes the single getDataAsScalar(Phase) value to be computed and
     * returned for the given phase. In response to a getData() call,
     * MeterAbstract superclass will loop over all phases previously specified
     * via setPhase and collect these values into a vector and return them in
     * response to a getData() call.
     */
	public final Data getData() {
		data.x = getDataAsScalar();
		return data;
	}
	
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
	public final DataDouble data;
    private String name;
}
