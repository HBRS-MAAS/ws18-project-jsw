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

public class OrderProcessingAgent extends Agent {
	private List<String> OrderList;
	// The list of Bakery agents
	private AID[] BakeryAgents;

	protected void setup() {
		// Welcome message
		System.out.println(getAID().getLocalName() + " is ready.");
		//System.out.println(getAID().getName() + " will process orders  " + OrderList);
		registerBakery();
		addBehaviour(new OrderProcessingServer());
		//checkAgentAvailability();

	}
	
	public void registerBakery(){
		//Register the customer in particular area
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("Bakery Orderprocessing Agent");
		sd.setName("Bakery");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	protected void takeDown() {
	        // Deregister 
			try {
				DFService.deregister(this);
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			System.out.println(getAID().getLocalName() + ": Terminating.");
		}
	
	private void checkAgentAvailability() {
		// TODO Auto-generated method stub
		
	}

	
	private class OrderProcessingServer extends CyclicyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg!=null) {
            	System.out.println(" Order to " + myAgent.getLocalName() + " is : " + msg.getContent());
            	ACLMessage reply = msg.createReply();
            	reply.setPerformative(ACLMessage.INFORM);
 
            	reply.setContent("Order Recived");
            	myAgent.send(reply);
            }
            
            else {
				block();
}
		}
	}


	// http://www.rickyvanrijn.nl/2017/08/29/how-to-shutdown-jade-agent-platform-programmatically/
	private class shutdown extends OneShotBehaviour {
		public void action() {
			ACLMessage shutdownMessage = new ACLMessage(ACLMessage.REQUEST);
			Codec codec = new SLCodec();
			myAgent.getContentManager().registerLanguage(codec);
			myAgent.getContentManager().registerOntology(
					JADEManagementOntology.getInstance());
			shutdownMessage.addReceiver(myAgent.getAMS());
			shutdownMessage.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			shutdownMessage.setOntology(JADEManagementOntology.getInstance()
					.getName());
			try {
				myAgent.getContentManager().fillContent(shutdownMessage,
						new Action(myAgent.getAID(), new ShutdownPlatform()));
				myAgent.send(shutdownMessage);
			} catch (Exception e) {
				// LOGGER.error(e);
			}
		}

	}

}
