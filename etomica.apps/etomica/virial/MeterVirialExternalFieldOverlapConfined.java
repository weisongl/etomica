package etomica.virial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import etomica.api.IAtomList;
import etomica.data.DataTag;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.graph.model.Graph;
import etomica.graph.property.HasSimpleArticulationPoint;
import etomica.graph.traversal.BCVisitor;
import etomica.graph.traversal.Biconnected;
import etomica.units.Null;
import etomica.util.DoubleRange;
import etomica.virial.cluster.ExternalVirialDiagrams;

/**
 * Measures value of clusters in a box and returns the values
 * divided by the sampling bias from the sampling cluster.
 */
public class MeterVirialExternalFieldOverlapConfined implements ClusterWeightSumWall.DataSourceClusterWall, java.io.Serializable {

    /**
	 * Constructor for MeterVirial.
	 */
	public MeterVirialExternalFieldOverlapConfined(ExternalVirialDiagrams diagrams, MayerFunction f, double [] walldistance) {
		Set<Graph> gset = diagrams.getMSMCGraphsEX(true);
		clusters = new ArrayList<ClusterAbstract>(gset.size());
		listsPoint = new ArrayList<List<List<Byte>>>(gset.size());
		comparator = new RangeComparator();
	    		
		for(Graph g : gset){

			ArrayList<ClusterBonds> allBonds = new ArrayList<ClusterBonds>();
			ArrayList<Double> weights = new ArrayList<Double>();
			diagrams.populateEFBonds(g, allBonds, weights, false);  
            double [] w = new double[]{weights.get(0)};            

            clusters.add(new ClusterSum(allBonds.toArray(new ClusterBonds[0]), w, new MayerFunction[]{f}));
            ArrayList<List<Byte>> listComponent = new ArrayList<List<Byte>>();
            ArrayList<Byte> listPointG = new ArrayList<Byte>();
            listComponent.add(listPointG);
            List<List<Byte>> biComponents = new ArrayList<List<Byte>>();
            BCVisitor v = new BCVisitor(biComponents);
            new Biconnected().traverseAll(g, v);
            HasSimpleArticulationPoint hap = new HasSimpleArticulationPoint();
            hap.check(g);
            
            List<List<Byte>> prvLayerList = new ArrayList<List<Byte>>();
            List<Byte> prvApList = new ArrayList<Byte>();
            prvApList.add((byte)0);
            while (!prvApList.isEmpty()){
            	List<List<Byte>> layerList = new ArrayList<List<Byte>>();
	            List<Byte> apList = new ArrayList<Byte>();
		        for(byte prvAP : prvApList){
		            
		            for(List<Byte> biComponent : biComponents){
		            	if(!biComponent.contains(prvAP) || prvLayerList.contains(biComponent)){
		            		continue;
		            	}
		        		boolean isterminal = true;
		        		
		        		for(byte b : biComponent){
		        			if(b == prvAP){
		        				continue;
		        			}
		        			if(hap.getArticulationPoints().contains(b)){
		        				isterminal = false;
		        				apList.add(b);
		        				layerList.add(biComponent);
		        					
		        			}
		        			
		        		}
		        		List<Byte> listAll = new ArrayList<Byte>();
		        		listAll.addAll(biComponent);
		        		listAll.remove((Byte)prvAP);
		        		if(isterminal){
		        			listComponent.add(listAll);
		        		}
		        		else {
		        			listPointG.addAll(listAll);
		    			}
		            }
		        }
		        prvApList = apList;
		        prvLayerList = layerList;
            }
            listsPoint.add(listComponent);
            
		}
		
		for (ClusterAbstract cluster : clusters){
			cluster.setTemperature(1);
		}
		
		this.wallDistance = walldistance;
        data = new DataDoubleArray(walldistance.length + 2);
        dataInfo = new DataInfoDoubleArray("Cluster Value",Null.DIMENSION, new int[]{walldistance.length + 2});
        tag = new DataTag();
        dataInfo.addTag(tag);
	}

