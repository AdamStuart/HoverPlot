

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import com.opencsv.CSVReader;

public class HoverPlotController implements Initializable
{

	private Stage stage;
	public void setStage(Stage primaryStage)	{		stage = primaryStage;	}
	
	@FXML private TableView<DataRecord> table;
	@FXML public TableColumn<DataRecord,Double> lane;
	@FXML public TableColumn<DataRecord,Double> block;
	@FXML public TableColumn<DataRecord,Double> row;
	@FXML public TableColumn<DataRecord,Double> column;
	@FXML public TableColumn<DataRecord,Double> start;
	@FXML public TableColumn<DataRecord,Double> center;
	@FXML public TableColumn<DataRecord,Double> end;
	@FXML public TableColumn<DataRecord,Double> height;
	@FXML public TableColumn<DataRecord,Double> area;
	@FXML public TableColumn<DataRecord,Double> fwhm;
	@FXML public TableColumn<DataRecord,Double> xCol;
	@FXML public TableColumn<DataRecord,Double> yCol;
	@FXML public TableColumn<DataRecord,ImageView> imageColumn;
	@FXML private Label version;
	@FXML private ImageView wholeimage;
	@FXML private StackPane wholeimagestack;
	@FXML private AnchorPane anchorpane;
//	@FXML 
	private HBox lanePictures;
	public HBox getGalleryBox()	{		return lanePictures;	}
	
	public TableColumn<DataRecord, Double>[] dblCols;			// just a way to assert all columns exist
	private ObservableList<DataRecord> model;
	public ObservableList<DataRecord> getModel()		{ return model; 	}
	private HoverPlotGateLayer gateLayer;
	// ------------------------------------------------------
	
	double MAX_X = 7000;
	double MAX_Y = 900;
	ScatterChart<Number, Number> scatter;
	// ------------------------------------------------------
	@Override public void initialize(URL location, ResourceBundle resources)
	{
		assert(table != null);		// injected from FXML
		dblCols = new TableColumn[]{ lane, block, row, column, start, center, end, height, area, fwhm, xCol, yCol};
		String[] names = new String[]{ "laneIndex", "block", "row", "column", "peakStart", "peakCenter", "peakEnd", "peakHeight", "peakArea", "peakFWHM", "x", "y"};
		for (int i=0; i< dblCols.length; i++)	
		{
			TableColumn c = dblCols[i];
			assert(c != null);
			c.setStyle( "-fx-alignment: CENTER-RIGHT;");
			c.setCellFactory(TextFieldTableCell.<DataRecord, Double>forTableColumn(new IntStringConverter()));
			c.setCellValueFactory(new PropertyValueFactory<DataRecord, Double>(names[i]));
		}
		imageColumn.setCellValueFactory(new PropertyValueFactory<DataRecord, ImageView>("imageView"));
		imageColumn.setMinWidth(200);
		imageColumn.setPrefWidth(200);
//		 
		table.setEditable(false);
		table.setFixedCellSize(50.0);
//		assert(lanePictures != null);		// injected from FXML
			
		version.setText("hover0.2");
		String path = "src/peakExport3.csv";			// ---------------- READ CSV FILE HERE
		File f = new File(path);
		if (f.exists())
			readCSVFile(path, table);
		else System.err.println(path + " not found");
//		doPlot();
	}
	
	class IntStringConverter extends DoubleStringConverter
	{
		  @Override public String toString(Double value) {		      
		        if (value == null)             return "";		  // If the specified value is null, return a zero-length String
		        return String.format("%.0f", value.doubleValue());
		    }
		
	}
	// ------------------------------------------------------

