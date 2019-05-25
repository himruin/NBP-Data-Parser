package pl.parser.nbp;

import java.util.ArrayList;

public final class GlobalCollection implements GlobalCollectionInterface {

	private ArrayList<Float> currencyData = new ArrayList<Float>();
	
	public void addData(float data) {
		this.currencyData.add(data);
	}

	public ArrayList<Float> getData() {
		return this.currencyData;
	}
}
