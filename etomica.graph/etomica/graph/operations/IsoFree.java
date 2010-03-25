package etomica.graph.operations;

import java.util.HashSet;
import java.util.Set;

import etomica.graph.iterators.IteratorWrapper;
import etomica.graph.iterators.filters.IsomorphismFilter;
import etomica.graph.model.Graph;
import etomica.graph.model.GraphIterator;

public class IsoFree implements Unary {

  public Set<Graph> apply(Set<Graph> argument, Parameters params) {

    PCopy pcopy = new PCopy();
    IteratorWrapper wrapper = new IteratorWrapper(pcopy.apply(argument, null).iterator());
    GraphIterator isomorphs = new IsomorphismFilter(wrapper);
    Set<Graph> result = new HashSet<Graph>();
    while (isomorphs.hasNext()) {
      result.add(isomorphs.next());
    }
    return result;
  }
}