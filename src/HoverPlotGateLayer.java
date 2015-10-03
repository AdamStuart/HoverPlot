

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class HoverPlotGateLayer
{
	/**
	 * This class adds a pseudo-layer on top of a given XY chart.
	 *
	 * It adds mouse handlers to the chart and the canvas where dots are drawn,
	 * creates a selection rectangle, and puts information into a label field
	 */

	private static final String INFO_LABEL_ID = "zoomInfoLabel";
	private Pane pane;
	private XYChart<Number, Number> chart;
	private NumberAxis xAxis;
	private NumberAxis yAxis;
	private Rectangle selectionRectangle;
	private Label infoLabel;
	private HoverPlotController controller;
	private Point2D selectionRectangleStart = null;
	private Point2D selectionRectangleEnd = null;
	private static final String STYLE_CLASS_SELECTION_BOX = "chart-selection-rectangle";

	/**
	 * Create a new instance of this class with the given chart and pane
	 * instances. The {@link Pane} instance is needed as a parent for the
	 * rectangle that represents the user selection.
	 * 
	 * @param chart
	 *            the xy chart to which the selection support should be added
	 * @param pane
	 *            the pane on which the selection rectangle will be drawn.
	 */
	public HoverPlotGateLayer(XYChart<Number, Number> premadeChart, HoverPlotController inController, Pane containerPane)
	{
		pane = containerPane;
		chart = premadeChart;
		xAxis = (NumberAxis) chart.getXAxis();
		yAxis = (NumberAxis) chart.getYAxis();
		makeSelectionRectangle();
		pane.getChildren().add(selectionRectangle);
		addDragSelectionMechanism();
		addInfoLabel();
		controller = inController;
//		selectionRectangle.setVisible(true); // DEBUG
	}

	private void makeSelectionRectangle()
	{
		selectionRectangle = new Rectangle(200, 120, 140, 140); // SelectionRectangle();
		selectionRectangle.setManaged(false);
		// selectionRectangle.setFill(null); // this loses mouse clicks
		selectionRectangle.setOpacity(0.1);
		selectionRectangle.getStyleClass().addAll(STYLE_CLASS_SELECTION_BOX);

		selectionRectangle.setStroke(Color.GREEN);
		selectionRectangle.setStrokeWidth(2f);
		selectionRectangle.addEventHandler(MouseEvent.MOUSE_PRESSED, new SelectionMousePressedHandler());
		selectionRectangle.addEventHandler(MouseEvent.MOUSE_DRAGGED, new SelectionMouseDraggedHandler());
		selectionRectangle.addEventHandler(MouseEvent.MOUSE_RELEASED, new SelectionMouseReleasedHandler());
	}
	
	public Rectangle getSelectionRectangle() { return selectionRectangle; }
	public void setSelectionRectangle(double x, double y, double w, double h) 
	{  
		RectangleUtil.setRect(selectionRectangle, x,y,w,h);
	}
	public Point2D getSelectionStart() { return selectionRectangleStart;		}
	public Point2D getSelectionEnd() { return selectionRectangleEnd;		}


	private boolean isRectangleSizeTooSmall()
	{
		double width = Math.abs(selectionRectangleEnd.getX() - selectionRectangleStart.getX());
		double height = Math.abs(selectionRectangleEnd.getY() - selectionRectangleStart.getY());
		return width < 10 || height < 10;
	}

	/**
	 * The info label is used here to show the gate coordinates and frequency
	 */
	private void addInfoLabel()
	{
		infoLabel = new Label("");
		infoLabel.setId(INFO_LABEL_ID);
		pane.getChildren().add(infoLabel);
		StackPane.setAlignment(infoLabel, Pos.TOP_RIGHT);
		infoLabel.setVisible(false);
	}

	/**
	 * Adds a mechanism to select an area in the chart that should be displayed
	 * counted.
	 */
	private void addDragSelectionMechanism()
	{
		pane.setOnMousePressed(event -> {
			if (event.isSecondaryButtonDown())	return;
			selectionRectangleStart = computeRectanglePoint(event.getX(), event.getY());
			event.consume();
		});
		pane.setOnMouseDragged(event -> {
			if (event.isSecondaryButtonDown()) return; // do nothing for a right-click
			selectionRectangleEnd = computeRectanglePoint(event.getX(), event.getY());
			RectangleUtil.setRect(selectionRectangle, selectionRectangleStart, selectionRectangleEnd);
			selectionRectangle.setVisible(true);
			event.consume();
		});
		pane.setOnMouseReleased(event ->
		{
			if (selectionRectangleStart == null || selectionRectangleEnd == null)			return;
			if (isRectangleSizeTooSmall())	{		selectionRectangle.setVisible(false);	return;		}
			selectionRectangleStart = selectionRectangleEnd = null;
			setSelection();
			// needed for the key event handler to receive events
			pane.requestFocus();
			event.consume();
		});
		pane.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
			if (KeyCode.ESCAPE.equals(event.getCode()))
			{
				xAxis.setAutoRanging(true);
				yAxis.setAutoRanging(true);
				infoLabel.setVisible(false);
			}
		});
		
		

	}

	private Point2D computeRectanglePoint(double eventX, double eventY)
	{
		double lowerBoundX = computeOffsetInChart(xAxis, false);
		double upperBoundX = lowerBoundX + xAxis.getWidth();
		double lowerBoundY = computeOffsetInChart(yAxis, true);
		double upperBoundY = lowerBoundY + yAxis.getHeight();
		// make sure the rectangle's end point is in the interval defined by the
		// lower and upper bounds for each
		// dimension
		double x = Math.max(lowerBoundX, Math.min(eventX, upperBoundX));
		double y = Math.max(lowerBoundY, Math.min(eventY, upperBoundY));
		return new Point2D(x, y);
	}

	/**
	 * Computes the pixel offset of the given node inside the chart node.
	 * 
	 * @param node
	 *            the node for which to compute the pixel offset
	 * @param vertical
	 *            flag that indicates whether the horizontal or the vertical
	 *            dimension should be taken into account
	 * @return the offset inside the chart node
	 */
	private double computeOffsetInChart(Node node, boolean vertical)
	{
		double offset = 0;
		do
		{
			offset += (vertical) ? node.getLayoutY() : node.getLayoutX();
			node = node.getParent();
		} while (node != chart);
		return offset;
	}

	/**
	 * ------------------------------------------------------------------------
	 * ------- Mouse handlers for clicks inside the selection rectangle
	 */
	private void drawSelectionRectangle(final double x, final double y, final double width, final double height)
	{
		selectionRectangle.setVisible(true);
		selectionRectangle.setX(x);
		selectionRectangle.setY(y);
		selectionRectangle.setWidth(width);
		selectionRectangle.setHeight(height);
	}

	private void drawSelectionRectangleAt(final double x, final double y)
	{
		drawSelectionRectangle(x, y, selectionRectangle.getWidth(), selectionRectangle.getHeight());
	}

	private void disableAutoRanging()
	{
		xAxis.setAutoRanging(false);
		yAxis.setAutoRanging(false);
	}

	private void showInfo()
	{
		infoLabel.setVisible(true);
	}

	double xMin, xMax, yMin, yMax;
	
	private void setSelection()
	{
		disableAutoRanging();
		if (selectionRectangleStart == null || selectionRectangleEnd == null)
		{
			selectionRectangleStart = new Point2D(selectionRectangle.getX(), selectionRectangle.getY() + selectionRectangle.getHeight());
			selectionRectangleEnd = new Point2D(selectionRectangle.getX() + selectionRectangle.getWidth(), selectionRectangle.getY());
		}
		double selectionMinX = Math.min(selectionRectangleStart.getX(), selectionRectangleEnd.getX());
		double selectionMaxX = Math.max(selectionRectangleStart.getX(), selectionRectangleEnd.getX());
		double selectionMinY = Math.min(selectionRectangleStart.getY(), selectionRectangleEnd.getY());
		double selectionMaxY = Math.max(selectionRectangleStart.getY(), selectionRectangleEnd.getY());

		xMin = frameToScaleX(selectionMinX);			// valueForDisplay
		xMax = frameToScaleX(selectionMaxX);
		yMin = frameToScaleY(selectionMaxY);
		yMax = frameToScaleY(selectionMinY);

		List<DataRecord> selected = getRecordsInside(chart, xMin, xMax, yMin, yMax);
		int YFUDGE = 100;
		
		double freq = ((double) selected.size()) / controller.getModel().size();  //getGateFreq(chart, xMin, xMax, yMin, yMax);
		NumberFormat fmt1 = new DecimalFormat("0.00");
		NumberFormat fmt = new DecimalFormat("0");
		String s = fmt1.format(freq * 100) + "% are in the ranges: \n( " + fmt.format(xMin) + " - " + fmt.format(xMax) + "), ("  + fmt.format(yMin + YFUDGE) + " - " + fmt.format(yMax+ YFUDGE) + ")";
		infoLabel.setText(s);
		showInfo();
		
		HBox gallery = controller.getGalleryBox();
		if (gallery != null)
		{
			gallery.getChildren().removeAll(gallery.getChildren());
			List<ImageView> cloneList = FXCollections.observableArrayList();
			for (DataRecord rec : selected)
				cloneList.add(rec.cloneImageView());
			gallery.getChildren().addAll(cloneList);
		}
	}
	
	public void resetSelectionRectangleToSize(NumberAxis xAxis, NumberAxis yAxis, double xOffset, double yOffset)
	{
		double x = (double) xAxis.getDisplayPosition(xMin) + xOffset;
		double y = (double) yAxis.getDisplayPosition(yMax);
		double r = (double) xAxis.getDisplayPosition(xMax) + xOffset;
		double b = (double) yAxis.getDisplayPosition(yMin);
		RectangleUtil.setRect(selectionRectangle,x, y, r-x, b - y ); 
	}
