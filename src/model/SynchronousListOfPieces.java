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

	public Piece getIndex(int index) {
		return list.get(index);
	}

	public synchronized void remove(int index) {
		list.remove(index);
	}
}