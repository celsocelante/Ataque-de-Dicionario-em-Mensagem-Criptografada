package br.inf.ufes.pp2017_01;

import java.rmi.RemoteException;
import java.util.UUID;
import java.util.List;
import java.util.LinkedList;

public class MasterImpl implements Master {
	List<Slave> slaves = new LinkedList<Slave>();

	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
