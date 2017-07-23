package br.inf.ufes.pp2017_01;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.NotBoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.io.FileOutputStream;

public class Client {
	private static int startTime;
	private static int endTime;

	public static void main(String[] args) {
		Random rand = new Random();
		
		String fileName = args[0];
		String knowntext = args[1];
		
		int arraySize = (args.length >= 3 ) ? Integer.parseInt(args[2]) : rand.nextInt((100000 - 1000) + 1) + 1000;
		arraySize = arraySize + (arraySize % 8); // faz o tamanho ser múltiplo de 8
		
		byte[] data; // conteúdo a ser decriptografado
		
		try {
			Path path = Paths.get(fileName);
			data = Files.readAllBytes(path);
		} catch (Exception e) {
			// Caso de problema na leitura do arquivo passado
			System.out.println("Gerando arquivo aleatório...");
			data = new byte[arraySize];
			
			// Gera arquivo de entrada aleatório
			try {
				SecureRandom.getInstanceStrong().nextBytes(data);
			} catch (NoSuchAlgorithmException e1) {
				e1.printStackTrace();
			}
			
			// Arquivo de saída
			Path output = Paths.get("entrada-" + arraySize + ".txt");
			try {
				Files.write(output, data);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		
		// Invoca mestre
		try {
			Registry registry = LocateRegistry.getRegistry(null);
			Master mestre = (Master) registry.lookup("mestre");
			
			System.out.println("Iniciando ataque...");
			
			startTime =  (int) System.currentTimeMillis() / 1000;
			
			// Efetivamente chama o método de ataque do mestre
			Guess[] g = mestre.attack(data, knowntext.getBytes());
			
			endTime =  (int) System.currentTimeMillis() / 1000;
			
			if(g.length > 0)
			{
				System.out.println("Resultado do ataque recebido, salvando em arquivo");
				
				for(Guess guess: g) {
					// Escreve saída em arquivos com mensagens decriptografadas
					FileOutputStream fos = new FileOutputStream(guess.getKey() + ".msg");
					fos.write(guess.getMessage());
					fos.close(); 
				}
				
			}
			else
			{
				System.out.println("Resultado recebido, mas nenhuma chave potencial foi encontrada");
			}
			
			System.out.println("O ataque levou " + (endTime - startTime) + "s");
			
		} catch (Exception e) {
			System.err.println("Não foi possível me conectar ao mestre");
			e.printStackTrace();
			System.exit(1);
		}
		
		
	}

}
