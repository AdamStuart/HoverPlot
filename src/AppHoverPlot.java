

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class AppHoverPlot extends Application
{
	@Override
	public void start(Stage primaryStage) {
		try {
	        Pane rootPane = null;
	        try {
	        	URL resURL = getClass().getResource("hoverplot.fxml");
	            if (resURL == null)  
	            {
	            	System.err.println("getResource failed: " + "hoverplot.fxml");
	            	System.exit(-1);
	            }
	            FXMLLoader loader = new FXMLLoader();
	            loader.setLocation(resURL);
	            loader.setBuilderFactory(new JavaFXBuilderFactory());
	            rootPane = loader.load(resURL.openStream());
	            controller = loader.getController();
	            controller.setStage(primaryStage);
	            primaryStage.setX(50);
	        } 
	        catch (IOException ex) 	
	        {
	            rootPane = new BorderPane();
	            Label l = new Label("Error on FXML loading:" + ex.getMessage());
	            rootPane.getChildren().add(l);
	            Logger.getLogger(AppHoverPlot.class.getName()).log(Level.SEVERE, null, ex);
	        }
	        instance = this;
			Scene scene = new Scene(rootPane,900,900);

//			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	private HoverPlotController controller;
	
	@Override public void stop()	{	}
	
	static AppHoverPlot instance;
	public static AppHoverPlot getApp()	{ return instance;	}
	public static void main(String[] args) {		launch(args);	}
}
