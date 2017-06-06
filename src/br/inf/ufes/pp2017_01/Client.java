package br.inf.ufes.pp2017_01;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

	public static void main(String[] args) {
		/*String host = (args.length < 1) ? null : args[0];
		String host = (args.length < 1) ? null : args[0];
		String host = (args.length < 1) ? null : args[0];*/
		
		try {
			Registry registry = LocateRegistry.getRegistry(null);
			MasterImpl master = (MasterImpl) registry.lookup("mestre");

			// Chamar função do mestre
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}

	}

}
