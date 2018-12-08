package org.maas.agents;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.FIPANames;
import jade.domain.JADEAgentManagement.JADEManagementOntology;
import jade.domain.JADEAgentManagement.ShutdownPlatform;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CustomerAgent extends BaseAgent {
	private JSONArray dataArray = new JSONArray();
	private JSONArray orders = new JSONArray();
	//private JSONObject location = new JSONObject();
	private Object location = null;
	
	private String customerName = "";
	private String customerID = "";
	
	private int n = 0;
	private int total = 0;
	
	private AID [] sellerAgents;

    protected void setup() {
    	super.setup();
    	
    	//Wait until order procesing agent set up
    	try {
    		Thread.sleep(3000);
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    	
		customerName = getAID().getLocalName();	
		
		System.out.println(customerName + " is ready.");
				
		getSellers();
		
		//System.out.println(customerName + " will send order to " + sellerAgents.length + " sellers");
		
		retrieve("src/main/resources/config/small/clients.json");
		total = getOrder(customerName);
		
		register("Customer", customerID);

        //addBehaviour(new isNewOrderChecker());
        addBehaviour(new GetCurrentOrder());
    }

    protected void takeDown() {
		deRegister();
		System.out.println("Customer " + customerName + " sent " + orders.length() + " order");
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
	
	protected void getSellers() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Bakery-Seller");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(CustomerAgent.this, template);
            sellerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                sellerAgents[i] = result[i].getName();
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
	}
	
	private class GetCurrentOrder extends Behaviour {
		private boolean isDone = false;
		
		@Override
		public void action() {
			if(!getAllowAction()) {
                return;
            }
		    					
			int hour = getCurrentHour();
			int day = getCurrentDay();
			
			//System.out.println("current hour: " + getCurrentHour());
			//System.out.println("current day: " + getCurrentDay());
			
			//Get Order at Specified Time
			JSONObject order = getCurrentOrder(hour, day);
			    	
	    	if (order != null) {
	    		//System.out.println(order);
	    		CustomerAgent.this.addBehaviour(new CallForProposal(order));
	    	}
	    	
	    	//System.out.println("call finish");
	    	finished();
	    	myAgent.addBehaviour(new GetCurrentOrder()); //don't call when all order are ordered?
	    	isDone = true;	    	
		}

		@Override
		public boolean done() {
			return isDone;
		}
		
	}
	
	private class CallForProposal extends OneShotBehaviour {
		private MessageTemplate mt;
		private JSONObject myOrder = new JSONObject();
		
		CallForProposal(JSONObject order) {
			myOrder = order;
		}
		
		@Override
		public void action() {
			// Send the order (message) to all sellers
			ACLMessage msg = new ACLMessage(ACLMessage.CFP);
							
			for (int i = 0; i < sellerAgents.length; ++i) {
				msg.addReceiver(sellerAgents[i]);
			}
			
			//System.out.println("myOrder: " + myOrder.toString());
			
			myOrder = includeLocation(myOrder);		    	
    		
    		String orderID = "";
			try {
				orderID = myOrder.getString("guid");
				msg.setConversationId(orderID);
				msg.setLanguage("JSON");
				msg.setContent(myOrder.toString());
				msg.addReplyTo(getAID());
				msg.setReplyWith("order-"+System.currentTimeMillis()); // Unique value
				sendMessage(msg);
				
				//System.out.println(customerName + " send order: " + msg.toString());
				
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId(orderID),
						MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
				
				CustomerAgent.this.addBehaviour(new ReceiveProposal(mt, myOrder));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			finished();
		}		
	}
	
	// Receive Proposals from Bakeries: Bakery name that sells the order and the price
	private class ReceiveProposal extends Behaviour {
		private JSONObject incomingProposal = new JSONObject();
		private JSONObject myOrder = new JSONObject();
		
		private MessageTemplate myTemplate;
		private boolean isDone = false;
		
		private String orderID = "";
		
		ReceiveProposal(MessageTemplate mt, JSONObject order) {
			System.out.println("Received Proposal");
			myTemplate = mt;
			myOrder = order;
		}		
		
		@Override
		public void action() {
			ACLMessage message = myAgent.receive(myTemplate);				
			
			if (message != null) {
				//Purchase order reply received
				//System.out.println("Received Message: " + message.getContent());
				String bakeryName = message.getSender().getLocalName();
				JSONObject products = new JSONObject();
				
				if (message.getPerformative() == ACLMessage.PROPOSE) {
					if (message.getLanguage().equals("JSON")) {
						try {
							JSONObject proposal = new JSONObject(message.getContent());
							products = proposal.getJSONObject("products");
							
							incomingProposal.put(bakeryName, products);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				} else if (message.getPerformative() == ACLMessage.REFUSE) {
					if (message.getLanguage().equals("JSON")) {
						try {
							incomingProposal.put(bakeryName, products);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				
				if (incomingProposal.length() == sellerAgents.length) {
					//System.out.println(incomingProposal.length());
					//System.out.println(customerName + "receive proposal from " + sellerAgents.length + " sellers");
					System.out.println("incomingProposal " + incomingProposal);
					
					isDone = true;
					finished();
					
					CustomerAgent.this.addBehaviour(new SendConfirmation(incomingProposal, myOrder));
				}  
			} else {
				block();
			}
		}

		@Override
		public boolean done() {
			return isDone;
		}
		
	}
	
	private class SendConfirmation extends OneShotBehaviour {
		private JSONObject proposal = new JSONObject();
		private JSONObject selected = new JSONObject();
		private JSONObject myOrder = new JSONObject();
		private JSONObject reOrder = new JSONObject();
		
		SendConfirmation(JSONObject incomingProposal, JSONObject order) {
			proposal = incomingProposal;
			myOrder = order;
		}
		
		@Override
		public void action() {
			try {
				selected = findTheCheapest(proposal);
				
				//System.out.println("Send Confirmation: " + confirmation);
				
				//Send the confirmation
				for (int i = 0; i < sellerAgents.length; ++i) {
					String name = sellerAgents[i].getLocalName();
					if (selected.has(name)) {						
						System.out.println("selected " + selected);
						String products = selected.getString(name); 
						
						System.out.println(products.length());
						
						if (products.length() > 0) {
							reOrder = myOrder;
							
							reOrder.put("products", products);
							
							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							confirm.addReceiver(sellerAgents[i]);
							confirm.setLanguage("JSON");
							confirm.setContent(reOrder.toString());
							send(confirm);
							
							System.out.println("confirm " + name + ": " + confirm.getContent());
						} else {
							ACLMessage confirm = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
							confirm.addReceiver(sellerAgents[i]);
							confirm.setLanguage("JSON");
							confirm.setContent("Your bakery is too expensive.. :(");
							send(confirm);
							
							System.out.println("confirm " + name + ": " + confirm.getContent());
						}
					}
	            }
				
				finished();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}
    
  //FUNCTIONS TO MANAGE JSON OBJECTS
  	//Retrieve client data from config file
  	private void retrieve(String fileName) {
  		File file = new File(fileName);
  		String filePath = file.getAbsolutePath();
  		String fileContent = "";	
  		
  		try {
  			fileContent = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
  			dataArray = new JSONArray(fileContent);
  		} catch (JSONException e) {
  			e.printStackTrace();
  		} catch (IOException e) {
  			e.printStackTrace();
  		}
  	}
  	
  	//Get list of order 
  	private int getOrder(String name) {
  		String customerName = "";
  				
  		//Take Orders from Customer (based on the name)
  		try {
  			for (int i = 0; i < dataArray.length(); i++) {
  				customerName = dataArray.getJSONObject(i).getString("name");
  				
  				if (customerName.equals(name)) {
  					orders = dataArray.getJSONObject(i).getJSONArray("orders");
  					customerID = dataArray.getJSONObject(i).getString("guid");
  					//location = dataArray.getJSONObject(i).getJSONObject("location");
  					location = dataArray.getJSONObject(i).get("location");
  					
  					System.out.println("Customer " + customerName + " has " + orders.length() + " order");
  					
  					return orders.length();
  				}
  			}
  		} catch (JSONException e) {
  			e.printStackTrace();
  		}
  		
  		return 0;
  	}
  	
  	private String getOrderID(JSONObject order) {
  		try {
  			return order.getString("guid");
  		} catch (JSONException e) {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
  		}
  		return null;
  	}
  	
  	private JSONObject findTheCheapest(JSONObject proposal) {
  		JSONObject confirmation = new JSONObject();
  		JSONObject product = new JSONObject();
  		
  		List<String> bakeryName = new ArrayList<String>();
  		List<String> productTypes = new ArrayList<String>();
  		
  		String chosenBakery = "";
  		
  		//Get All Bakery Name
  		try {
  			Iterator<?> iter = proposal.keys();
  			while(iter.hasNext()) {
  				String key = (String)iter.next();
  				bakeryName.add(key);
  				
  				product = proposal.getJSONObject(key);
  				Iterator<?> iter2 = product.keys();
  				while(iter2.hasNext()) {
  					String key2 = (String)iter2.next();
  					if (!productTypes.contains(key2)) {
  						productTypes.add(key2);
  					}
  				}
  			}
  		} catch (JSONException e) {
  			e.printStackTrace();
  		}
  		
  		//Get The Cheapest Price
  		try {
  			for (String type : productTypes) {
  				Double min_price = Double.MAX_VALUE;
  				for (String name : bakeryName) {
  					product = proposal.getJSONObject(name);	
  					
  					if (min_price > product.getDouble(type) && product.getDouble(type) != 0) {
  						chosenBakery = name;
  						min_price = product.getDouble(type);
  					}
  				}
  				
  				if (confirmation.has(chosenBakery)) {
  					type = type + ", " + confirmation.getString(chosenBakery);
  				}
  				
  				confirmation.put(chosenBakery, type);
  			}
  		} catch (JSONException e) {
  			e.printStackTrace();
  		}
  		
  		return confirmation;
  	}
  	
  	private JSONObject getCurrentOrder(int currentHour, int currentDay) {
  		JSONObject order_time = new JSONObject();
  		
  		//Check Date
  		try {
  			for (int i = 0; i < orders.length(); i++) {
  				order_time = orders.getJSONObject(i).getJSONObject("order_date");
  				
  				int hour = order_time.getInt("hour");
  				int day = order_time.getInt("day");
  				
  				if ((hour == currentHour) && (day == currentDay)) {
  					return orders.getJSONObject(i);
  				} else {
  					baseAgent.finished();
  				}
  			}
  		} catch (JSONException e) {
  			e.printStackTrace();
  		}
  		
  		return null;
  	}
  	
  	private JSONObject includeLocation(JSONObject order) {
  		try {
  			order.put("location", location);
  		} catch (JSONException e) {
  			e.printStackTrace();
  		}
  		
  		return order;
  	}
}