package etomica;

/** 
 * Class used to define an action on an AtomPair.
 * Can be passed to allPairs method of an AtomPair iterator to perform the action on 
 * all pairs generated by the iterator
 */
public abstract class AtomPairAction implements java.io.Serializable {
    public abstract void action(AtomPair pair);

/**
 * Wrapper class that makes an AtomPairAction suitable for input to an AtomIterator.
 * This is needed to enable an AtomPair iterator to perform an AtomPairAction on all its pairs.
 * Class is an AtomAction that contains the desired AtomPairAction. The first Atom is set
 *   externally before being fed into an AtomIterator, which iterates values for the
 *   second Atom of the pair; the pair is sent to the wrapped AtomPairAction on each iteration.
 */
    public static final class Wrapper extends AtomAction {
        AtomPairAction pairAction;
        final AtomPair pair;
        public Wrapper(AtomPair p) {pair = p;}
        public void actionPerformed(Atom a) {
            pair.atom2 = a;
            pair.reset();
            pairAction.action(pair);
//            System.out.println(pair.atom1.signature()+"   "+pair.atom2.signature());
        }
        public void actionPerformed() {
            pair.reset();
            pairAction.action(pair);
        }
    }//end of Wrapper
    
    public static final class OuterWrapper extends AtomAction {
        public final Wrapper innerWrapper;
        public final IteratorDirective directive;
        final AtomPair pair;
        public AtomIterator aiInner;
        public OuterWrapper(AtomPair p, IteratorDirective d) {
            innerWrapper = new Wrapper(p);
            directive = d;
            pair = p;
        }
        public void actionPerformed(Atom a) {
            pair.atom1 = a;
            aiInner.reset(directive.set(a));
            aiInner.allAtoms(innerWrapper);
        }
    }//end of OuterWrapper
            
    
}//end of Action
