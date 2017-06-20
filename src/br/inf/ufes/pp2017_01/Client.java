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

public class Client {

	public static void main(String[] args) {
		Random rand = new Random();
		
		String fileName = args[0];
		String knowntext = args[1];
		int arraySize = (args.length >= 3 ) ? Integer.parseInt(args[2]) : rand.nextInt((100000 - 1000) + 1) + 1000;
		
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
			
			// Efetivamente chama o método de ataque do mestre
			Guess[] g = mestre.attack(data, knowntext.getBytes());
			
			System.out.println("Resultado do ataque recebido!");
			// TODO salvar em array de saída
			
		} catch (Exception e) {
			System.err.println("Não foi possível me conectar ao mestre");
			e.printStackTrace();
			System.exit(1);
		}
		
		
	}

}
