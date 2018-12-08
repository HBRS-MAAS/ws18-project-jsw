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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
	
	private int[] latestOrder = new int[2];
	
	private AID [] sellerAgents;
	
	private int sum_sent; //number of sent orders
	private int sum_total; //number o total orders
	private boolean process_done; //wait until communication with order processing finished

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
		sum_total = getOrder(customerName);
				
		register("customer", customerID);

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
        sd.setType("OrderProcessing");
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
		private boolean passTime = false;
		
		@Override
		public void action() {
			if(!getAllowAction()) {
                return;
            }
		    					
			int hour = getCurrentHour();
			int day = getCurrentDay();
			
			passTime = whenLatestOrder(hour, day);
			
			//System.out.println("current hour: " + getCurrentHour());
			//System.out.println("current day: " + getCurrentDay());
			
			//Get Order at Specified Time
			ArrayList<JSONObject> orderList = getCurrentOrder(hour, day);
			JSONObject order = new JSONObject();
			
	    	while (orderList.size() > 0) {
	    		//System.out.println(order);
	    		order = orderList.remove(0);
	    		CustomerAgent.this.addBehaviour(new CallForProposal(order));
	    		sum_sent++;
	    		process_done = false;
	    	}
	    	
	    	//System.out.println("call finish");
	    	finished();
	    	myAgent.addBehaviour(new GetCurrentOrder()); //don't call when all order are ordered?
	    	isDone = true;	    	
		}

		@Override
		public boolean done() {
			//System.out.println(sum_sent);
			//System.out.println(sum_total);
			
			if (process_done && (sum_sent >= sum_total || passTime == true)) {
				addBehaviour(new shutdown());
			}
			return isDone;
		}
		
		private class shutdown extends OneShotBehaviour{
			public void action() {
				ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
				Codec codec = new SLCodec();
				myAgent.getContentManager().registerLanguage(codec);
				myAgent.getContentManager().registerOntology(JADEManagementOntology.getInstance());
						shutdownMessage.addReceiver(myAgent.getAMS());
						shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
						shutdownMessage.setOntology(JADEManagementOntology.getInstance().getName());
				try {
					myAgent.getContentManager().fillContent(shutdownMessage,new Action(myAgent.getAID(), new ShutdownPlatform()));
					myAgent.send(shutdownMessage);
				} catch (Exception e) {
					//LOGGER.error(e);
				}
			}
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
						//System.out.println("selected " + selected);
						String products = selected.getString(name); 
						
						//System.out.println(products.length());
						
						if (products.length() > 0) {
							reOrder = myOrder;
							
							reOrder.put("products", products);
							
							ACLMessage confirm = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							confirm.addReceiver(sellerAgents[i]);
							confirm.setLanguage("JSON");
							confirm.setContent(reOrder.toString());
							send(confirm);
							
							//System.out.println("confirm " + name + ": " + confirm.getContent());
						} else {
							ACLMessage confirm = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
							confirm.addReceiver(sellerAgents[i]);
							confirm.setLanguage("JSON");
							confirm.setContent("Your bakery is too expensive.. :(");
							send(confirm);
							
							//System.out.println("confirm " + name + ": " + confirm.getContent());
						}
					}
	            }
				
				finished();
				process_done = true;
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
  					
  					//Should the length reduced by one?
  					System.out.println("Customer " + customerName + " has " + (orders.length() - 1) + " order");
  					
  					return orders.length() - 1;
  				}
  			}
  		} catch (JSONException e) {
  			e.printStackTrace();
  		}
  		
  		return 0;
  	}
  	
  	
  	private boolean whenLatestOrder(int currentHour, int currentDay) {
  		JSONObject order_time = new JSONObject();

  		try {
			ArrayList<Date> date = new ArrayList<>();
			
			for (int i = 0; i < orders.length(); i++) {
				order_time = orders.getJSONObject(i).getJSONObject("order_date");
				
				int day = order_time.getInt("day");
				int hour = order_time.getInt("hour");
				
				date.add(new Date(hour, day));
			}
			
			Comparator<Date> comparator = Comparator.comparingInt(Date::getDay).thenComparingInt(Date::getHour);

		    // Sort the stream:
		    Stream<Date> DateStream = date.stream().sorted(comparator);

		    // Make sure that the output is as expected:
		    List<Date> sortedDate = DateStream.collect(Collectors.toList());
		    
		    /*for (int i = 0; i < sortedDate.size(); i++) {
		    	System.out.println(sortedDate.get(i).getDay() + " ~~ " + sortedDate.get(i).getHour());
		    }*/
		    
		    int lastDay = sortedDate.get(sortedDate.size() - 1).getDay();
		    int lastHour = sortedDate.get(sortedDate.size() - 1).getHour();
		    
		    if (currentDay >= lastDay && currentHour >= lastHour) {
		    	return true;
		    }
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
  		return false;	
  	} 
  	
  	public static class Date {
  	    public int hour;
  	    public int day;
  		
  		public Date(int hour, int day)
  	    {
  	        this.hour = hour;
  	        this.day = day;
  	    }
  		
  		public int getHour() {
  			return this.hour;
  		}
  		
  		public int getDay() {
  			return this.day;
  		}
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
  	
  	private ArrayList<JSONObject> getCurrentOrder(int currentHour, int currentDay) {
  		JSONObject order_time = new JSONObject();
  		ArrayList<JSONObject> orderList = new ArrayList<JSONObject>();
  		
  		//Check Date
  		try {
  			int n = 0;
  			for (int i = 0; i < orders.length(); i++) {
  				order_time = orders.getJSONObject(i).getJSONObject("order_date");
  				
  				int hour = order_time.getInt("hour");
  				int day = order_time.getInt("day");
  				
  				if ((hour == currentHour) && (day == currentDay)) {
  					orderList.add(orders.getJSONObject(i));
  					n++;
  				}
  			}
  			
  			if (n > 0) {
  				System.out.println(orderList.toString());
  			}
  			
  		} catch (JSONException e) {
  			e.printStackTrace();
  		}
  		
  		return orderList;
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