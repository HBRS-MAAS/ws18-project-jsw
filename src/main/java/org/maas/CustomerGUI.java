package org.maas;

import javafx.application.Application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.event.EventHandler;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import javafx.scene.control.ProgressIndicator;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomerGUI extends Application{
	private List<String> customerName;
	private JSONArray dataArray = new  JSONArray();
	private JSONArray orders = new  JSONArray();
	
	private String test = "";
	
	public void init(JSONArray data) {
		System.out.println("init");
		dataArray = data;
		System.out.println(dataArray.toString());
		test = "initial test";
	}
	
	public void open() {		
		System.out.println("open");
		Application.launch(CustomerGUI.class);
	}
		
	@SuppressWarnings("unchecked")
	@Override
	public void start(Stage stage) {
		System.out.println("start");
		System.out.println(test);
		System.out.println(dataArray.toString());
		Text cnameLabel = new Text("Customer name");
		
		retrieve("src/main/resources/config/small/clients.json");
		customerName=getName();
		 
		//creating labels
	    //Text cnameLabel = new Text("Customer name");
	    Text orderLable = new Text("Order");
	    Text orderdateLable=new Text("Order Date");
	    Text deliverydateLable=new Text("Delivery Date");
	    Text s1=new Text("Send Order");
	    Text s2=new Text("Receive Proposal");

	    Text s4=new Text("Receive Order");
	    Text s=new Text("Status");
	    
	    ComboBox orderCB = new ComboBox();
		ComboBox customerCB = new ComboBox();
		
		customerCB.setPromptText("Customer Name");
		orderCB.setPromptText("Order ID");
	    
	    //creating dropdown menu
	    for (String name: customerName){
	    	 customerCB.getItems().add(name);
	    }

	    customerCB.setOnAction(new EventHandler<ActionEvent> () {
			@Override
			public void handle(ActionEvent event) {
				getOrder(customerCB, orderCB);
			}
	    });

	    //creating progress indicators
	    Text orderLbl = new Text("");
	    //status_sendorder.setPrefSize(5,5);

	    Text proposalLbl = new Text("");
	    //status_reciveprposal.setPrefSize(5,5);

	    Text arriveLbl = new Text("");
	    //status_reciveorder.setPrefSize(5,5);

	    TextField orderDateTF = new TextField();
	    TextField deliveryDateTF= new TextField();
	    TextArea process_status = new TextArea();

	    orderCB.setOnAction(new EventHandler<ActionEvent> () {
			@Override
			public void handle(ActionEvent event) {
				refresh(customerCB, orderCB, 
						orderDateTF, deliveryDateTF, 
						orderLbl, proposalLbl, arriveLbl);
			}
	    });

	    //Label for register
	    Button buttonExit = new Button("Exit");
	    buttonExit.setOnAction(new EventHandler<ActionEvent> () {
	    	@Override
			public void handle(ActionEvent event) {
				stage.close();
			}
	    });
	    
	    //Creating a Grid Pane
	    GridPane gridPane = new GridPane();

	    //Setting size for the pane
	    gridPane.setMinSize(1000, 1000);

	    //Setting the padding
	    gridPane.setPadding(new Insets(50, 50, 50, 50));

	    //Setting the vertical and horizontal gaps between the columns
	    gridPane.setVgap(5);
	    gridPane.setHgap(5);

	    //Setting the Grid alignment
	    gridPane.setAlignment(Pos.CENTER);

	    //Arranging all the nodes in the grid
	    gridPane.add(cnameLabel, 0, 0);
	    gridPane.add(customerCB,1,0);

	    gridPane.add(orderLable,0,1);
	    gridPane.add( orderCB,1,1);

	    gridPane.add(orderdateLable,0,2);
	    gridPane.add( orderDateTF ,1,2);

	    gridPane.add(deliverydateLable,0,3);
	    gridPane.add( deliveryDateTF,1,3);

	    gridPane.add(s,0,4);
	    gridPane.add(orderLbl,0,5);
	    gridPane.add( s1,1,5);

	    gridPane.add(proposalLbl,0,6);
	    gridPane.add( s2,1,6);

	    gridPane.add(arriveLbl,0,7);
	    gridPane.add( s4,1,7);

	    gridPane.add(process_status,1,9);
	    gridPane.add(buttonExit,2,12);

	    //Styling nodes
	    buttonExit.setStyle(
	    	"-fx-background-color: darkslateblue; -fx-textfill: white;");

    	cnameLabel.setStyle("-fx-font: normal bold 15px 'serif' ");
    	orderLable .setStyle("-fx-font: normal bold 15px 'serif' ");
    	orderdateLable.setStyle("-fx-font: normal bold 15px 'serif' ");
    	deliverydateLable.setStyle("-fx-font: normal bold 15px 'serif' ");

    	s.setStyle("-fx-font: normal bold 15px 'serif' ");
    	s1.setStyle("-fx-font: normal bold 15px 'serif' ");
    	s2.setStyle("-fx-font: normal bold 15px 'serif' ");

    	s4.setStyle("-fx-font: normal bold 15px 'serif' ");
    	s2.setStyle("-fx-font: normal bold 15px 'serif' ");

    	//Setting the back ground color
    	gridPane.setStyle("-fx-background-color: BEIGE;");

    	//Creating a scene object
    	Scene scene = new Scene(gridPane);

    	//Setting title to the Stage
    	stage.setTitle("Customer");

    	//Adding scene to the stage
    	stage.setScene(scene);

    	//Displaying the contents of the stage
    	stage.show();
    }
	 
	public void retrieve(String fileName) {
		File file = new File(fileName);
		String filePath = file.getAbsolutePath();
		String fileContent = "";
		JSONArray orders = new JSONArray();
		
		JSONObject listStatus= new JSONObject();
		listStatus.put("send_order", false);
		listStatus.put("receive_proposal", false);
		listStatus.put("receive_order", false);
		
		//System.out.println("retrieve all order");
		try {
			fileContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
			dataArray = new JSONArray(fileContent);
			
			for(int i = 0; i < dataArray.length(); i++){
				
				orders = dataArray.getJSONObject(i).getJSONArray("orders");
				
				//Add new keys to order
				for(int j = 0; j < orders.length(); j++) {
					orders.getJSONObject(j).put("status", listStatus);
				}
				
				dataArray.getJSONObject(i).put("orders", orders);
			}
			
			//System.out.println(dataArray.toString());
	
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public List<String> getName() {
		List<String> name = new ArrayList();


		try {
			for (int i = 0; i < dataArray.length(); i++) {
				JSONObject customerData = dataArray.getJSONObject(i);
				name.add(customerData.getString("name"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return name;
	}
	private void getOrder(ComboBox customerCB, ComboBox orderCB) {
		orderCB.getItems().clear();
		orderCB.setPromptText("Order ID");
				
		String customerName = "";
		String order_id;
		
		//Take Orders from Customer (based on the name)
		try {
			for (int j = 0; j < dataArray.length(); j++) {
				customerName = dataArray.getJSONObject(j).getString("name");

				if ((String)customerCB.getValue()==customerName) {
					orders = dataArray.getJSONObject(j).getJSONArray("orders");
					for (int i = 0; i < orders.length(); i++) {

						order_id = orders.getJSONObject(i).getString("guid");
						
						orderCB.getItems().add(order_id);
					}
				break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private void refresh(ComboBox customerCB, ComboBox orderCB, 
			TextField orderDateTF, TextField deliveryDateTF, 
			Text s1TF, Text s2TF, Text s3TF){
	    deliveryDateTF.clear();
	    orderDateTF.clear();
	    s1TF.setText("");
	    s2TF.setText("");
	    s3TF.setText("");
	    
		String customerName = "";
		String order_id;
		List id;
		
		JSONObject date=new JSONObject();
		JSONObject status=new JSONObject();
		
		//Take Orders from Customer (based on the name)
		try {
			for (int j = 0; j < dataArray.length(); j++) {
				customerName = dataArray.getJSONObject(j).getString("name");

				if ((String)customerCB.getValue()==customerName) {
					orders = dataArray.getJSONObject(j).getJSONArray("orders");
					
					for (int i = 0; i < orders.length(); i++) {

						order_id = orders.getJSONObject(i).getString("guid");
						status = orders.getJSONObject(i).getJSONObject("status");
						
						if (orderCB.getValue()== order_id){

							date = orders.getJSONObject(i).getJSONObject("order_date");
	
							int hour = date.getInt("hour");
							int day = date.getInt("day");
	
							orderDateTF.setText("Hour: " + hour + " Day: " + day);
	
							date = orders.getJSONObject(i).getJSONObject("delivery_date");
	
							hour = date.getInt("hour");
							day = date.getInt("day");
	
							deliveryDateTF.setText("Hour: " + hour + " Day: " + day);
								
							boolean sendStatus = status.getBoolean("send_order");
							if (sendStatus== true) {
								s1TF.setText("O");
							} else {
								s1TF.setText("X");
							}
	
							boolean receiveProposal = status.getBoolean("receive_proposal");
							if (receiveProposal== true){
								s2TF.setText("O");
							} else {
								s2TF.setText("X");
							}
							
							boolean receiveOrder=status.getBoolean("receive_order");
							if (receiveOrder== true){
								s3TF.setText("O");
							} else {
								s3TF.setText("X");
							}
							
							break;
						}
					}
					break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

     }
	
    private enum Status{
    	 SEND_ORDER,
    	 RECEIVE_PROPOSAL,
    	 RECEIVE_ORDER;
    }

    public void setStatus(String customerName, String orderID, Status orderStatus) {
    	 for (int i = 0; i < dataArray.length(); i++) {
             String name = dataArray.getJSONObject(i).getString("name");
             orders = dataArray.getJSONObject(i).getJSONArray("orders");

             if (name == customerName) {
                 for (int j = 0; j < orders.length(); j++) {
                     String id = orders.getJSONObject(i).getString("guid");

                     if (id == orderID) {
                    	 switch (orderStatus){
                    		 case SEND_ORDER:
                    			 orders.getJSONObject(i).getJSONObject("status").put("send_status", true);
                    			 break;
                    		 case RECEIVE_PROPOSAL:
                    			 orders.getJSONObject(i).getJSONObject("status").put("receive_proposal", true);
                    			 break;
                    		 case RECEIVE_ORDER:
                    			 orders.getJSONObject(i).getJSONObject("status").put("receive_order", true);
                    			 break;
                    	 }
                         orders.getJSONObject(i).getJSONObject("status").put("send_status", true);
                         break;
                     }
                 }break;
             }
         }
     }
}