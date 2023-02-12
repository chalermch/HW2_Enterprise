/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package livescore;


import java.util.Scanner;
import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

public class Main {
    @Resource(mappedName = "jms/ConnectionFactory")
    private static ConnectionFactory connectionFactory;
    @Resource(mappedName = "jms/LiveScoreQueue")
    private static Queue queue;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Connection connection = null;
        TextListener listener = null;
                 
        try {
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(
                        false,
                        Session.AUTO_ACKNOWLEDGE);
            listener = new TextListener();
            //Create a temporary queue that this client will listen for responses on then create a consumer
            //that consumes message from this temporary queue...for a real application a client should reuse
            //the same temp queue for each message to the server...one temp queue per client
            Queue tempDest = session.createTemporaryQueue();
            MessageConsumer responseConsumer = session.createConsumer(tempDest);
            responseConsumer.setMessageListener(listener);
            MessageProducer producer = session.createProducer(queue);
            Scanner in = new Scanner(System.in);
            System.out.println("To end program, type Q or q, then <return>");
            while(true){
                TextMessage message = session.createTextMessage();
                System.out.print("Enter Live Score : ");
                String str = in.nextLine();
                if(str.equals("q") || str.equals("Q")){
                    break;
                }
                message.setText(str);
                message.setJMSReplyTo(tempDest);
                          
                //Set a correlation ID so when you get a response you know which sent message the response is for
                //If there is never more than one outstanding message to the server then the
                //same correlation ID can be used for all the messages...if there is more than one outstanding
                //message to the server you would presumably want to associate the correlation ID with this
                //message somehow...a Map works good
                String correlationId = "12345";
                message.setJMSCorrelationID(correlationId);
                connection.start();
                System.out.println("Sending message: " + message.getText());
                producer.send(message);
            }
            
            
        } catch (JMSException e) {
            System.err.println("Exception occurred: " + e.toString());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (JMSException e) {
                }
            }
        }
    }
}