	private void readCSVFile(String path, TableView<DataRecord> table)
	{
		if (path == null || table == null) return ;
		try
		{
			String[] row = null;
			FileReader rdr = new FileReader(path);
			
			CSVReader csvReader = new CSVReader(new FileReader(path));
			List<String[]> content = csvReader.readAll();
			csvReader.close();
			int nCols = -1;
			 
			row = (String[]) content.get(0);		
			nCols = row.length;
			System.out.println(nCols + " columns");
			boolean isHeader = true;
			model = FXCollections.observableArrayList();
			for (Object aRow : content)
				if (isHeader) 	isHeader = false;
				else model.add(new DataRecord((String[])aRow));
	        table.setItems(model);
		} 
		catch (NumberFormatException e)	
		{		 
			System.err.print("Wrong number of columns in row"); 
			e.printStackTrace();	
			return ;
		}
		catch (Exception e)				{			e.printStackTrace();	return ;	}
	}
	

	// ------------------------------------------------------
	@FXML private void doPlot()
	{
		final NumberAxis xAxis = new NumberAxis();
		xAxis.setLowerBound(0);
		xAxis.setUpperBound(MAX_X);
		final NumberAxis yAxis = new NumberAxis();
		yAxis.setLowerBound(0);
		yAxis.setUpperBound(MAX_Y);
		scatter = new ScatterChart<Number, Number>(xAxis, yAxis);
		xAxis.setLabel("Lane Index");
		yAxis.setLabel("Center of Well");
		scatter.setTitle("Sequential Display of Peak Centers");
		
		
		StackPane stack = new StackPane();
		lanePictures = new HBox(5);
		ScrollPane galleryScroller = new ScrollPane(lanePictures);
		galleryScroller.setPrefHeight(200);
		galleryScroller.setMinHeight(200);
		AnchorPane canvas = new AnchorPane();
		StackPane canvasStack = new StackPane();
		canvasStack.getChildren().addAll(scatter, canvas);

		VBox container = new VBox(canvasStack, galleryScroller);
		stack.getChildren().addAll(container);
   
		scatter.widthProperty().addListener((num)-> onUpdateDots());	// Add a listener to the chart's width
		gateLayer = new HoverPlotGateLayer(scatter, this, canvas);		// install listeners to implement marquee selection
		
		try
		{
			String s = "slide_lanes.png";	//chiptemplate.png 		// ----------------------- READ IMAGE FILE HERE
			Image image = new Image(s );	
			System.out.println("Size of " + s + " is " + image.getWidth() + " x " + image.getHeight());
			for (DataRecord rec : table.getItems())
			{
				rec.setImage(image);							// set the image in the DataRecord's ImageView
				canvas.getChildren().add(rec.getNode());		// add point to the graph
				anchorpane.getChildren().add(rec.getOutline());
			}
			wholeimage.setImage(image);
			wholeimage.setFitWidth(image.getWidth() / 10);
			wholeimage.setFitHeight(image.getHeight() / 10);

		}
		catch (Exception e){
			System.err.println("failed");
		}
		Stage newstage = new Stage();
		newstage.setTitle("Hover Plot Peak Data");
		newstage.setScene(new Scene(stack, 850, 450));
		newstage.setX(stage.getX());
		newstage.setY(stage.getY() + 450);

		newstage.show();
		onUpdateDots();	
	}
//---------------------------------------------------------------------------------------------------------------
	 public void onUpdateDots() {
//		 System.err.println("UpdateDots");
	        Node chartArea = scatter.lookup(".chart-plot-background");
	       
	        javafx.geometry.Bounds chartAreaBounds = chartArea.localToParent(chartArea.getBoundsInLocal());
	        NumberAxis xAxis = (NumberAxis) scatter.getXAxis();
	        NumberAxis yAxis = (NumberAxis) scatter.getYAxis();
	        double xOffset = chartAreaBounds.getMinX();
	        double yOffset = chartAreaBounds.getMinY();
			for (DataRecord rec : table.getItems())
			{
				Point2D pos = rec.getXY();
		        double dispX = xOffset + xAxis.getDisplayPosition(pos.getX());
		        double dispY = yOffset + yAxis.getDisplayPosition(pos.getY());
		        Circle c = (Circle) rec.getNode();
		        c.setCenterX(dispX);
		        c.setCenterY(dispY);
			}
			gateLayer.resetSelectionRectangleToSize(xAxis, yAxis, xOffset, yOffset);
	    }

	
}