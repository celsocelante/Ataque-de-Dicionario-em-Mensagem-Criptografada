package br.inf.ufes.pp2017_01;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.spec.SecretKeySpec;
import javax.jms.*;
import javax.crypto.Cipher;

public class SlaveImpl implements Slave {
	// Carrega dicionario em array de strings
	static ArrayList<String> dict;
	static String nome;
	static UUID id;
	
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

	public static void startSubAttack(byte[] ciphertext, byte[] knowntext, long initialwordindex, long finalwordindex,
		int attackNumber) throws JMSException {
		
		long i = initialwordindex;
		for(i = initialwordindex; i < finalwordindex; i++) {
				String chave;
				chave = dict.get((int) i);
				
				System.out.println("Ataque " + attackNumber + " na chave " + i + " (" + chave + ")");
				
				try {
					// Procedimentos de decriptografia
					SecretKeySpec keySpec = new SecretKeySpec(chave.getBytes(), "Blowfish");
					Cipher cipher = Cipher.getInstance("Blowfish");
					cipher.init(Cipher.DECRYPT_MODE, keySpec);
					
					byte[] decrypted;
					decrypted = cipher.doFinal(ciphertext);
					boolean hit = false;
					
					for(int j = 0; j < decrypted.length; j++) {
						
						for(int k = 0; k < knowntext.length; k++) {
							if(decrypted[j + k] != knowntext[k]) {
								hit = false;
								break;
							}
							
							hit = true;
						}
						
						if(hit)
						{
							BytesMessage guess = (BytesMessage) context.createBytesMessage();
							guess.setStringProperty("type", "guess");
							guess.setStringProperty("key", chave);
							guess.setIntProperty("attack", attackNumber);
							guess.setIntProperty("length", decrypted.length);
							guess.writeBytes(decrypted);
							
							producer.send(guesses, guess);
							
							System.out.println("Guess enviado por ao mestre");
							break;
						}
					}
					
				} catch (javax.crypto.BadPaddingException e) {
					//System.out.println("...");
				} catch(Exception e) {
					System.out.println("Problema na decriptografia");	
				}
			}
		
		// Indica fim do subattack
		Message signal = context.createMessage();
		signal.setStringProperty("type", "endSignal");
		signal.setIntProperty("attack", attackNumber);
		
		producer.send(guesses, signal);
		System.out.println("Subattack concluido");
	}
	
	public static void main(String[] args) throws JMSException {
		// Id único gerado para o escravo no momento da inicialização
		id = UUID.randomUUID();
				
		// Obtem nome da linha de comando
		nome = (args.length >= 2 ) ? args[1] : id.toString();
		
		System.out.println("Escravo (" + nome + ") iniciado");
		
		if(args.length < 1) {
			System.err.println("Uso: programa caminho-do-dicionario");
			System.exit(1);
		}
		
		try {
			// Carrega dicionario em array de strings
			dict = (ArrayList<String>) Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("Problema na leitura do arquivo");
			e.printStackTrace();
			System.exit(1);
		}
		
		// Procedimentos de fila
		try {
			Logger.getLogger("").setLevel(Level.SEVERE);

			connectionFactory = new com.sun.messaging.ConnectionFactory();
			// connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,host+":7676");	
					
			subattacks = new com.sun.messaging.Queue("SubAttacksQueue");
			guesses = new com.sun.messaging.Queue("GuessesQueue");
			
			context = connectionFactory.createContext();
			consumer = context.createConsumer(subattacks);

			producer = context.createProducer();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Polling na fila de subattacks
		while(true) {
			Message m = consumer.receive();
			
			if(m instanceof BytesMessage)
			{
				int start = (int) m.getIntProperty("start");
				int end = (int) m.getIntProperty("end");
				
				System.out.println(start);
				System.out.println(end);
				
				
				int attackNumber = (int) m.getIntProperty("attack");
				System.out.println(attackNumber);

				int length_kt = (int) m.getIntProperty("length_knowntext");
				int length = (int) m.getIntProperty("length");
				byte[] ciphertext = new byte[length];
				byte[] knowntext = new byte[length_kt];
				
				((BytesMessage) m).readBytes(ciphertext, length);
				((BytesMessage) m).readBytes(knowntext, length_kt);
				
				// Inicia subattack com mensagem da fila
				startSubAttack(ciphertext, knowntext, start, end, attackNumber);
			}
			
		}
		
	}

}
