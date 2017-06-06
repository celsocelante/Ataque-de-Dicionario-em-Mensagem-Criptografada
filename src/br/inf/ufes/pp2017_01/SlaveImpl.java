package br.inf.ufes.pp2017_01;

import java.rmi.RemoteException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;

public class SlaveImpl implements Slave {
	// Carrega dicionario em array de strings
	static ArrayList<String> dict;

	@Override
	public void startSubAttack(byte[] ciphertext, byte[] knowntext, long initialwordindex, long finalwordindex,
		int attackNumber, SlaveManager callbackinterface) throws RemoteException {
		
		// Inicia thread de ataque
		Runnable r = new Runnable() {
			public void run() {
				for(long i = initialwordindex; i < finalwordindex; i++) {
					String chave;
					chave = dict.get((int) i);
					
					try {
						// Procedimentos de decriptografia
						SecretKeySpec keySpec = new SecretKeySpec(chave.getBytes(), "Blowfish");
						Cipher cipher = Cipher.getInstance("Blowfish");
						cipher.init(Cipher.DECRYPT_MODE, keySpec);
						byte[] decrypted = cipher.doFinal(ciphertext);
						
					} catch(Exception e) {
						System.out.println("Problema na decriptografia");	
					}
				}
				
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}
	
	public static void main(String[] args) {
		
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
		
		// Lookup do mestre

		// Inicia tarefa de registro
		Timer t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				System.out.println("Registrando no mestre...");
			}
		}, 0, 3000);
		
	}

}
