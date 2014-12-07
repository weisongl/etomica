package etomica.normalmode;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISpecies;
import etomica.api.IVectorMutable;
import etomica.data.DataSourceScalar;
import etomica.space.ISpace;
import etomica.units.Angle;

/**
 * Meter that measures the average tilt angle (not the angle of average tilt!)
 *
 * @author Andrew Schultz
 */
public class MeterPhiDeviation extends DataSourceScalar {

    public MeterPhiDeviation(ISpace space) {
        super("phi deviation", Angle.DIMENSION);
        dr = space.makeVector();
    }
    
    public void setBox(IBox newBox) {
        box = newBox;
    }

    public double getDataAsScalar() {
        IMoleculeList molecules = box.getMoleculeList();
        int nMolecules = molecules.getMoleculeCount();
        double sum = 0;
        for (int i=0; i<nMolecules; i++) {
            IMolecule molecule = molecules.getMolecule(i);
            IAtomList atomList = molecule.getChildList();
            int leafCount = atomList.getAtomCount();
            dr.E(atomList.getAtom(leafCount-1).getPosition());
            dr.ME(atomList.getAtom(0).getPosition());
            dr.normalize();
            double phi = Math.atan2(dr.getX(1), dr.getX(0));
            double sintheta = Math.sqrt(dr.getX(0)*dr.getX(0) + dr.getX(1)*dr.getX(1));
            double u = phi*sintheta;
            sum += u*u;
        }
        return Math.sqrt(sum/nMolecules);
    }

    private static final long serialVersionUID = 1L;
    protected IBox box;
    protected final IVectorMutable dr;
}
