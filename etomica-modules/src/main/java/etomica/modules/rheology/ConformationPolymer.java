/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.modules.rheology;

import etomica.api.IAtomList;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.config.IConformation;
import etomica.space.ISpace;

/**
 * Places polymer in a blob near the origin.
 *
 * @author Andrew Schultz
 */
public class ConformationPolymer implements IConformation {
    
    public ConformationPolymer(ISpace space, IRandom random) {
        this.random = random;
        r = space.makeVector();
    }

    public void initializePositions(IAtomList atomList) {
        r.E(0);
        for (int i=0; i<atomList.getAtomCount(); i++) {
            IVectorMutable p = atomList.getAtom(i).getPosition();
            for (int j=0; j<p.getD(); j++) {
                p.setX(j, random.nextGaussian());
            }
            r.PE(p);
        }
        r.TE(-1.0/atomList.getAtomCount());
        for (int i=0; i<atomList.getAtomCount(); i++) {
            atomList.getAtom(i).getPosition().PE(r);
        }
    }

    protected final IRandom random;
    protected final IVectorMutable r;
}
