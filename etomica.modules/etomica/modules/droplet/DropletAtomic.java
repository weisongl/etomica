package etomica.modules.droplet;
import etomica.action.BoxInflate;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomList;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomTypeSphere;
import etomica.atom.MoleculeArrayList;
import etomica.box.Box;
import etomica.chem.elements.Argon;
import etomica.config.ConfigurationLattice;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2LennardJones;
import etomica.potential.P2SoftSphericalTruncatedForceShifted;
import etomica.potential.P2SoftSphericalTruncatedShifted;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;

/**
 * Atomic simulation for Droplet module.
 *
 * @author Andrew Schultz
 */
public class DropletAtomic extends Simulation {

    private static final long serialVersionUID = 1L;
    public final SpeciesSpheresMono species;
    public final IBox box;
    public final IntegratorVelocityVerlet integrator;
    public final ActivityIntegrate activityIntegrate;
    public final PotentialMasterList potentialMaster;
    public final P2LennardJones p2LJ;
    public final P2SoftSphericalTruncatedShifted p2LJt;
    public final P1Smash p1Smash;
    protected int nNominalAtoms;
    protected double dropRadius;
    protected double xDropAxis;
    protected double density;
    protected double sigma;

    public DropletAtomic(Space _space) {
        super(_space);
        double pRange = 3;
        sigma = 3.35;
        nNominalAtoms = 32000;
        dropRadius = 0.4;
        xDropAxis = 1;
        density = 0.6;
        
        potentialMaster = new PotentialMasterList(this, sigma*pRange*1.5, space);

        //controller and integrator
	    integrator = new IntegratorVelocityVerlet(this, potentialMaster, space);
	    integrator.setTimeStep(0.005);
	    integrator.setIsothermal(true);
	    integrator.setThermostatInterval(5000);
        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        integrator.setTemperature(Kelvin.UNIT.toSim(118));

	    //species and potentials
	    species = new SpeciesSpheresMono(this, space, Argon.INSTANCE);
        getSpeciesManager().addSpecies(species);
        IAtomTypeLeaf leafType = species.getLeafType();
        ((AtomTypeSphere)leafType).setDiameter(sigma);
        
        p2LJ = new P2LennardJones(space);
        p2LJ.setEpsilon(Kelvin.UNIT.toSim(118));
        p2LJ.setSigma(sigma);
        p2LJt = new P2SoftSphericalTruncatedForceShifted(space, p2LJ, sigma*pRange);
        potentialMaster.addPotential(p2LJt, new IAtomTypeLeaf[]{leafType,leafType});

        p1Smash = new P1Smash(space);
        p1Smash.setG(4);
        potentialMaster.addPotential(p1Smash, new IAtomTypeLeaf[]{leafType});

        //construct box
	    box = new Box(new BoundaryRectangularPeriodic(space), space);
        addBox(box);
        integrator.setBox(box);

        makeDropShape();
        
        integrator.addIntervalAction(potentialMaster.getNeighborManager(box));
    }
    
    public void makeDropShape() {
        box.setNMolecules(species, nNominalAtoms);

        BoxInflate inflater = new BoxInflate(box, space);
        inflater.setTargetDensity(density/(sigma*sigma*sigma));
        inflater.actionPerformed();

        ConfigurationLattice config = new ConfigurationLattice(new LatticeCubicFcc(space), space);
        config.initializeCoordinates(box);
        
        IAtomList leafList = box.getLeafList();
        IVectorMutable v = space.makeVector();
        IVector dim = box.getBoundary().getDimensions();
        System.out.println(dim.x(0));
        double dropRadiusSq = 0.25*dropRadius*dropRadius*dim.x(0)*dim.x(0);
        int ambientCount = 0;
        MoleculeArrayList outerMolecules = new MoleculeArrayList();
        for (int i=0; i<leafList.getAtomCount(); i++) {
            v.E(((IAtomPositioned)leafList.getAtom(i)).getPosition());
            v.setX(0, v.x(0)/xDropAxis);
            if (v.squared() > dropRadiusSq) {
                ambientCount++;
                if (ambientCount == 20) {
                    ambientCount = 0;
                }
                else {
                    outerMolecules.add(leafList.getAtom(i).getParentGroup());
                }
            }
        }
        for (int i=0; i<outerMolecules.getMoleculeCount(); i++) {
            box.removeMolecule(outerMolecules.getMolecule(i));
        }
        System.out.println(outerMolecules.getMoleculeCount());
    }
    
    
    public static void main(String[] args) {
        Space space = Space3D.getInstance();
            
        DropletAtomic sim = new DropletAtomic(space);
        sim.getController().actionPerformed();
    }//end of main
}