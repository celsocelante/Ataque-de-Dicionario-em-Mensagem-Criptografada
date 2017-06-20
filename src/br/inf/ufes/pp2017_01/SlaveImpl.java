package br.inf.ufes.pp2017_01;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class SlaveImpl implements Slave {
	// Carrega dicionario em array de strings
	static ArrayList<String> dict;
	static Master mestre; // referência remota do mestre
	static UUID id;
	static String nome;
	static SlaveImpl obj;
	static Slave sref; // própria referência remota
	
	public static void findMaster() throws RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry();
		mestre = (Master) registry.lookup("mestre");
	}

	@Override
	public void startSubAttack(byte[] ciphertext, byte[] knowntext, long initialwordindex, long finalwordindex,
		int attackNumber, SlaveManager callbackinterface) throws RemoteException {
		
		// Inicia thread de ataque
		Runnable r = new Runnable() {
			long i = initialwordindex;
			
			// Checkpoint periódico	
			public void run() {
				
				Timer t = new Timer();
				t.schedule(new TimerTask() {
					public void run() {
						try {
							mestre.checkpoint(id, attackNumber, i);
						} catch (Exception e) {
							System.out.println("Problema ao fazer checkpoint");
							//e.printStackTrace();
						}
						
					}
				}, 0, 10000);
				
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
								// Guess atual
								Guess g = new Guess();
								g.setKey(chave);
								g.setMessage(decrypted);
								
								// Devolve guess ao mestre
								callbackinterface.foundGuess(id, attackNumber, i - 1, g);
								System.out.println("Guess enviado por " + id);
								break;
							}
						}
						
					} catch (javax.crypto.BadPaddingException e) {
						//System.out.println("...");
					} catch(Exception e) {
						System.out.println("Problema na decriptografia");	
					}
				}
				
				try {
					mestre.checkpoint(id, attackNumber, i);
				} catch (RemoteException e) {
					//e.printStackTrace();
				}
				
				// Para checkpoint para corrente ataque
				t.cancel();
				
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}
	
	public static void main(String[] args) {
		// Obtem nome da linha de comando
		nome = (args.length >= 2 ) ? args[1] : id.toString();
		
		// Id único gerado para o escravo no momento da inicialização
		id = UUID.randomUUID();
		
		System.out.println("Escravo (" + id + ") iniciado");
		
		if(args.length < 1) {
			System.err.println("Uso: programa caminho-nome-do-dicionário");
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
		
		
		try {
			// Remote reference
			obj = new SlaveImpl();
			sref = (Slave) UnicastRemoteObject.exportObject(obj, 0);
		} catch (RemoteException e1) {
			System.out.println("Problema ao criar objeto remoto");
			//e1.printStackTrace();
			System.exit(1);
		}

		// Tarefa de registro do escravo (repetida a cada 3 segundos)
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				System.out.println("Registrando no mestre (" + id + ")");
				try {
					findMaster();
					mestre.addSlave(sref, nome, id);
				} catch (Exception e) {
					System.out.println("Problema ao registrar. Tentando novo contato em 30s...");
					//e.printStackTrace();
				}
				
			}
		}, 0, 30000);
		
	}

}
