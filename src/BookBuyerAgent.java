
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.RequestManagementBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BookBuyerAgent extends Agent{

    private String targetBookTitle;
    
    private AID[] sellerAgents;
    
    protected void setup()
    {
        // Hej fra Kristian
        // Hej igen
        // Gejsdas
        // Hej fra peter
        // Hej fra Kasper 
        
        System.out.println("Hello Hello! Buyer agent "+getAID().getName()+" is ready");
        
        // Get the title of the book as a startup argument
        Object[] args = getArguments();
        if(args != null && args.length > 0)
        {
            targetBookTitle = (String) args[0];
            System.out.println("Trying to buy "+targetBookTitle);
            
            // Add tickerbehaviour that schedules a request to seller agents every minute
            addBehaviour(new TickerBehaviour(this, 20000) {
                protected void onTick() {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("book-selling");
                    template.addServices(sd);
                    try{
                        DFAgentDescription[] results = DFService.search(myAgent, template);
                        sellerAgents = new AID[results.length];
                        for(int i=0; i<results.length; ++i){
                            sellerAgents[i] = results[i].getName();
                        }
                    } 
                    catch(FIPAException fe){
                        fe.printStackTrace();
                    }
                    
                    // Perfrom the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
            });
        }
        else
        {
            System.out.println("No book title specified");
            doDelete();
        }
    }
    
    public void takeDown()
    {
        System.out.println("Buyer agent "+getAID().getName()+ " is shutting down");
    }
    
    /**
     * This is the behaviour used by Book-buyer agents to request seller
     * agents the target book
     */
    private class RequestPerformer extends Behaviour{
        private AID bestSeller; // The agent who provides the best offer
        private int bestPrice; // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        
        public void action(){
            switch(step){
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for(int i=0; i<sellerAgents.length; ++i){
                        cfp.addReceiver(sellerAgents[i]);
                    }
                    cfp.setContent(targetBookTitle);
                    cfp.setConversationId("book-trade");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    
                    System.out.println("CFP send to all sellers");
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if(reply != null){
                        // Reply received
                        if(reply.getPerformative() == ACLMessage.PROPOSE){
                            // this is an offer
                            int price = Integer.parseInt(reply.getContent());
                            if(bestSeller == null || price < bestPrice){
                                // This is the best offer at present
                                bestPrice = price;
                                bestSeller = reply.getSender();
                            }
                            System.out.println("Proposals received from "+reply.getSender().getName()+ " with price "+price);
                        }
                     repliesCnt++;
                     if(repliesCnt >= sellerAgents.length){
                         // We received all replies
                         step = 2;
                     }
                     
                    }
                    else
                    {
                        System.out.println("No proposals received");
                        block();
                    }
                    
                    break;
                    
                case 2:
                    // Send the purchase order to the seller that provided the best offer 
                    ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    order.addReceiver(bestSeller);
                    order.setContent(targetBookTitle);
                    order.setConversationId("book-trade");
                    order.setReplyWith("order"+System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 3;
                    System.out.println("Send purchase order to seller "+bestSeller.getName());
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if(reply != null){
                        // Purchase order reply received
                        if(reply.getPerformative() == ACLMessage.INFORM){
                            // Purchase succesful. We can terminate.
                            System.out.println(targetBookTitle+" succesfully purchased.\nPrice = "+bestPrice);
                            myAgent.doDelete();
                        }
                        step = 4;
                    }
                    else {
                        block();
                        System.out.println("No purchase order reply received");
                    }
                    break;
            }
        }
        
        public boolean done(){
            return ((step == 2 && bestSeller == null) || step == 4);
        }
    }
    
}
