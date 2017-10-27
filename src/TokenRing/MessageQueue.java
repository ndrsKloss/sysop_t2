package TokenRing;

import java.util.LinkedList;
import java.util.Deque;


/* Esta classe deve implementar uma fila de mensagens. Observe que esta fila será
 * acessada por um consumidor (MessageSender) e um produtor (Classe principal, TokenRing).
 * Portanto, implemente controle de acesso (sincronização), para acesso a fila. 
 */

public class MessageQueue {
    /*Implemente uma estrutura de dados para manter uma lista de mensagens em formato string. 
     * Você pode, por exemplo, usar um ArrayList(). 
     * Não se esqueça que em uma fila, o primeiro elemente a entrar será o primeiro
     * a ser removido.
    */
    Deque deque = new LinkedList<String>();

    
    public MessageQueue(){
    
    }
    
    
    public void AddMessage(String message){
        /* Adicione a mensagem no final da fila. Não se esqueça de garantir que apenas uma thread faça isso 
        por vez. */
        
        deque.addLast(message);
    }
    
    public String RemoveMessage(){
        /* Retive uma mensagem do inicio da fila. Não se esqueça de garantir que apenas uma thread faça isso 
        por vez.  */
        
        return (String) deque.pop();
    }
    

    
}