//
//	private double getGateFreq(XYChart<Number, Number> chart, double xMin, double xMax, double yMin, double yMax)
//	{
//		int ct = 0;
//		ObservableList<ZephRec> model = controller.getModel();
//		if (model == null || model.size() <= 0)
//			return 0;
//
//		for (ZephRec rec : model)
//			if (rec.inside(xMin, xMax, yMin, yMax))
//				ct++;
//	
//		return ct / (double) model.size();
//
//	}

	// -------------------------------------------------------------------------------
	private List<DataRecord> getRecordsInside(XYChart<Number, Number> chart, double xMin, double xMax, double yMin, double yMax)
	{
		ObservableList<DataRecord> model = controller.getModel();
		ObservableList<DataRecord> subset = FXCollections.observableArrayList();
		if (model == null || model.size() <= 0)
			return subset;

		for (DataRecord rec : model)
			if (rec.inside(xMin, xMax, yMin, yMax))
				subset.add(rec);
		return subset;
	}

	/**
	 * ------------------------------------------------------------------------
	 */
	boolean resizing = false;

	private final class SelectionMousePressedHandler implements EventHandler<MouseEvent>
	{
		@Override public void handle(final MouseEvent event)
		{

			// do nothing for a right-click
			if (event.isSecondaryButtonDown())
			{
				return;
			}
			if (RectangleUtil.inCorner(event, selectionRectangle))
			{
				resizing = true;
				selectionRectangleStart = RectangleUtil.oppositeCorner(event, selectionRectangle);
			} else
			{
				selectionRectangleStart = new Point2D(event.getX(), event.getY());
				offsetX = event.getX() - selectionRectangle.getX();
				offsetY = event.getY() - selectionRectangle.getY();
			}

			// store position of initial click
			// selectionRectangleStart = computeRectanglePoint(event.getX(),
			// event.getY());
			// System.out.println("SelectionMousePressedHandler");
			event.consume();
		}
	}

	double offsetX = 0, offsetY = 0;

	/**--------------------------------------------------------------------------
	 *
	 */
	private final class SelectionMouseDraggedHandler implements EventHandler<MouseEvent>
	{
		@Override public void handle(final MouseEvent event)
		{
			if (event.isSecondaryButtonDown())	return;		// do nothing for a right-click
			// System.out.println("SelectionMouseDraggedHandler");
			if (resizing)
			{
				// store current cursor position
				selectionRectangleEnd = computeRectanglePoint(event.getX(), event.getY());
				if (selectionRectangleStart == null)
					selectionRectangleStart = new Point2D(event.getX(), event.getY());
				double x = Math.min(selectionRectangleStart.getX(), selectionRectangleEnd.getX());
				double y = Math.min(selectionRectangleStart.getY(), selectionRectangleEnd.getY());
				double width = Math.abs(selectionRectangleStart.getX() - selectionRectangleEnd.getX());
				double height = Math.abs(selectionRectangleStart.getY() - selectionRectangleEnd.getY());
				drawSelectionRectangle(x, y, width, height);
			} else
			{
				double oldX = selectionRectangleStart.getX();
				double oldY = selectionRectangleStart.getY();
				double dx = event.getX() - oldX;
				double dy = event.getY() - oldY;
				offsetRectangle(selectionRectangle, dx, dy);
				drawSelectionRectangleAt(event.getX() - offsetX, event.getY() - offsetY);
				selectionRectangleStart = new Point2D(event.getX(), event.getY());
			}
			event.consume();
		}

		private void offsetRectangle(Rectangle r, double dx, double dy)
		{
//			NumberFormat fmt = new DecimalFormat("0.00");
			// System.out.println("dx:" + fmt.format(dx) + "dx:" +
			// fmt.format(dy));
			selectionRectangle.setX(r.getX() + dx - offsetX);
			selectionRectangle.setY(r.getY() + dy - offsetY);
		}
	}

	/**
	 *
	 */
	private final class SelectionMouseReleasedHandler implements EventHandler<MouseEvent>
	{

		@Override public void handle(final MouseEvent event)
		{
			// System.out.println("SelectionMouseReleasedHandler");
			if (resizing && isRectangleSizeTooSmall())
				return;

			setSelection();
			selectionRectangleStart = selectionRectangleEnd = null;
			resizing = false;
			pane.requestFocus(); // needed for the key event handler to receive
									// events
			event.consume();
		}

	}




	private double frameToScaleX(double value)
	{
		Node chartPlotArea = chart.lookup(".chart-plot-background");
		double chartZeroX = chartPlotArea.getLayoutX();
		double chartWidth = chartPlotArea.getLayoutBounds().getWidth();
		return computeBound(value, chartZeroX, chartWidth, xAxis.getLowerBound(), xAxis.getUpperBound(), false);
	}

	private double frameToScaleY(double value)
	{
		Node chartPlotArea = chart.lookup(".chart-plot-background");
		double chartZeroY = chartPlotArea.getLayoutY();
		double chartHeight = chartPlotArea.getLayoutBounds().getHeight();
		return computeBound(value, chartZeroY, chartHeight, yAxis.getLowerBound(), yAxis.getUpperBound(), true) + 0.5;
	}

	private double computeBound(double pixelPosition, double pixelOffset, double pixelLength, double lowerBound, double upperBound,
					boolean axisInverted)
	{
		double pixelPositionWithoutOffset = pixelPosition - pixelOffset;
		double relativePosition = pixelPositionWithoutOffset / pixelLength;
		double axisLength = upperBound - lowerBound;

		// The screen's y axis grows from top to bottom, whereas the chart's y
		// axis goes from bottom to top.
		// That's why we need to have this distinction here.
		double offset = lowerBound;		// assume !axisInverted
		int sign = 1;
		if (axisInverted)
		{
			offset = upperBound;
			sign = -1;
		}
		double newBound = offset + sign * relativePosition * axisLength;
		return newBound;

	}
}
