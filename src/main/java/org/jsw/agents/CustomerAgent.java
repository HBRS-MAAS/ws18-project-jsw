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

import java.util.List;
import java.util.Vector;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsw.helpers.GenerateOrder;

public class CustomerAgent extends Agent {
	private List<JSONObject> orders;
	private GenerateOrder generateOrder;
	
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
		private AID [] OrderProcessingAgents;
		private AID bakeryOrders;
		private MessageTemplate mt;
		private int step=0;
		
		public void registerCustomer(){
			// Register the Customer service in the yellow pages
			DFAgentDescription dfd = new DFAgentDescription();
			dfd.setName(getAID());
			ServiceDescription sd = new ServiceDescription();
			sd.setType("Bakery-Customer");
			sd.setName("Bakery");
			dfd.addServices(sd);
			
			try {
				DFService.register(this, dfd);
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}
		}

				
		public void action() {
			switch (step) {
			case 0:
				registerCustomer();
				
				// Send the order (message) to all sellers
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				for (int i = 0; i < OrderProcessingAgents.length; ++i) {
					msg.addReceiver(OrderProcessingAgents[i]);
				}
				
				orders = generateOrder.getOrder();
				for(JSONObject order : orders) {
					msg.setConversationId("customer-order");
					msg.setLanguage("JSON");
					msg.setContent(order.toString());
					msg.addReplyTo(getAID());
					msg.setReplyWith("msg"+System.currentTimeMillis()); // Unique value
					send(msg);
				}
				
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("customer-order"),
						MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
				step = 1;
				break;
				
			case 1:
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						bakeryOrders = reply.getSender();
						System.out.println(reply.getContent());
					}
					else {
						System.out.println("No reply recived");
						}
					}
					else {
						block();
					}
					step = 2;
				}
}
					
		  public boolean done() {
			if (step == 2) {
				addBehaviour(new shutdown());
			}
			return (step == 2);
}
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
				}
				catch (Exception e) {
				    //LOGGER.error(e);
			}
		}

}
           

	
	
	
	
	
	
	
	
	
