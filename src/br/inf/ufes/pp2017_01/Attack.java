package br.inf.ufes.pp2017_01;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

public class Attack {
	private int attackNumber;
	
	// UUID -> Integer
	private ConcurrentHashMap<UUID, Integer> slavesFirstIndex;
	private ConcurrentHashMap<UUID, Integer> slavesCurrentIndex;
	private ConcurrentHashMap<UUID, Integer> slavesLastIndex;
	private List<Guess> guesses;
	private int current = 0;
	private int startTime;
	
	
	public Attack(int attackNumber) {
		this.attackNumber = attackNumber;
		slavesFirstIndex = new ConcurrentHashMap<UUID, Integer>();
		slavesCurrentIndex = new ConcurrentHashMap<UUID, Integer>();
		slavesLastIndex = new ConcurrentHashMap<UUID, Integer>();
		guesses = new LinkedList<Guess>();
		current = 0;
		startTime = (int) System.currentTimeMillis() / 1000;
	}
	
	public Guess[] getGuesses() {
		Guess[] g;
		synchronized(this) {
			g = guesses.toArray(new Guess[guesses.size()]);
		}
		return g;
	}
	
	public void setCurrentIndex(UUID id, int currentIndex) {
		synchronized(this) {
			slavesCurrentIndex.put(id, currentIndex);
		}
	}
	
	public int getCurrentIndex(UUID id) {
		int index;
		synchronized(this) {
			index = slavesCurrentIndex.get(id);
		}
		return index;
	}
	
	public void setLastIndex(UUID id, int lastIndex) {
		synchronized(this) {
			slavesLastIndex.put(id, lastIndex);
		}
	}
	
	public int getLastIndex(UUID id) {
		int index;
		synchronized(this) {
			index = slavesLastIndex.get(id);
		}
		return index;
	}
	
	public void setFirstIndex(UUID id, int firstIndex) {
		synchronized(this) {
			slavesFirstIndex.put(id, firstIndex);
		}
	}
	
	public int getFirstIndex(UUID id) {
		int index;
		synchronized(this) {
			index = slavesFirstIndex.get(id);
		}
		return index;
	}
	
	public void addGuess(Guess g) {
		synchronized(this) {
			guesses.add(g);
		}
	}
	
	public int getAttackNumber() {
		return attackNumber;
	}
	
	public void increment() {
		synchronized(this) {
			current = current + 1;
		}
	}
	
	public void decrement() {
		synchronized(this) {
			current = current - 1;
		}
	}
	
	public int getAttackers() {
		synchronized(this) {
			return current;
		}
	}
	
	public int getStartTime() {
		return startTime;
	}

}
