package pl.parser.nbp;

import java.util.ArrayList;

public interface GlobalCollectionInterface {
	
	public ArrayList<Float> currencyData = null;
	public ArrayList<Float> getData();
	public void addData(float data);
}
