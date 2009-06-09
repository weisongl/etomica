package etomica.virial.cluster2.graph;

import java.util.Set;

/**
 * This is a generic interface that allows tagging an object. Tagging is a
 * simple mechanism whereby you dynamically attach string properties to an
 * object.
 * 
 * Use cases. Graphs are tagged by the graph set generators that create them.
 * Each generator defines a set of flags based on how it generates graphs.
 * Additionally, each generator collects the tags of its filter chain, if any,
 * and attaches them to its own set of tags.
 * 
 * @author Demian Lessa
 * 
 * @history
 * 
 */
public interface Tagged {

  public Set<String> getTags();
}
