package model;
import java.util.*;

public class SynchronousListOfPieces {

	public List<Piece> list;

	public SynchronousListOfPieces(List list) {
		this.list = list;
	}

	public synchronized void add(Piece piece) {
		list.add(piece);
	}

	public synchronized void addToHead(Piece piece) {
		list.add(0, piece);
	}

	public Piece getIndex(int index) {
		return list.get(index);
	}

	public synchronized void remove(int index) {
		list.remove(index);
	}

	// Retrieve and remove list's head
	public synchronized Piece poll() {
		Piece piece = list.get(0);
		list.remove(0);
		return piece;
	}

	public synchronized int size() {
		return list.size();
	}

	@Override
	public String toString() {
		String s = "";
		ListIterator<Piece> iterator = list.listIterator();
		while (iterator.hasNext())
			s += iterator.next().toString();

		return s;
	}
}