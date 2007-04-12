package etomica.atom;

import java.io.Serializable;


/**
 * Defines the index as the Atom's node's index.
 * @author andrew
 */
public class AtomToIndexChild implements AtomToIndex, Serializable {

    /**
     * @throws NullPointerException if the atom is null.
     */
    public int getIndex(IAtom atom) {
        return atom.getIndex();
    }
    
    private static final long serialVersionUID = 1L;

}
