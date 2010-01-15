package etomica.virial.cluster2.mvc.view;

import java.awt.Color;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.svg.SVGDocument;

import etomica.virial.cluster2.graph.ColorMap;
import etomica.virial.cluster2.graph.Edges;
import etomica.virial.cluster2.graph.GraphFactory;
import etomica.virial.cluster2.graph.GraphSet;
import etomica.virial.cluster2.graph.Nodes;

public class SVGClusterMap {

  static int CLUSTERS_ACROSS = 10;
  static int CLUSTERS_ALONG = 1;
  static int CLUSTER_WIDTH = -1;
  static int CLUSTER_HEIGHT = -1;
  static int CLUSTER_SIZE = 6;
  private static final int BORDER = 8;
  private static final int GUTTER = 9;
  private static final int NODE_RADIUS = 10;
  private static final int GRAPH_RADIUS = 40;
  private static final double INITIAL_ANGLE = 1.5 * Math.PI;
  SVGDocument doc;
  List<Rectangle2D> tiles = new ArrayList<Rectangle2D>();

  private SVGClusterMap(GraphSet gs) {

    // Create an SVG document.
    DOMImplementation impl = SVGDOMImplementation.getDOMImplementation();
    String svgNS = SVGDOMImplementation.SVG_NAMESPACE_URI;
    doc = (SVGDocument) impl.createDocument(svgNS, "svg", null);
    SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(doc);
    ctx.setComment("Generated by Etomica Cluster Viewer using Apache Batik.");
    ctx.setEmbeddedFontsOn(true);
    int col = 0;
    int row = 0;
    Iterator<Edges> iter = gs.getEdgesSet().iterator();
    while (iter.hasNext()) {
      Edges e = iter.next();
      int hzGutter = (col == 0 ? GUTTER : 2 * col * GUTTER);
      int vtGutter = (row == 0 ? GUTTER : 2 * row * GUTTER);
      drawCluster(gs.getNodes(), e, BORDER + hzGutter + col * CLUSTER_WIDTH,
          BORDER + vtGutter + row * CLUSTER_HEIGHT);
      col++;
      if (col % CLUSTERS_ACROSS == 0) {
        col = 0;
        row++;
      }
    }
    CLUSTERS_ALONG = gs.getSize() / CLUSTERS_ACROSS
        + (gs.getSize() % CLUSTERS_ACROSS > 0 ? 1 : 0);
    double mapWidth = 2 * BORDER + CLUSTERS_ACROSS * CLUSTER_WIDTH + 2
        * CLUSTERS_ACROSS * GUTTER;
    double mapHeight = 2 * BORDER + CLUSTERS_ALONG * CLUSTER_HEIGHT + 2
        * CLUSTERS_ALONG * GUTTER;
    Element root = doc.getDocumentElement();
    root
        .setAttributeNS(null, "style",
            "fill:white;fill-opacity:1;stroke:black;stroke-width:1.0;stroke-opacity:1");
    root.setAttributeNS(null, "width", String.valueOf(mapWidth));
    root.setAttributeNS(null, "height", String.valueOf(mapHeight));
  }

  public static SVGState getState(GraphSet gs) {

    return new SVGState(new SVGClusterMap(gs));
  }

  /**
   * This method translates a point within the SVG canvas into a tile in the
   * array of tiles which are drawn on the canvas. If the point does not
   * correspond to a tile, the method returns the length of the array of tiles,
   * otherwise it returns the tile to which the point belongs.
   */
  public int getTile(Point p) {

    int row = (p.x - SVGClusterMap.BORDER)
        / (SVGClusterMap.CLUSTER_WIDTH + 2 * SVGClusterMap.GUTTER);
    int col = (p.y - SVGClusterMap.BORDER)
        / (SVGClusterMap.CLUSTER_HEIGHT + 2 * SVGClusterMap.GUTTER);
    if ((row > SVGClusterMap.CLUSTERS_ALONG)
        || (col > SVGClusterMap.CLUSTERS_ACROSS)) {
      return tiles.size();
    }
    return row + col * SVGClusterMap.CLUSTERS_ACROSS;
  }

