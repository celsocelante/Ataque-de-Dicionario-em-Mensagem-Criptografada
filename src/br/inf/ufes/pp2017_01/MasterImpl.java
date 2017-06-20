package br.inf.ufes.pp2017_01;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class MasterImpl implements Master {
	// Referência (a ser remota) deste objeto
	private static Master mref;
	private static MasterImpl obj;
	// Contador de ataques (usado como attackNumber)
	static int currentAttack = 0;
	// Tamanho total do dicionário
	static int lenDict;
	// UUID -> Slave
	protected static ConcurrentHashMap<UUID, Slave> slaves = new ConcurrentHashMap<UUID, Slave>();
	// UUID -> Slave
	protected static ConcurrentHashMap<UUID, String> slaveNames = new ConcurrentHashMap<UUID, String>();
	// Integer -> Attack
	protected static ConcurrentHashMap<Integer, Attack> attacks = new ConcurrentHashMap<Integer, Attack>();
	

	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		slaves.put(slavekey, s);
		slaveNames.put(slavekey, slaveName);
		System.out.println("Escravo " + slavekey + " registrado");
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		slaves.remove(slaveKey);
		slaveNames.remove(slaveKey);
		
		System.out.println("Escravo " + slaveKey + " removido");
		
		// TODO Iterar sobre todos os ataques e remover indices (?)
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess)
			throws RemoteException {

		Attack a = attacks.get(attackNumber);
		a.addGuess(currentguess);
		a.setCurrentIndex(slaveKey, (int) currentindex);
		
		System.out.println("Escravo " + slaveKey + " enviou um guess para a chave " + currentguess.getKey());
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		Attack a = attacks.get(attackNumber);
		
		a.setCurrentIndex(slaveKey, (int) currentindex);
		
		// Se chegou ao fim, decrementa contador de ativos
		if(a.getLastIndex(slaveKey) <= currentindex)
		{
			System.out.println("Escravo " + slaveKey + " concluiu ataque " + attackNumber);
			a.decrement();
			return;
		}
		
		int time = (int) System.currentTimeMillis() / 1000;
		System.out.println("Checkpoint de " + slaveNames.get(slaveKey) + " na posição " + currentindex + " de "+ a.getLastIndex(slaveKey) + " em " + (time - a.getStartTime()) + "s");
	}


	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
		Attack attack = createAttack();
		
		Runnable r = new Runnable() {		
			public void run() {
				ConcurrentHashMap<UUID, Slave> slavesCopy;
				int numSlaves = 0;
				
				// Protege operação de cópia da lista antes de iterar sobre ela
				synchronized(this) {
					slavesCopy = new ConcurrentHashMap<UUID, Slave>(slaves);
				}
				numSlaves = slavesCopy.values().size();
				
				if(numSlaves == 0)
				{
					System.out.println("Sem escravos registrados");
					System.exit(1);
				}
				
				int lenChunks;
				int remainder;
				int start = 0;
				int end = 0;
				
				lenChunks = lenDict / numSlaves;
				remainder = lenDict % numSlaves;
				
				// Delega atividades, dividindo proporcionalmente pelo número de escravos
				Iterator<Map.Entry<UUID, Slave>> entries = slavesCopy.entrySet().iterator();
				while(entries.hasNext()) {
					Map.Entry<UUID, Slave> entry = entries.next();
					Slave s = entry.getValue();
					UUID key = entry.getKey();
					
					start = end;
					
					// Redistribui o resto da divisão
					if(remainder > 0)
					{
						end = start + (lenChunks);
					}
					else
					{
						end = start + (lenChunks - 1);
					}
					
					try {
						s.startSubAttack(ciphertext, knowntext, start, end, attack.getAttackNumber(), mref);
						// incrementa contador de atacadores corrente
						attack.increment();
						
						// seta start e end do escravo
						attack.setFirstIndex(key, start);
						attack.setLastIndex(key, end);

					} catch (RemoteException e) {
						try {
							removeSlave(key);
						} catch (RemoteException e1) {
							e1.printStackTrace();
						}
						e.printStackTrace();
					}
					end++;
					remainder--;
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
		
		
		// Espera ocupada pelos resultados
		while(true)
		{
			if(attack.getAttackers() <= 0)
			{
				break;
			}
		}
		
		System.out.println("Ataque finalizado. Retornando resultado...");

		return attack.getGuesses();
	}
	
	private Attack createAttack() {
		int id;
		// Incrementa contador de ataques
		synchronized(this) {
			currentAttack++;
			id = currentAttack;
		}
		
		// Cria instância do ataque para controle interno
		Attack a = new Attack(id);
		attacks.put(id, a);
		
		return a;
	}
	
	public static void main(String[] args) {
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
	}

}
