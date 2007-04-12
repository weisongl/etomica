package etomica.nbr.site;

import etomica.atom.IAtom;
import etomica.lattice.AbstractLattice;
import etomica.lattice.RectangularLattice;
import etomica.lattice.SiteFactory;

/**
 * Site used to form array of cells for cell-based neighbor listing.  Each
 * cell is capable of holding lists of atoms that are in them.
 */


public class AtomSite {

    public AtomSite(int latticeArrayIndex) {
        this.latticeArrayIndex = latticeArrayIndex;
    }
    
    public IAtom getAtom() {return atom;}
    
    public void setAtom(IAtom atom) {
        this.atom = atom;
    }
    
    public int getLatticeArrayIndex() {
        return latticeArrayIndex;
    }
    
    private IAtom atom;
    final int latticeArrayIndex;//identifies site in lattice

    public static final SiteFactory FACTORY = new SiteFactory() {
        public Object makeSite(AbstractLattice lattice, int[] coord) {
            return new AtomSite(((RectangularLattice)lattice).arrayIndex(coord));
        }
    };
}
