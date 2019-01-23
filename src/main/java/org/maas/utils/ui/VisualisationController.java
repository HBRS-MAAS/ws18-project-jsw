package org.maas.utils.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class VisualisationController implements Initializable  {
	public JSONObject listStatus= new JSONObject();
	
	@FXML
    private TextField orderDateTF;

    @FXML
    private TextField deliveryDateTF;

    @FXML
    private Label orderLbl;

    @FXML
    private Label proposalLbl;

    @FXML
    private Label arriveLbl;

    @FXML
    private ComboBox<String> customerCB;

    @FXML
    private ComboBox<String> orderCB;

    @FXML
    private Button buttonExit;

    @FXML
    void exit(ActionEvent event) {
    	  Stage stage = (Stage) buttonExit.getScene().getWindow();
          // do what you have to do
          stage.close();

    }
	private List<String> customerName;
	private static JSONArray dataArray = new JSONArray();
	private JSONArray orders = new JSONArray();
	
	private static String scenario = "small";
	private static String scenarioPath = "src/main/resources/config/";
	
	public VisualisationController() {
		retrieve(scenarioPath + scenario + "/clients.json");
		customerName= getName();
	}
	
	public void setScenario(String args) {
		scenario = args;
	}

	public void updateStatus(JSONObject msg) {
		System.out.println("update status");
		String id = msg.getString("customer_id");
		String status = msg.getString("status");
		String guid = msg.getString("guid");
		
		for(int i = 0; i < dataArray.length(); i++){
			String customerID = dataArray.getJSONObject(i).getString("guid");
			
			if (customerID.equals(id)) {
				//System.out.println("same id");
				orders = dataArray.getJSONObject(i).getJSONArray("orders");
				
				for(int j = 0; j < orders.length(); j++) {
					//System.out.println(orders.getJSONObject(j).getString("guid"));
					if (orders.getJSONObject(j).getString("guid").equals(guid)) {
						JSONObject newStatus = orders.getJSONObject(j).getJSONObject("status");
						
						newStatus.put(status, true);
						
						orders.getJSONObject(j).put("status", newStatus);
						//System.out.println(orders.toString());
						break;
					}
				}
				
				dataArray.getJSONObject(i).put("orders", orders);
				//System.out.println(dataArray.getJSONObject(i).toString());
				break;
			}
			
			System.out.println(i);
		}		
	}

	public void retrieve(String fileName) {
		File file = new File(fileName);
		String filePath = file.getAbsolutePath();
		String fileContent = "";
		JSONArray orders = new JSONArray();

		
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
		List<String> name = new ArrayList<String>();


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
	 @FXML
	    void getOrder(ActionEvent event){


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
	  @FXML
	    void refresh(ActionEvent event)  {
		   deliveryDateTF.clear();
		    orderDateTF.clear();
		    orderLbl.setText("");
		    proposalLbl.setText("");
		    arriveLbl.setText("");

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
									orderLbl.setText("O");
								} else {
									orderLbl.setText("X");
								}

								boolean receiveProposal = status.getBoolean("receive_proposal");
								if (receiveProposal== true){
									proposalLbl.setText("O");
								} else {
									proposalLbl.setText("X");
								}

								boolean receiveOrder=status.getBoolean("receive_order");
								if (receiveOrder== true){
									arriveLbl.setText("O");
								} else {
									arriveLbl.setText("X");
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

	@Override
     public void initialize(URL location, ResourceBundle resources) {
//		 TODO Auto-generated method stub

	     ObservableList<String> cn = FXCollections.observableArrayList(customerName);

	     customerCB.getItems().clear();
	     customerCB.setItems(cn);
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




