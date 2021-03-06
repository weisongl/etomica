/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.models.nitrogen;

import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IVectorMutable;
import etomica.data.DataSourceScalar;
import etomica.normalmode.CoordinateDefinition.BasisCell;
import etomica.space3d.Space3D;
import etomica.units.Null;

/**
 * Meter used to sample orientation order parameter
 * 
 * @author Tai Boon Tan
 */
public class MeterOrientationOrderParameter extends DataSourceScalar {
	
	public MeterOrientationOrderParameter(CoordinateDefinitionNitrogen coordDef) {
    	super("Scaled Unit",Null.DIMENSION);
         
    	this.coordinateDefinition = coordDef;
    	 
        molAxis = Space3D.makeVector(3);
        initOrient = Space3D.makeVector(3);
    }
    
	public double getDataAsScalar() {
		  BasisCell[] cells = coordinateDefinition.getBasisCells();
	        
	        double sum = 0.0;
	        for (int iCell = 0; iCell<cells.length; iCell++) {
	      
	            BasisCell cell = cells[iCell];
	            IMoleculeList molecules = cell.molecules;
	            numMol = molecules.getMoleculeCount();
	            
	            for (int iMol=0; iMol<numMol; iMol++){
	            	
		          	IMolecule molecule = molecules.getMolecule(iMol);
		          	IVectorMutable leafPos0 = molecule.getChildList().getAtom(0).getPosition();
			    	IVectorMutable leafPos1 = molecule.getChildList().getAtom(1).getPosition();
			
			    	molAxis.Ev1Mv2(leafPos1, leafPos0);
			       	molAxis.normalize();
			       	
			       	initOrient.E(coordinateDefinition.getMoleculeOrientation(molecule)[0]);
			       	double cosT = molAxis.dot(initOrient);
			       	
		            double cosTSq = cosT*cosT;
//		            sum += (2*cosTSq -1)*(2*cosTSq -1)+ 2*cosT*Math.sqrt(1-cosTSq);
		            sum += (3*cosTSq-1);
	            }
	        } 
	        
	        return sum/numMol;
	}
	
	private static final long serialVersionUID = 1L;
	private IVectorMutable initOrient;
	private IVectorMutable molAxis;
	protected CoordinateDefinitionNitrogen coordinateDefinition;
	protected int numMol;

}
