package br.inf.ufes.pp2017_01;
 
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.List;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;

import com.sun.messaging.ConnectionConfiguration;

public class MasterImpl implements Master, MessageListener {
	// Referência (a ser remota) deste objeto
	private static Master mref;
	private static MasterImpl obj;
	
	// Contador de ataques (usado como attackNumber)
	static int currentAttack = 0;
	
	// Tamanho total do dicionário
	static int lenDict;
	
	// Tamanho dos ataques
	static int m; // TODO inicializar o m por args
	
	// UUID -> Slave
	protected static ConcurrentHashMap<Integer, Integer> resultsPerAttack = new ConcurrentHashMap<Integer, Integer>();
	
	// Integer -> Attack
	protected static ConcurrentHashMap<Integer, LinkedList<Guess>> guessesPerAttack = new ConcurrentHashMap<Integer, LinkedList<Guess>>();
	
	// Connection factory
	static com.sun.messaging.ConnectionFactory connectionFactory;
	
	// Context
	static JMSContext context;
	
	// Producer
	static JMSProducer producer;
	
	// Consumer
	static JMSConsumer consumer;
	
	// Subattacks queue
	static Queue subattacks;
	
	// Guesses queue
	static Queue guesses;

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
		// Número do subataque corrente
		int attackNumber = currentAttack + 1;
		
		Runnable r = new Runnable() {		
			public void run() {
				int lenChunks;
				int remainder;
				int start = 0;
				int end = 0;
				int countSubAttacks = 0;
				
				// Inicializa o contador de subataques concluidos com 0 e a lista de guesses
				resultsPerAttack.put(attackNumber, 0);
				guessesPerAttack.put(attackNumber, new LinkedList<Guess>());
				
				// Delega atividades, dividindo proporcionalmente pelo número de escravos
				while(true) {
					// Incrementa o contador de subattacks gerados para comparacao posterior
					countSubAttacks++;
					
					start = end;
					end = end + m;
					
					// Redistribui o resto da divisão
					if(end >= (lenDict - 1))
					{
						end = end - Math.abs(end - (lenDict - 1));
					}

					
					// Envia mensagem de subattack a fila
					BytesMessage message = (BytesMessage) context.createBytesMessage(); 
					
					try {
						message.setIntProperty("start", start);
						message.setIntProperty("end", end);
						message.setIntProperty("attack", attackNumber);
						message.setIntProperty("length", ciphertext.length);
						message.setIntProperty("length_knowntext", knowntext.length);
						message.writeBytes(ciphertext);
						message.writeBytes(knowntext);
						
					} catch (JMSException e) {
						System.out.println("Problema ao gerar mensagem.");
						e.printStackTrace();
					}
					producer.send(subattacks, message);
					
					end++;
					
					if(end == lenDict)
					{
						break;
					}
				}
				
				
				// Espera ocupada pelos resultados
				while(true)
				{
					if(resultsPerAttack.get(attackNumber) >= countSubAttacks)
					{
						break;
					}
				}

			}
		};
		
		Thread t = new Thread(r);
		t.start();
		
		// Aguarda thread terminar sua tarefa
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Ataque finalizado. Retornando resultado...");
		
		// Converte para Guess[] e envia ao cliente
		List l = guessesPerAttack.get(attackNumber);
		Guess[] a = new Guess[l.size()];
		a = (Guess[]) l.toArray(a);
		
		return a;
	}
	
	public static void main(String[] args) {
		m = (args.length >= 2 ) ? Integer.parseInt(args[1]) : 100;
		
		// Carrega dicionario em array de strings
		try {
			lenDict = Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8).size();
		} catch (Exception e1) {
			System.err.println("Problema na leitura do arquivo");
			System.exit(1);
		}
		
		try {
			obj = new MasterImpl();
			mref = (Master) UnicastRemoteObject.exportObject(obj, 3000); // exporta objeto
			
			// Amarra referência remota ao registry
			Registry registry = LocateRegistry.getRegistry();
			System.err.println("Amarrando mestre...");
			registry.rebind("mestre", mref);
			System.err.println("Mestre pronto");
			
		} catch (Exception e) {
			System.err.println("Não foi possível fazer amarração deste servidor");
			e.printStackTrace();
			System.exit(1);
		}
		
		// Procedimentos de fila
		try {
			Logger.getLogger("").setLevel(Level.SEVERE);
			
			connectionFactory = new com.sun.messaging.ConnectionFactory();
			// connectionFactory.setProperty(ConnectionConfiguration.imqAddressList, host+":7676");	
			
			System.out.println("obtaining queues...");
			subattacks = new com.sun.messaging.Queue("SubAttacksQueue");
			guesses = new com.sun.messaging.Queue("GuessesQueue");

			context = connectionFactory.createContext();
			producer = context.createProducer();
			consumer = context.createConsumer(guesses);
			
			MessageListener listener = new MasterImpl();
			consumer.setMessageListener(listener); 
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onMessage(Message m) {
		// Tipo da mensagem
		String type = "";
		
		try {
			type = m.getStringProperty("type");
		} catch (JMSException e) {
			e.printStackTrace();
		}
		
		if(type.equals("guess"))
		{
			try {
				int length = m.getIntProperty("length");
				int attackNumber = m.getIntProperty("attack");
				String key = m.getStringProperty("key");
				byte[] decrypted = new byte[length];
				((BytesMessage) m).readBytes(decrypted);
				
				
				Guess g = new Guess();
				g.setKey(key);
				g.setMessage(decrypted);
				
				// Adiciona a lista de guesses
				List l = guessesPerAttack.get(attackNumber);
				l.add(g);
			}
			catch (JMSException e) {
				e.printStackTrace();
			}
			
		}
		else if(type.equals("endSignal"))
		{
			try {
				int attackNumber = m.getIntProperty("attack");
				
				// Incrementa o contador de subattacks
				resultsPerAttack.put(attackNumber, resultsPerAttack.get(attackNumber) + 1);
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.out.println("Subattack inesperado");
		}
		
	}

}
