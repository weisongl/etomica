/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import etomica.api.IAtomType;
import etomica.api.IMolecule;
import etomica.atom.Atom;
import etomica.atom.AtomLeafDynamic;
import etomica.atom.AtomTypeLeaf;
import etomica.atom.Molecule;
import etomica.chem.elements.Carbon;
import etomica.chem.elements.ElementSimple;
import etomica.space.ISpace;
import etomica.species.Species;

/**
 *  
 * Species Anthracene molecule
 * rigid , LJ potential, no charge, this model is very similar to 3site 545 model, only the weight of the sites are different
 * reference: Iwai, monte carlo sim of Naphthalene, phenathlene,anthracene in SCF 1998
 *  * @author shu
 * March, 7, 2011
 *
 */
public class SpeciesAnthracene3site464 extends Species {

    public SpeciesAnthracene3site464(ISpace space) {
        this(space, false);
    }
    
    public SpeciesAnthracene3site464(ISpace space, boolean isDynamic) {
        super();
        this.space = space;
        this.isDynamic = isDynamic;
       
        
        // the virial simulation  doesnt care this , but still I added this info here FYI
        //CMass is the middle site, including 6 carbons and 2 hydrogens
        //CHMass is the side site, including 4 carbons and 4 hydrogens
        double CMass = 6 * 12.0107 + 1 * 2;
        double CHMass = 4 * 12.0107 + 4 * 5;
        chType = new AtomTypeLeaf(new ElementSimple("CH", CHMass));
        cType = new AtomTypeLeaf(new ElementSimple("C", CMass));
               
        //should change because it is not united atom!!!
        addChildType(chType);
        addChildType(cType);

        setConformation(new ConformationAnthracene3site(space)); 
     }

     public IMolecule makeMolecule() {
         Molecule Anthracene = new Molecule(this, 10);
         // 2 Carbon without H, 8 Carbon with H
         Anthracene.addChildAtom(isDynamic ? new AtomLeafDynamic(space, cType) : new Atom(space, cType));
         
         Anthracene.addChildAtom(isDynamic ? new AtomLeafDynamic(space, chType) : new Atom(space, chType));
         Anthracene.addChildAtom(isDynamic ? new AtomLeafDynamic(space, chType) : new Atom(space, chType));
         conformation.initializePositions(Anthracene.getChildList());
         return Anthracene;
     }

     public IAtomType getCType() {
         return cType;
     }

     public IAtomType getCHType() {
         return chType;
     }


     public int getNumLeafAtoms() {
         return 3;
     }
    
    public final static int indexC1 = 0;
    public final static int indexCH1 = 1;
    public final static int indexCH2 = 2;
   
    
    private static final long serialVersionUID = 1L;
    protected final ISpace space;
    protected final boolean isDynamic;
    protected final AtomTypeLeaf cType, chType;
}