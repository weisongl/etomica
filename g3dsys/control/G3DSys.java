package g3dsys.control;

import g3dsys.images.*;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jmol.g3d.Graphics3D;

/**
 * Abstraction and delegation container class to facilitate communication
 * between the CoordMapper, FigureManager, and G3DPanel without deeply
 * coupling them in their own definitions.
 * 
 * Users need only instantiate G3DSys with the target AWT component and then
 * start adding Figures for them to appear. No Figure constructors should be
 * called directly, as the addFig method will do that itself based on the
 * figure type argument (e.g.; BALL, LINE, BOX).
 */

public class G3DSys {

  /** implemented shapes */
  public static final int BALL = 0;
  public static final int CYLINDER = 1;
  public static final int RECTANGLE = 2;
  public static final int LINE = 3;
  public static final int BOX = 4;
  public static final int AXES = 5;
  public static final int TIMEDBALL = 6;

  // classes needing communication amongst themselves
  // coupled in function not in code
  private CoordMapper cm;
  private FigureManager fm;
  private Display dm;

  private java.awt.Container parent; //awt object in which system is contained
  private Graphics3D g3d; //instance of G3D

  public G3DSys(java.awt.Container window) {
    //init infrastructure
    parent = window;
    dm = new Display(this);

    fm = new FigureManager(this);
    cm = new CoordMapper(this);
    dm.setSize(parent.getSize());

    //init g3d
    g3d = new Graphics3D(dm);
    g3d.setWindowSize(window.getWidth()-10, window.getHeight()-60, false);
    g3d.setBackgroundArgb(0xFF000000);
    g3d.setSlabAndDepthValues(0, Short.MAX_VALUE);

    //enable scaling resize
    parent.addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {}
      public void componentMoved(ComponentEvent e) { fastRefresh(); }
      public void componentShown(ComponentEvent e) {}
      public void componentResized(ComponentEvent e) { refresh(); }});

    //add keyboard controls
    dm.addKeyListener(new KeyboardControl(this));
    //add mouse controls
    new MouseManager(dm,this);
    //dm.addMouseListener(new MouseControl(this));

    parent.add(dm);
    refresh();
    dm.repaint();
    parent.repaint();
  }




  /* ****************************************************************
   * G3D-related methods
   * ****************************************************************/

  /** For a thorough redraw of the display */
  public void refresh() {
    dm.setSize(parent.getSize());
    dm.setLocation(0,0);
    g3d.setWindowSize(parent.getWidth(), parent.getHeight(), false);
    cm.recalcPPA();
    dm.repaint();
  }

  /** A less thorough redraw for a quick refresh */
  public void fastRefresh() {
    //dm.paint(dm.getGraphics());
    dm.repaint();
  }

  /**Change the background color of the G3D display
   * @param color the G3d background color to use
   */
  public void setBGColor(int color) { g3d.setBackgroundArgb(color); }

  /** Get G3D
   *  @return associated Graphics3D object */
  public Graphics3D getG3D() { return g3d; }




  /* ****************************************************************
   * Dimension accessors for calculation pixel per Angstrom ratio
   * ****************************************************************/
  /** Gets the pixel width of the display
   *  @return width of the g3d display area in pixels */
  public int getPixelWidth() { return parent.getWidth(); }
  /** Gets the pixel height of the display
   *  @return height of the g3d display area in pixels */
  public int getPixelHeight() { return parent.getHeight(); }
  /** Gets the pixel depth of the display
   *  @return depth of the g3d display area in pixels */
  public int getPixelDepth() { return cm.angToPixel(fm.getDepth()); }
  /** Gets the Angstrom width of the model
   *  @return width of the model in Angstroms */
  public float getAngstromWidth() { return fm.getWidth(); }
  /** Gets the Angstrom height of the model
   *  @return height of the model in Angstroms */
  public float getAngstromHeight() { return fm.getHeight(); }
  /** Gets the Angstrom depth of the model
   *  @return depth of the model in Angstroms */
  public float getAngstromDepth() { return fm.getDepth(); }




  /* ****************************************************************
   * Methods for modifying the model
   * ****************************************************************/
  /**Adds a shape to the associated FigureManager at the given molspace
   * coordinate. All positional values are in Angstroms.
   * @param TYPE the kind of shape to add
   * @param color the color of the shape
   * @param p molspace position
   * @param d molspace diameter
   * @return the tracking ID of the Figure that was added; -1 if no addition
   */
  public long addFigNoRescale(int TYPE, java.awt.Color color, Point3f p, float d) {
    return addFigNoRescale(TYPE, color, p.x, p.y, p.z, d);
  }
  public long addFig(int TYPE, java.awt.Color color, Point3f p, float d) {
    return addFig(TYPE, color, p.x, p.y, p.z, d);
  }
  public long addFig(int TYPE, java.awt.Color color, float x, float y, float z, float d) {
    short c = Graphics3D.getColix(color2argb(color));
    switch(TYPE) {
    case BALL:
      return fm.addFig(new Ball(this,c,x,y,z,d));
    case TIMEDBALL:
      return fm.addFig(new TimedBall(this,c,x,y,z,d));
    case BOX:
      return fm.addFig(new Box(this, c));
    case AXES:
      return fm.addFig(new Axes(this, c));
    default:
      System.out.println("unknown shape type");
    return -1;
    }
  }
  public long addFigNoRescale(int TYPE, java.awt.Color color, float x, float y, float z, float d) {
    short c = Graphics3D.getColix(color2argb(color));
    switch(TYPE) {
    case BALL:
      return fm.addFigNoRescale(new Ball(this,c,x,y,z,d));
    case TIMEDBALL:
      return fm.addFig(new TimedBall(this,c,x,y,z,d));
    case BOX:
      return fm.addFigNoRescale(new Box(this,c));
    case AXES:
      return fm.addFigNoRescale(new Axes(this,c));
    default:
      System.out.println("unknown shape type");
    return -1;
    }
  }

  /**
   * Removes the Figure that has the given ID but does not rescale
   * Good for batch removals, but shrinkModel must be called manually at the
   * end.
   * @param id the id of the Figure to remove
   * @return the Figure that was removed (or null)
   */
  public Figure removeFigNoRescale(long id) {
    return fm.removeFigNoRescale(id);
  }

  /**
   * Removes the Figure that has the given ID and automatically rescales
   * @param id the ID of the Figure to remove
   * @return the Figure that was removed (or null)
   */
  public Figure removeFig(long id) {
    return fm.removeFig(id);
  }

  /**
   * Gets the collection figures as represented by ID.
   * Iterators are not safe here since the model could be modified while
   * the user has an open iterator.
   * @return an array of longs of Figure IDs
   */
  public long[] getFigs() {
    return fm.getFigs();
  }

  /**
   * Modifies the Figure represented by id, and assigns the given
   * color and locations.
   * @param id the ID of the Figure to modify
   * @param color the color to apply
   * @param x the Angstrom x location to apply
   * @param y the Angstrom y location to apply
   * @param z the Angstrom z location to apply
   * @param d the Angstrom diameter to apply
   */
  public void modFig(long id, java.awt.Color color, float x, float y, float z, float d, boolean drawable) {
    short c = Graphics3D.getColix(color2argb(color));
    Figure f = fm.getFig(id);
    if(f != null) {
      f.setColor(c);
      f.setX(x);
      f.setY(y);
      f.setZ(z);
      f.setD(d);
      f.setDrawable(drawable);
    }
  }
  
  public void modFigXYZ(long id, float x, float y, float z) {
    Figure f = fm.getFig(id);
    if(f != null) {
      f.setX(x);
      f.setY(y);
      f.setZ(z);
    }
  }
  
  public void modFigColor(long id, java.awt.Color color) {
    Figure f = fm.getFig(id);
    if(f != null) f.setColor(Graphics3D.getColix(color2argb(color)));
  }
  
  public void modFigDrawable(long id, boolean drawable) {
    Figure f = fm.getFig(id);
    if(f != null) f.setDrawable(drawable);
  }

  /* ****************************************************************
   * Rotation and translation delegation
   * ****************************************************************/
  private boolean flipYAxis = true;
  private boolean flipXAxis = false;
  /**Rotate the display around the x axis
   * @param degrees the number of degrees to rotate
   */
  public void rotateByX(float degrees) {
    cm.rotateByX( (flipXAxis ? -1 : 1) * degrees);
  }

  /**Rotate the display around the y axis
   * @param degrees the number of degrees to rotate
   */
  public void rotateByY(float degrees) {
    cm.rotateByY( (flipYAxis ? -1 : 1) * degrees);
  }

  /**Rotate the display around the z axis
   * @param degrees the number of degrees to rotate
   */
  public void rotateByZ(float degrees) { cm.rotateByZ(degrees);	}

  /** Remove all rotation (and translation) and return to the home position */
  public void rotateToHome() { cm.rotateToHome(); }
  /** Translate model 20% of pixel width to the left (-x) */
  public void xlateLeft() { cm.xlateX(-1*getPixelWidth()/5); }
  /** Translate model 20% of pixel width to the right (+x) */
  public void xlateRight() { cm.xlateX(getPixelWidth()/5); }
  /** Translate model 20% of pixel width up (-y) */
  public void xlateUp() { cm.xlateY(-1*getPixelHeight()/5); }
  /** Translate model 20% of pixel width down (+y) */
  public void xlateDown() { cm.xlateY(getPixelHeight()/5); }
  /** Translate model horizontally x pixels */
  public void xlateX(int x) { cm.xlateX(x); }
  /** Translate model vertically y pixels */
  public void xlateY(int y) { cm.xlateY(y); }
  /** Set the molecule space location around which rotation occurs */
  public void setCenterOfRotation(Point3f p) { cm.setCenterOfRotation(p); }
  public Point3f getCenterOfRotation() { return cm.getCenterOfRotation(); }




  /* ****************************************************************
   * CoordMapper delegation
   * ****************************************************************/
  /**
   * Converts molspace point p (in Angstroms) to a point on the display
   * in pixels.
   * @param p the point to convert
   * @return the pixel coordinate
   */
  public Point3i screenSpace(Point3f p) { return cm.screenSpace(p); }
  /**
   * Handles perspective based on depth and diameter.
   * @param z depth of an object
   * @param d diameter of an object
   * @return the new perspective-scaled diameter for the object
   */
  public int perspective(int z, float d) { return cm.perspective(z,d); }
  /**
   * Recalculates the pixel per Angstrom ratio
   */
  public void recalcPPA() { cm.recalcPPA(); }
  /**
   * Increase the zoom level by i percent
   * @param i the percentage amount to increase 
   */
  public void zoomUp(int i) { cm.zoomUp(i); }
  /**
   * Decrease the zoom level by i percent
   * @param i the percentage amount to decrease
   */
  public void zoomDown(int i) { cm.zoomDown(i); }




  /* ****************************************************************
   * FigureManager delegation
   * ****************************************************************/
  public float getMinX() { return fm.getMinX(); }
  public float getMinY() { return fm.getMinY(); }
  public float getMinZ() { return fm.getMinZ(); }
  public float getMaxX() { return fm.getMaxX(); }
  public float getMaxY() { return fm.getMaxY(); }
  public float getMaxZ() { return fm.getMaxZ(); }
  public void draw() { fm.draw();}


  /**
   * Converts the given AWT color to an argb int
   * @param color the color to be converted
   * @returns an argb int
   **/
  public static int color2argb(java.awt.Color color) {
    float[] compArray = color.getComponents(null);
    int a = (int)(compArray[3]*255+0.5);
    int r = (int)(compArray[0]*255+0.5);
    int g = (int)(compArray[1]*255+0.5);
    int b = (int)(compArray[2]*255+0.5);
    int argb = (a << 24) | (r << 16) | (g << 8) | b;
    return argb;
  }

}