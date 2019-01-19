package org.maas.utils.ui;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.fxml.FXMLLoader;
import java.util.concurrent.CountDownLatch;
import java.net.URL;
import java.net.MalformedURLException;

public class VisualisationMain extends Application {
   
	private static final CountDownLatch countDownLatch = new CountDownLatch(1);
	public static VisualisationMain currentInstance = null;
	
	
	
	public static void setInstance(VisualisationMain visulizer) {
        currentInstance = visulizer;
		countDownLatch.countDown();}
	public static VisualisationMain waitForInstance() {
			try {
				countDownLatch.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return currentInstance;
		}
		
		public static void run(String[] args) {
			launch(args);
	}
	@Override
	public void start(Stage primaryStage) {
		try {
			
			AnchorPane root = (AnchorPane)FXMLLoader.load(getClass().getResource("/fxml/Visualisation_gui.fxml"));
			Scene scene = new Scene(root,400,400);
			scene.getStylesheets().add(getClass().getResource("/fxml/application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
			primaryStage.setTitle("Customer");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

    @Override
	public void stop() {
		System.out.println("Closing application");
}

}


