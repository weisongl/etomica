package etomica.models.nitrogen;

import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IVectorMutable;
import etomica.potential.PotentialMolecular;
import etomica.space.ISpace;
import etomica.units.Degree;

public class PRotConstraint extends PotentialMolecular{

	/**
	 *  This class is created to constraint the rotation of the molecule
	 *  Make a hard-wall at 90 deg from the molecule's nominal orientation
	 *  
	 *  
	 */
	private static final long serialVersionUID = 1L;
	
	public PRotConstraint(ISpace space, CoordinateDefinitionNitrogen coordinateDefinition, IBox box) {
		super(1, space);
		this.box = box;
		int numMolec = box.getMoleculeList().getMoleculeCount();
		
		System.out.println("Imposed Rotation constraint at 90 deg angle");
		molecOrientation = space.makeVector();
		initMolecOrientation = new IVectorMutable[numMolec][3];
		/*
		 * initializing the initial orientation of the molecule
		 */
		for (int i=0; i<numMolec; i++){
			initMolecOrientation[i] = space.makeVectorArray(3);
			initMolecOrientation[i] = coordinateDefinition.getMoleculeOrientation(box.getMoleculeList().getMolecule(i));
		}
	
		
	}//End of Constructor

	
	public double energy(IMoleculeList molecules) {
						
		IMolecule molecule = molecules.getMolecule(0);
		int index = molecule.getIndex();
				
		IVectorMutable leafPos0 = molecule.getChildList().getAtom(0).getPosition();
		IVectorMutable leaftPos1 = molecule.getChildList().getAtom(1).getPosition();
		
		molecOrientation.Ev1Mv2(leaftPos1, leafPos0);
		molecOrientation.normalize();
		
		double cosangle = molecOrientation.dot(initMolecOrientation[index][0]);
		if(cosangle <= Math.cos(Degree.UNIT.toSim(constraintAngle))){
			
			return Double.POSITIVE_INFINITY;
		}
		
		return 0;
	}


	public double getRange() {
		return 0;
	}


	public void setBox(IBox box) {
		this.box = box;
		
	}
	
	public void setConstraintAngle(double angle){
		this.constraintAngle = angle;
	}

	private IVectorMutable[][] initMolecOrientation;
	private IVectorMutable molecOrientation;
	private IBox box;
	protected double constraintAngle = 90.0; //in degree

	
}