  private Node drawCluster(Nodes nodes, Edges e, int dX, int dY) {

    int r = NODE_RADIUS;
    float dr = NODE_RADIUS * 0.2f;
    int R = GRAPH_RADIUS;
    int gutter = GUTTER;
    int size = nodes.count();
    double[] x = new double[size];
    double[] y = new double[size];
    double minX = -1, minY = -1, maxX = -1, maxY = -1;
    double angle = 2.0 * Math.PI / size;
    Shape node = new Ellipse2D.Double(0, 0, r, r);
    Shape nodei = new Ellipse2D.Double(0, 0, r - dr, r - dr);
    // create 2D graphics compatible with the SVGCanvas
    SVGGraphics2D g = new SVGGraphics2D(doc);
    // add hints to improve the quality of the images
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING,
        RenderingHints.VALUE_RENDER_QUALITY);
    g.translate(BORDER, BORDER);
    // compute node centers, as well as the minimum and maximum X and Y
    // positions of the cluster nodes; with this information, we define
    // the bounding rectangles around each cluster
    for (int i = 0; i < size; i++) {
      x[i] = (R - r) * Math.cos(INITIAL_ANGLE + i * angle) + (R - r + dX);
      y[i] = (R - r) * Math.sin(INITIAL_ANGLE + i * angle) + (R - r + dY);
      if (minX == -1) {
        minX = x[i];
        maxX = x[i];
        minY = y[i];
        maxY = y[i];
      }
      if (minX > x[i]) {
        minX = x[i];
      }
      if (maxX < x[i]) {
        maxX = x[i];
      }
      if (minY > y[i]) {
        minY = y[i];
      }
      if (maxY < y[i]) {
        maxY = y[i];
      }
    }
    // compute the bounding rectangle of this cluster; gutter spaces are added
    // to top, bottom, left, and right so that the clusters are not too tightly
    // packed on the canvas
    Rectangle2D rect = new Rectangle2D.Double(BORDER + minX - gutter, BORDER
        + minY - gutter, maxX - minX + r + 2 * gutter, maxY - minY + r + 2
        * gutter);
    // the bounding rectangle just computed is a "tile" on the SVGCanvas
    tiles.add(rect);
    // all clusters have the same dimensions, so if this is the first cluster
    // we computed, define the default cluster width and height
    if (CLUSTER_HEIGHT < 0) {
      CLUSTER_HEIGHT = (int) Math.round(rect.getHeight());
      CLUSTER_WIDTH = (int) Math.round(rect.getWidth());
    }
    // cluster edges form the bottom layer, so we draw them first
    for (int i = 0; i < size; i++) {
      for (int j = i + 1; j < size; j++) {
        if (e.hasEdge(i, j)) {
          Point2D pi = new Point2D.Double(x[i] + r / 2, y[i] + r / 2);
          Point2D pj = new Point2D.Double(x[j] + r / 2, y[j] + r / 2);
          g.setPaint(ColorMap.getEdgeColor(e.getAttributes(i, j).getColor()));
          g.draw(new Line2D.Double(pi, pj));
        }
      }
    }
    // nodes form the top layer, so we draw them last
    for (int i = 0; i < size; i++) {
      g.translate(x[i], y[i]);
      // a root node is drawn as a hollow circle, with a thick stroke (20% of
      // the node radius), with the same color as the node
      if (nodes.getAttributes(i).isSameClass(GraphFactory.ROOT_NODE_ATTRIBUTES)) {
        g.setPaint(ColorMap.getNodeColor(nodes.getAttributes(i).getColor()));
        g.fill(node);
        g.translate(dr / 2, dr / 2);
        g.setPaint(Color.WHITE);
        g.fill(nodei);
        g.translate(-dr / 2, -dr / 2);
      }
      // a field node is a solid node with the node color, and a thin black
      // stroke
      else {
        g.setPaint(Color.BLACK);
        g.draw(node);
        g.setPaint(ColorMap.getNodeColor(nodes.getAttributes(i).getColor()));
        g.fill(node);
      }
      g.translate(-x[i], -y[i]);
    }
    // update the document such that this new cluster is added as a node
    // below the root; this way all clusters are independent objects on
    // the SVG document
    Element root = doc.getDocumentElement();
    Node child = g.getTopLevelGroup().getFirstChild();
    return root.appendChild(child);
  }
}