	public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }
    
    public DataTag getTag() {
        return tag;
    }
    
    public IData getData() {
    	
        double x[] = data.getData();
        IAtomList atoms = box.getLeafList();
        for(int i=0; i<x.length; i++){
        	x[i]=0;
        }                    
            
    	for(int c=0; c<clusters.size();c++){
    		double v =clusters.get(c).value(box);        			
    		if (v==0){
    	    	continue;
    	    }
    		      
    	    List<List<Byte>> cList = listsPoint.get(c);
    	    List<Byte> gList = cList.get(0);
    	    
    	    int nPoints = clusters.get(0).pointCount();
    	    double glowestatom = 0.0;
    	    double ghighestatom = 0.0;     	   	    
    	       
    	    DoubleRange [] gm1lowhigh = new DoubleRange [cList.size()-1];
    	    	    	
    	    for(byte g : gList){
    	    	if (atoms.getAtom(g).getPosition().getX(2) < glowestatom){
    	    		glowestatom = atoms.getAtom(g).getPosition().getX(2);    	    			
    	    	}    	 
    	    	else if (atoms.getAtom(g).getPosition().getX(2) > ghighestatom){
    	    		ghighestatom = atoms.getAtom(g).getPosition().getX(2);
    	    	}
    	    }    	  
    	    
    	    for(int j=1; j<cList.size(); j++){
    	    	List<Byte> gm1List = cList.get(j); 
    	    	double gm1low0 = nPoints - 0.5;
    	    	double gm1high0 = 0.5 - nPoints; 
    	    	for(byte gm1 :gm1List){    	    		
    	    		if (atoms.getAtom(gm1).getPosition().getX(2) <  gm1low0){
    	    			gm1low0 = atoms.getAtom(gm1).getPosition().getX(2);    	    			
    	    		}
    	    		if (atoms.getAtom(gm1).getPosition().getX(2) > gm1high0){
    	    			gm1high0 = atoms.getAtom(gm1).getPosition().getX(2);    	    				    			
    	    		}    	    		
    	    	}     	    	
    	    	v=-v;
    	    	gm1lowhigh[j-1] = new DoubleRange(gm1low0, gm1high0);     	    	
    	    }  	   
    	    x[0]+=v; 
    	    
    	    Arrays.sort(gm1lowhigh, comparator);    	    
    	    
    	   
    	    	    
    	 
    	    for (int j=0; j<wallDistance.length; j++){
    	    	double l1 = 0;
        	    double l2 = 0;    	   
        	    double l = 0;  
    	    	double high = ghighestatom - wallDistance[j] + 1; 
    	    	
    	    	for(int i=0; i<cList.size()-1; i++){	    		
    	    		
    	    		/*if (gm1lowhigh[0].maximum() < ghighestatom){
    	    			l0 = 1;
    	    			break;    	    			
    	    		}*/
    	    		if (gm1lowhigh[i].maximum() - wallDistance[j] +1 < high){
    	    			if (gm1lowhigh[i].minimum() > high){
    	    				high = gm1lowhigh[i].minimum();
    	    				if (high > glowestatom){
        	    				high = glowestatom;
        	    				break;
        	    			}
    	    			}     	    			
    	    		}
    	    		else {    	    			
    	    			l2 += gm1lowhigh[i].maximum() - wallDistance[j]+1- high;
    	    			high = gm1lowhigh[i].minimum();
    	    			if (high > glowestatom){
    	    				high = glowestatom;
    	    				break;
    	    			}
    	    		}    	    		
    	    	}   	    		    	    	
    	    	l1 = glowestatom - high;
    	    	    	    	
    	    	l = l1 + l2;    	    	    	    	
    	    	
    	    	
    	    	x[x.length-1] +=Math.abs(l * v);  
    	    	     	     	    	
        	    x[j+1] += (l * v);         	   
    	    }
    	    	    	       	    
      	}   
        return data; 
        
    }
    
   
    public BoxCluster getBox() {
        return box;
    }
    
    public void setBox(BoxCluster newBox) {
        box = newBox;
    }
    protected final List<List<List<Byte>>> listsPoint;
    protected final List<ClusterAbstract> clusters;
	private final DataDoubleArray data;
	private final IEtomicaDataInfo dataInfo;
    private final DataTag tag;
    protected final RangeComparator comparator;
    private final double [] wallDistance;
    private BoxCluster box;
    private static final long serialVersionUID = 1L;
    
    protected static class RangeComparator implements Comparator<DoubleRange>{

		@Override
		public int compare(DoubleRange o1, DoubleRange o2) {
			if(o1.maximum() < o2.maximum()){
			// TODO Auto-generated method stub
			return -1;
			}
			if(o1.maximum() > o2.maximum()){
				return +1;
			}
			return 0;
		}
    	
    }
}