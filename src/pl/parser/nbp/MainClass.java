package pl.parser.nbp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainClass {

	private static final GlobalCollection buyData = new GlobalCollection();
	private static final GlobalCollection sellData = new GlobalCollection();

	private static final String[] CURRENCIES = { "USD", "EUR", "CHF", "GBP" };
	private static final String DATA_FILE_PATTERN = "c[0-9]{3}z[0-9]{6}";
	private static final String NBP_URL_TXT = "https://www.nbp.pl/kursy/xml/dir";
	private static final String NBP_URL_XML = "https://www.nbp.pl/kursy/xml/";
	private static final String TXT_EXTENSION = ".txt";
	private static final String XML_EXTENSION = ".xml";

	private static void urlAccess(String currency, String inputFirstDate, String inputSecondDate) throws IOException {
		
		String yearInit = inputFirstDate.substring(0, 4);
		String monthInit = inputFirstDate.substring(5, 7);
		String dayInit = inputFirstDate.substring(8, 10);
		String yearLast = inputSecondDate.substring(0, 4);
		String monthLast = inputSecondDate.substring(5, 7);
		String dayLast = inputSecondDate.substring(8, 10);

		String dateInit = yearInit.substring(2, 4) + monthInit + dayInit;
		String dateLast = yearLast.substring(2, 4) + monthLast + dayLast;

		if (isInputCorrect(dateInit, dateLast, currency)) {
			for (int year = Integer.parseInt(yearInit); year <= Integer.parseInt(yearLast); ++year) {
				URL url = null;
				try {
					if (isCurrentYear(year)) {
						url = new URL(NBP_URL_TXT + TXT_EXTENSION);
					} else {
						url = new URL(NBP_URL_TXT + Integer.toString(year) + TXT_EXTENSION);
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}

				txtReader(url, currency, dateInit, dateLast);
			}
			OutputPrinter();
		}
	}

	private static void txtReader(URL url, String currency, String start, String end) throws IOException {
		BufferedReader bReader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));

		String xmlCode = null;
		String currentDate = null;
		String xmlURL = null;

		Pattern pattern = Pattern.compile(DATA_FILE_PATTERN);
		while ((xmlCode = bReader.readLine()) != null) {

			// in the first line of each txt file the special sign at the beginning has to be removed
			if (xmlCode.length() == 12) {
				xmlCode = xmlCode.substring(1, 12);
			}

			Matcher matcher = pattern.matcher(xmlCode);
			if (matcher.matches()) {

				// currentDate is taken from the part of the xml code in txt file instead of
				// "data_publikacji" term in xml file (dates are the same)
				currentDate = xmlCode.substring(5, 11);

				if (Integer.parseInt(currentDate) >= Integer.parseInt(start)
						&& Integer.parseInt(currentDate) <= Integer.parseInt(end)) {

					xmlURL = NBP_URL_XML + xmlCode + XML_EXTENSION;
					xmlParser(xmlURL, currency);// buyData, sellData);
				}
			}
		}
		bReader.close();
	}

	private static void xmlParser(String xml_url, String currency_type) {
		String buyRate = null;
		String sellRate = null;
		String currencyCode = null;
		float buyRateFLOAT = 0;
		float sellRateFLOAT = 0;

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {

			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			Document xmlDoc = docBuilder.parse(new URL(xml_url).openStream());
			NodeList currencyList = xmlDoc.getElementsByTagName("pozycja");
			for (int i = 0; i < currencyList.getLength(); ++i) {
				Node c = currencyList.item(i);
				if (c.getNodeType() == Node.ELEMENT_NODE) {
					Element currency = (Element) c;
					currencyCode = currency.getElementsByTagName("kod_waluty").item(0).getTextContent();
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

					if (currencyCode.equals(currency_type)) {
						buyData.addData(buyRateFLOAT);
						sellData.addData(sellRateFLOAT);
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

	static void OutputPrinter() {
		float averageBuyData = getAverage(buyData.getData());
		double standDevSellData = getStandDev(sellData.getData());

		String formattedBuyValue = String.format("%.04f", averageBuyData);
		String formattedSellValue = String.format("%.04f", standDevSellData);

		System.out.println(formattedBuyValue);
		System.out.println(formattedSellValue);
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

	static boolean isInputCorrect(String dateInit, String dateLast, String currency) {
		Date date = new Date();
		String currentDate = new SimpleDateFormat("yyMMdd").format(date);
		List<String> availableCurrencies = Arrays.asList(CURRENCIES);
		boolean isCurrencyOk = availableCurrencies.contains(currency);

		if (dateLast.compareTo(currentDate) <= 0 && dateLast.compareTo(dateInit) > 0 && isCurrencyOk) {
			return true;
		} else {
			System.out.println("WRONG DATES OR CURRENCY!");
			return false;
		}
	}

	static boolean isCurrentYear(int year) {
		String yearStr = String.valueOf(year);
		Date date = new Date();
		String currentYear = new SimpleDateFormat("yyyy").format(date);

		return (yearStr.compareTo(currentYear) == 0) ? true : false;
	}

	public static void main(String[] inputData) throws IOException {

		urlAccess(inputData[0], inputData[1], inputData[2]);
	}
}
