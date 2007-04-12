package etomica.action;

import etomica.atom.AtomArrayList;
import etomica.atom.AtomGroup;
import etomica.atom.IAtom;

/**
 * Wraps an AtomAction, and performs the wrapped action on the atom
 * only if it is a leaf atom; if given an atom group (as indicated
 * by the atom's node), performs action instead on all the atom's
 * child atoms.  This process continues recursively until the leaf atoms are
 * encountered.
 *
 * @author David Kofke
 */
public class AtomGroupAction extends AtomActionAdapter {

    /**
     * Constructor takes wrapped action, which is final.
     */
    public AtomGroupAction(AtomAction action) {
        this.action = action;
    }
    /* (non-Javadoc)
     * @see etomica.action.AtomAction#actionPerformed(etomica.Atom)
     */
    public void actionPerformed(IAtom atom) {
        if(atom instanceof AtomGroup) {
            AtomArrayList atomList = ((AtomGroup)atom).getChildList();
            int size = atomList.size();
            for(int i=0; i<size; i++) {
                this.actionPerformed(atomList.get(i));
            }
        }
        else {
            action.actionPerformed(atom);
        }
    }

    /**
     * @return Returns the wrapped action.
     */
    public AtomAction getAction() {
        return action;
    }
    
    private static final long serialVersionUID = 1L;
    private final AtomAction action;
}
