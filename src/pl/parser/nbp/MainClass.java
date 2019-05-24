package pl.parser.nbp;

import java.util.Scanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.util.ArrayList;

public class MainClass {

	static void OutputPrinter(ArrayList<Float> buyData, ArrayList<Float> sellData) {
		String formattedBuyVal = String.format("%.04f", getAverage(buyData));
		String formattedSellVal = String.format("%.04f", getStandDev(sellData));

		System.out.println(formattedBuyVal);
		System.out.println(formattedSellVal);	
	}
	
	public static void urlAccess(String inputData) throws IOException {
		String currency = inputData.substring(0, 3);

		String year_init = inputData.substring(4, 8);
		String month_init = inputData.substring(9, 11);
		String day_init = inputData.substring(12, 14);
		String year_last = inputData.substring(15, 19);
		String month_last = inputData.substring(20, 22);
		String day_last = inputData.substring(23, 25);

		String first_checker = year_init.substring(2, 4) + month_init + day_init;
		String last_checker = year_last.substring(2, 4) + month_last + day_last;
		
		ArrayList<Float> buyData = new ArrayList<Float>();
		ArrayList<Float> sellData = new ArrayList<Float>();
		
		String basic_url = "https://www.nbp.pl/kursy/xml/dir";
		for (int year = Integer.parseInt(year_init); year <= Integer.parseInt(year_last); ++year) {
			URL url = null;
			try {
				if (year != 2019) {
					url = new URL(basic_url + Integer.toString(year) + ".txt");
				} else {
					url = new URL(basic_url + ".txt");
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
			txtReader(url, currency, first_checker, last_checker, buyData, sellData);

		}
		OutputPrinter(buyData, sellData);
	}

	public static void txtReader(URL url, String currency, String start, String end, ArrayList<Float> buyData,
			ArrayList<Float> sellData) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

		String line;
		String subLine;

		String xml_url = null;

		Pattern pattern = Pattern.compile("c[0-9]{3}z[0-9]{6}");
		while ((line = in.readLine()) != null) {

			if (line.length() == 12) {
				line = line.substring(1, 12);
			}

			Matcher matcher = pattern.matcher(line);
			if (matcher.matches()) {
				subLine = line.substring(5, 11);
				if (Integer.parseInt(subLine) >= Integer.parseInt(start)
						&& Integer.parseInt(subLine) <= Integer.parseInt(end)) {

					xml_url = "https://www.nbp.pl/kursy/xml/" + line + ".xml";
					xmlParser(xml_url, currency, buyData, sellData);
				}
			}
		}
		in.close();
	}

	public static void xmlParser(String xml_url, String currency_type, ArrayList<Float> buyData, ArrayList<Float> sellData) {

		String buyRate = null;
		String sellRate = null;
		float buyRateFLOAT = 0;
		float sellRateFLOAT = 0;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new URL(xml_url).openStream());
			NodeList currencyList = doc.getElementsByTagName("pozycja");
			for (int i = 0; i < currencyList.getLength(); ++i) {
				Node c = currencyList.item(i);
				if (c.getNodeType() == Node.ELEMENT_NODE) {
					Element currency = (Element) c;
					String curr_name = currency.getElementsByTagName("kod_waluty").item(0).getTextContent();
					buyRate = currency.getElementsByTagName("kurs_kupna").item(0).getTextContent();
					sellRate = currency.getElementsByTagName("kurs_sprzedazy").item(0).getTextContent();

					DecimalFormatSymbols symbols = new DecimalFormatSymbols();
					symbols.setDecimalSeparator(',');
					DecimalFormat format = new DecimalFormat("0.#");
					format.setDecimalFormatSymbols(symbols);

					try {
						buyRateFLOAT = format.parse(buyRate).floatValue();
						sellRateFLOAT = format.parse(sellRate).floatValue();
					} catch (ParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (curr_name.equals(currency_type)) {
						buyData.add(buyRateFLOAT);
						sellData.add(sellRateFLOAT);
					}
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	static float getAverage(ArrayList<Float> dataList) {
		float sum = 0;
		for (float element : dataList)
			sum += element;
		return (sum / dataList.size());
	}

	static double getStandDev(ArrayList<Float> dataList) {
		float avg = getAverage(dataList);
		float temp = 0;
		for (float element : dataList)
			temp += Math.pow((element - avg), 2);
		return Math.sqrt(temp / dataList.size());
	}

	public static void main(String[] args) throws IOException {

		Scanner userInput = new Scanner(System.in); // Create a Scanner object
		System.out.println("Input data: <currency initial_date last_date [yyyy-mm-dd]>: ");
		String inputData = userInput.nextLine();
		userInput.close();
		 
		urlAccess(inputData);

	}
}
