package TokenRing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MessageController implements Runnable{
    private MessageQueue queue; /*Tabela de roteamento */
    private InetAddress IPAddress;
    private int port;
    private Semaphore WaitForMessage;
    private String nickname;
    private int time_token;
    private Boolean initialToken;
    
    private static final String TOKEN = "4060";
    private static final String MESSAGE = "4066";
    private static final String AKC = "4067";
    
    private String originMessage;
    private String destNickname;
    private String originNickname;
    
    private Boolean messageReadyToSend = false;
    private String message;
    
    public MessageController(   MessageQueue q, 
                                String ip_port, 
                                int t_token,
                                Boolean t,
                                String n) throws UnknownHostException{
        
        queue = q;
        
        String aux[] = ip_port.split(":");
        
        IPAddress = InetAddress.getByName(aux[0]);
        port = Integer.parseInt(aux[1]);
        
        time_token = t_token;
        
        initialToken = t;
        
        nickname = n;
        
        WaitForMessage = new Semaphore(0);
        
    }
    
    /** ReceiveMessage()
     *  Nesta função, vc deve decidir o que fazer com a mensagem recebida do vizinho da esquerda:
     *      Se for um token, é a sua chance de enviar uma mensagem de sua fila (queue);
     *      Se for uma mensagem de dados e se for para esta estação, apenas a exiba no console, senão, 
     * envie para seu vizinho da direita;
     *       Se for um ACK e se for para você, sua mensagem foi enviada com sucesso, passe o token para o vizinho da direita, senão, 
     * repasse o ACK para o seu vizinho da direita.
     */
    public void ReceivedMessage(String rawMessage){
        
        System.out.println("Mensagem recebida: " + rawMessage);

        if (this.isMessage(rawMessage) && this.isForMe()) {
            System.out.println("É uma mensagem para mim");
            System.out.println(this.originNickname + ": " + this.originMessage);
            System.out.println("Preparando para enviar ACK");
            this.prepareACK();
        } else if (this.isACK(rawMessage) && this.isForMe()) {
            System.out.println("É um ACK para mim");
            System.out.println("Preparando para liberar o Token");
            this.prepareToken();
        } else if (this.isToken(rawMessage)) {
            System.out.println("É um Token");
            System.out.println("Preparando para enviar uma mensagem, caso exista");
            this.prepareMessage();
        } else {
            System.out.println("Esta mensagem não é endereçada a mim");
            System.out.println("Repassando a mensagem");
            this.message = rawMessage;
            this.messageReadyToSend = true;
        }
        
         /* Libera a thread para execução. */
         WaitForMessage.release();
    }
    
    private Boolean isMessage(String m) {
        String[] messageSplited = m.split(";");
        
        if (messageSplited.length != 2) {
            return false;
        }
        
        if (!messageSplited[0].equals(MessageController.MESSAGE)) {
            return false;
        } 

        
       String[] messageInfo = messageSplited[1].split(":"); 
        
        if (messageInfo.length != 3) {
            return false;
        }
        
        this.originNickname = messageInfo[0];
        this.destNickname = messageInfo[1];
        this.originMessage = messageInfo[2];
        
        return true;
    }
    
    private Boolean isACK(String m) {
        String[] messageSplited = m.split(";");
        
        if (messageSplited.length != 2) {
            return false;
        } 
        
        if (!messageSplited[0].equals(MessageController.AKC)) {
            return false;
        }
        
        this.destNickname = messageSplited[1];
        
        return true;
    }
    
    private Boolean isToken(String m) {
        return m.equals(MessageController.TOKEN);
    }
    
    private Boolean isForMe() {
        return this.destNickname.equals(this.nickname);
    }
    
    private void prepareMessage() {
        if (this.queue.isEmpty()) {
            System.out.println("Fila de mensagens vazia");
            System.out.println("Preparando para liberar o Token");
            this.prepareToken();
        } else {
            this.message = this.queue.RemoveMessage();
        }
        this.messageReadyToSend = true;
    }
    
    private void prepareACK() {
        this.message = MessageController.AKC + ";" + this.originNickname;
        this.messageReadyToSend = true;
    }
    
    private void prepareToken() {
        this.message = "";
        this.message = MessageController.TOKEN;

        this.messageReadyToSend = true;
    }
 
    private void cleanUpVariables() {
        this.message = new String();
        this.originMessage = new String();
        this.destNickname = new String();
        this.originNickname = new String();
    }
    
    @Override
    public void run() {
        DatagramSocket clientSocket = null;
        byte[] sendData;
        
        /* Cria socket para envio de mensagem */
        try {
            clientSocket = new DatagramSocket();
        } catch (SocketException ex) {
            Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        while(true){

            /* Neste exemplo, considera-se que a estação sempre recebe o token 
               e o repassa para a próxima estação. */

            try {
                /* Espera time_token segundos para o envio do token. Isso é apenas para depuração,
                   durante execução real faça time_token = 0,*/    
                Thread.sleep(time_token*1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if(this.initialToken || this.messageReadyToSend) {
                if (this.initialToken) {
                    this.message = MessageController.TOKEN;
                    this.initialToken = false;
                }
                
                /* Converte string para array de bytes para envio pelo socket. */
                sendData = this.message.getBytes();
            
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);         
                
                /* Realiza envio da mensagem. */
                try {
                    System.out.println("Enviando mensagem: " + this.message);
                    clientSocket.send(sendPacket);
                    System.out.println("Mensagem enviada");
                    this.messageReadyToSend = false;
                    this.cleanUpVariables();
                } catch (IOException ex) {
                    Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            /* A estação fica aguardando a ação gerada pela função ReceivedMessage(). */
            try {
                WaitForMessage.acquire();
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
