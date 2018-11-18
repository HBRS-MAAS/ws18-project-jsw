package org.jsw.agents;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsw.helpers.GenerateOrder;
import org.jsw.helpers.ManageMessage;

@SuppressWarnings("serial")
public class CustomerAgent extends Agent {	
	private List<JSONObject> orders;
	private GenerateOrder generateOrder;
	private JSONObject incomingProposal = new JSONObject();
	private JSONObject confirmation = new JSONObject();
	
	private List<String> productTypes = new ArrayList<>(Arrays.asList("baguette", 
			"eclair", "muffin", "doughnut", "cheesecake", "croissant", "apple pie", 
			"swiss roll", "brownies", "strudel", "cup bake", "biscuit"));
	
	private List<String> bakeryName = new ArrayList<>(Arrays.asList("J-Co", 
			"BreadTalk", "Tous Le Jours", "Paris Baguette", "Chiz"));
	
	protected void setup() {
		generateOrder = new GenerateOrder();
		
		System.out.println(getAID().getLocalName() + " is ready.");
		
		addBehaviour(new RequestPerformer());
	    try {
	    	Thread.sleep(3000);
	    } catch (InterruptedException e) {
	    	//e.printStackTrace();
	    }			
	}
	
	protected void takeDown() {
        // Deregister from the yellow pages
		try {
			DFService.deregister(this);
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		System.out.println(getAID().getLocalName() + ": Terminating.");
	}
		
	private class RequestPerformer extends Behaviour {
		private AID [] sellerAgents;
		private MessageTemplate mt;
		private int step=0;
		
		public void registerCustomer(){
			System.out.println("Register Customer");
			
			// Register the Customer service in the yellow pages
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Bakery-Customer");
			//sd.setName("Bakery");
			sd.setName("Bakery");
			dfd.addServices(sd);
			
			try {
				DFService.register(CustomerAgent.this, dfd);
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
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
				
		public void action() {
			//System.out.println("Action Start");
			switch (step) {
			case 0:
				registerCustomer();
				getSellers();
				
				System.out.println("Send Order");
				
				// Send the order (message) to all sellers
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
								
				for (int i = 0; i < sellerAgents.length; ++i) {
					msg.addReceiver(sellerAgents[i]);
				}
				
				System.out.println("Prepare Order");
				
				CustomerAgent.this.orders = generateOrder.getOrder(CustomerAgent.this.productTypes);
				for(JSONObject order : orders) {
					System.out.println("order: " + order);
					msg.setConversationId("customer-order");
					msg.setLanguage("JSON");
					msg.setContent(order.toString());
					msg.addReplyTo(getAID());
					msg.setReplyWith("order-"+System.currentTimeMillis()); // Unique value
					myAgent.send(msg);
				}
				
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("customer-order"),
						MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
				step = 1;
				break;
				
			case 1:
				//System.out.println("Get Proposal");
				
				// Receive the purchase order reply: Bakery name that sells the order and the price
				ACLMessage proposal = myAgent.receive(mt);
				if (proposal != null) {
					// Purchase order reply received
					if (proposal.getPerformative() == ACLMessage.PROPOSE) {
						if (proposal.getLanguage().equals("JSON")) {
							try {
								System.out.println("Received Proposal: " + proposal.getContent());
								CustomerAgent.this.incomingProposal = new JSONObject(proposal.getContent());
								CustomerAgent.this.confirmation = ManageMessage.getConfirmation(incomingProposal,
										CustomerAgent.this.bakeryName, CustomerAgent.this.productTypes);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						System.out.println("Send Confirmation");
						
						//Send the confirmation
						ACLMessage confirm = new ACLMessage(ACLMessage.CONFIRM);
						confirm.setConversationId("customer-order");
						confirm.setLanguage("JSON");
						confirm.setContent(CustomerAgent.this.confirmation.toString());
						confirm.addReplyTo(getAID());
						confirm.setReplyWith("confirm-"+System.currentTimeMillis()); // Unique value
						send(confirm);
						
						//Debug
						System.out.println(CustomerAgent.this.confirmation);
					} else {
						System.out.println("No reply received");
						block(); //Wait until receive any reply
					}
					step = 2;
				}
			default:
				break;
			}
		}
		
		//Stop the action loop
		public boolean done() {
			if (step == 2) {
				addBehaviour(new shutdown());
			}
			return (step == 2);
		}
		
		// Taken from http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
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
}
		

           

	
	
	
	
	
	
	
	
	
