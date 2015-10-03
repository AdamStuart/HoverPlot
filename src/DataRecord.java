
import java.lang.reflect.Field;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

public class DataRecord {

	private DoubleProperty laneIndex;		// numbering of lanes by position
	private DoubleProperty block;
	private DoubleProperty row;
	private DoubleProperty column;
	private DoubleProperty peakStart;		// characteristics of the measured peak
	private DoubleProperty peakCenter;
	private DoubleProperty peakEnd;
	private DoubleProperty peakHeight;
	private DoubleProperty peakArea;
	private DoubleProperty peakFWHM;
	private DoubleProperty x;				// pixel coordinates in the image
	private DoubleProperty y;
	private Rectangle2D rectangle2D;
	private Rectangle outline;
	private Shape node;
	private Tooltip tip;
	private ImageView imageView;

	static int LANEWIDTH = 40;
	static int LANEHEIGHT= 180;
	
	DataRecord(Object[] rawRow)			// 12 columns from a csv file
	{
		laneIndex = newDoubleProperty(rawRow[0]);
		block = newDoubleProperty(rawRow[1]);
		row = newDoubleProperty(rawRow[2]);
		column  = newDoubleProperty(rawRow[3]);
		peakStart = newDoubleProperty(rawRow[4]);
		peakEnd  = newDoubleProperty(rawRow[5]);
		peakCenter = newDoubleProperty(rawRow[6]);
		peakHeight = newDoubleProperty(rawRow[7]);
		peakFWHM  = newDoubleProperty(rawRow[8]);
		peakArea  = newDoubleProperty(rawRow[9]);
		x  = new SimpleDoubleProperty(Double.parseDouble(rawRow[10].toString())-LANEWIDTH/2);		//	newDoubleProperty(rawRow[10]);
		y  = newDoubleProperty(rawRow[11]);
		node = new Circle(3,3,3);
		node.setId(String.format("%.0f", laneIndex.get()));
		double center = Double.parseDouble(rawRow[6].toString());
		boolean isOutlier = center < 600 || center > 800;
		tip = new Tooltip("");
		Tooltip.install(node, tip);
        tip.setText(toString());
		hackTooltipStartTiming(tip);
		imageView = new ImageView();
		rectangle2D = new Rectangle2D(getX(), getY()-LANEHEIGHT, LANEWIDTH, LANEHEIGHT);
		imageView.setFitWidth(LANEWIDTH);
		imageView.setFitHeight(LANEHEIGHT);
		imageView.setTranslateY(-LANEHEIGHT / 2);
		imageView.setTranslateX(-LANEWIDTH / 2);
		imageView.setRotate(-90);
		imageView.setTranslateY(20-LANEHEIGHT / 2);			// HACK THE OFFSETS TO FIT
		imageView.setTranslateX(40+LANEWIDTH);
		imageView.setViewport(rectangle2D);
		outline = new Rectangle(getX()/10, getY()/10-LANEHEIGHT/10, LANEWIDTH/10, LANEHEIGHT/10);
		outline.setFill(null);
		outline.setStroke(isOutlier ? Color.RED : Color.YELLOW);
		outline.setStrokeWidth(0.5);
        tip.setGraphic(imageView);

	}
	// http://stackoverflow.com/questions/26854301/control-javafx-tooltip-delay/27739605#27739605
	public static void hackTooltipStartTiming(Tooltip tooltip) {
	    try {
	        Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
	        fieldBehavior.setAccessible(true);
	        Object objBehavior = fieldBehavior.get(tooltip);

	        Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
	        fieldTimer.setAccessible(true);
	        Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

	        objTimer.getKeyFrames().clear();
	        objTimer.getKeyFrames().add(new KeyFrame(new Duration(1)));
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}	
	
	private DoubleProperty newDoubleProperty(Object o)
	{
		return  new SimpleDoubleProperty(Double.parseDouble(o.toString()));
	}
	
	public double getLaneIndex()	{ return laneIndex.get();	}
	public double getBlock()		{ return block.get();	}
	public double getRow()			{ return row.get();	}
	public double getColumn()		{ return column.get();	}
	public double getPeakStart()	{ return peakStart.get();	}
	public double getPeakCenter()	{ return peakCenter.get();	}
	public double getPeakEnd()		{ return peakEnd.get();	}
	public double getPeakHeight()	{ return peakHeight.get();	}
	public double getPeakArea()		{ return peakArea.get();	}
	public double getX()			{ return x.get();	}
	public double getY()			{ return y.get();	}
	public double getPeakFWHM()		{ return peakFWHM.get();	}
	public Shape getNode()			{ return node; }
	public Rectangle getOutline()	{ return outline;	}
	public ImageView getImageView()			{ return imageView; }

	public Point2D getXY()  { return new Point2D(getLaneIndex(), getPeakCenter()); }
	
	public void setImage(Image image) {		imageView.setImage(image);	}

	
	public String toString()			//  shows up in the tooltip
	{
		String s = "Lane: %.0f (  %.0f ,  %.0f,  %.0f )\n  start: %.0f center:  %.0f end: %.0f\n  hght: %.2f fwhm:  %.2f\narea: %.2f";
		return String.format(s, laneIndex.get(), block.get(), row.get(), column.get(), peakStart.get(), peakCenter.get(), peakEnd.get(), peakHeight.get(), peakFWHM.get(), peakArea.get());
	}
	public boolean inside(double xMin, double xMax, double yMin, double yMax) {
		
		double x = getLaneIndex(), y = getPeakCenter();
		
//		String s = "Testing if %.0f is in range %.0f - %.0f AND if %.0f is in range %.0f - %.0f ";
//		System.out.println(String.format(s, x, xMin, xMax, y, yMin, yMax));
//		
		if (x < xMin || x >= xMax) return false;
		if (y < yMin || y >= yMax) return false;
		return true;
	}
	
	public ImageView cloneImageView()
	{
		ImageView clone = new ImageView(imageView.getImage());
		clone.setViewport(imageView.getViewport());
		clone.setFitWidth(LANEWIDTH);
		clone.setFitHeight(LANEHEIGHT);
		return clone;
	}
}
