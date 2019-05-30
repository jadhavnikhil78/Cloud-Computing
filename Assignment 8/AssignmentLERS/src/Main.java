import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class Main {
	static Scanner input;
	
	public static List<String> attributeNames = new ArrayList<String>();
	public static List<String> stableAttributes = new ArrayList<String>();
	public static List<String> flexibleAttributes = new ArrayList<String>();
	
	public static String decisionAttribute,decisionFrom,decisionTo;
	public static String outputData = "";
	
	
	public static ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>(); 
	
	static Map<String, HashSet<String>> distinctAttributeValues = new HashMap<String, HashSet<String>>();
	static Map<HashSet<String>, HashSet<String>> attributeValues = new HashMap<HashSet<String>, HashSet<String>>();
	static Map<HashSet<String>, HashSet<String>> reducedAttributeValues = new HashMap<HashSet<String>, HashSet<String>>();
	static Map<String, HashSet<String>> decisionValues = new HashMap<String, HashSet<String>>();
	static Map<ArrayList<String>, HashSet<String>> markedValues = new HashMap<ArrayList<String>, HashSet<String>>();
	public static Map<ArrayList<String>,String> certainRules = new HashMap<ArrayList<String>,String>();
	public static Map<ArrayList<String>,HashSet<String>> possibleRules = new HashMap<ArrayList<String>,HashSet<String>>();
	
	
	public static void main(String[] args) throws IOException {
		//Read the attribute names
		readAttributes();
		
		//Read data
		readData();
		
		//Read stable,flexible and decision attributes
//		setStableAttributes(attributeNames);
		setDecisionAttribute(attributeNames);
		
		//Find Certain and Possible rules
		findRules();
		
		// Saving the output file
		saveOutput();

	}

	public static void saveOutput() throws IOException{
//		System.out.println("Printing file contents....");
//		System.out.println(content);
		File file = new File("Output.txt");
		
		FileWriter fileWriter = new FileWriter(file);
		fileWriter.write(outputData);
		fileWriter.close();
	}
	
	public static void storeData(String content){
		outputData += content + "\n";
	}
	
	//Printing String, List and Map
	public static void printMessage(String content){
		System.out.println(content);
		storeData(content);
	}
	
//	Writing Printing content to a file
	
	public static void printList(List<String> list){
		Iterator iterate = list.iterator();
		
		while(iterate.hasNext()){
			printMessage(iterate.next().toString());
		}
	}
	
	private static void printAttributeMap(Map<HashSet<String>, HashSet<String>> values) {
		for(Map.Entry<HashSet<String>, HashSet<String>> set : values.entrySet()){
			printMessage(set.getKey().toString() + " = " + set.getValue());
		}
	}
	
	private static void printCertainRulesMap(Map<ArrayList<String>, String> value) {
		printMessage("\nCertain Rules:");
		for(Map.Entry<ArrayList<String>,String> set : value.entrySet()){
			int support = calculateSupportLERS(set.getKey(),set.getValue());
			String confidence = calculateConfidenceLERS(set.getKey(),set.getValue());
			
			printMessage(set.getKey().toString() + " -> " + set.getValue() + "[Support:-" + support + ", Confidence:-" + confidence +"%]");
			
		}
	}
	
	private static void printPossibleRulesMap(Map<ArrayList<String>, HashSet<String>> value) {
		
		if(!value.isEmpty()){
			printMessage("\nPossible Rules:");
			for(Map.Entry<ArrayList<String>,HashSet<String>> set : value.entrySet()){
				for(String possibleValue:set.getValue()){
					int support = calculateSupportLERS(set.getKey(),possibleValue);
					String confidence = calculateConfidenceLERS(set.getKey(),possibleValue);
					
					printMessage(set.getKey().toString() + " -> " + possibleValue + "[Support:-" + support + ", Confidence:-" + confidence +"%]");
				}

			}
		}
	}

	private static int findLERSSupport(ArrayList<String> tempList) {
		int count = 0;
		
		for(ArrayList<String> data : data){	
			if(data.containsAll(tempList))
				count++;
		}
		
		return count;
	}
	
	private static int calculateSupportLERS(ArrayList<String> key, String value) {
		ArrayList<String> tempList = new ArrayList<String>();
		
		for(String val : key){
			tempList.add(val);
		}
		
		if(!value.isEmpty())
			tempList.add(value);
	
		return findLERSSupport(tempList);
		
	}


	private static String calculateConfidenceLERS(ArrayList<String> key,
			String value) {
		int num = calculateSupportLERS(key, value);
		int den = calculateSupportLERS(key, "");
		double confidence = 0.0;
		
		if(den != 0)
			confidence = (num * 100)/den;
//		int confidence = (num * 100)/den;
		
		return String.valueOf(confidence);
	}
	
	//Reading attributes and data
	private static void readAttributes() {
		try {
			input = new Scanner(new File("attribute.txt"));
			
			while (input.hasNext()) {
				attributeNames.add(input.next());		
			}
			
		} catch (FileNotFoundException e) {
			printMessage("File Not Found");
			e.printStackTrace();
		}
		
	}
	
	private static void readData() {
		try {
			input = new Scanner(new File("data.txt"));
			int lineNo = 0;
			
			while(input.hasNextLine()){
				String[] lineData = input.nextLine().split("\\s+");
				String key;
				
				lineNo++;
				ArrayList<String> tempList = new ArrayList<String>();
				HashSet<String> set;
				
				for (int i=0;i<lineData.length;i++) {
					String currentAttributeValue = lineData[i];
					String attributeName = attributeNames.get(i);
					key = attributeName + currentAttributeValue;
					
					tempList.add(key);

					HashSet<String> mapKey = new HashSet<String>();
					mapKey.add(key);
					setMap(attributeValues,lineData[i],mapKey,lineNo);
					
					if (distinctAttributeValues.containsKey(attributeName)) {
						set = distinctAttributeValues.get(attributeName);
						set.add(key);
						
					}else{
						set = new HashSet<String>();
					}
					
					set.add(key);
					distinctAttributeValues.put(attributeName, set);
				}
		
				data.add(tempList);
			}
			
			
		} catch (FileNotFoundException e) {
			printMessage("File Not Found");
			e.printStackTrace();
		}
	}

	private static void setMap(Map<HashSet<String>, HashSet<String>> values,
			String string, HashSet<String> key, int lineNo) {
		HashSet<String> tempSet;
		
		if (values.containsKey(key)) {
			tempSet = values.get(key);						
		}else{
			tempSet = new HashSet<String>();
		}
		
		tempSet.add("x"+lineNo);
		values.put(key, tempSet);
	}

	private static void setStableAttributes(List<String> attributes) {
		printMessage("\nPlease Give a choice\n1.Enter Stable Attributes...\n2.Go next...");
		
		input = new Scanner(System.in);
		int choice = input.nextInt();
		if(choice==1){
			printMessage("\nAttributes Available");
			printMessage("--------------------------");
			printList(attributes);
			printMessage("Enter a stable attribute name...");
			
			String userStableAttribute = input.next();
			if(checkValid(attributes,userStableAttribute)){
				stableAttributes.addAll(distinctAttributeValues.get(userStableAttribute));
				attributes.remove(userStableAttribute);
			}else{
				printMessage("Invalid Attribute name...\n");
			}
			
			setStableAttributes(attributes);
		}
		
		else if(choice==2){
			setDecisionAttribute(attributes);
		}
		
		else{
			printMessage("\nPlease Enter 1 or 2");
			setStableAttributes(attributes);
		}
	}

	private static boolean checkValid(List<String> attributes,String userStableAttribute) {
		if(attributes.contains(userStableAttribute))
			return true;
		else return false;
	}

	private static void setDecisionAttribute(List<String> attributes) {
		printMessage("\nAttributes Available");
		printMessage("--------------------------");
		printList(attributes);
		printMessage("Type a decision attribute name and Press ENTER...");
		
		input = new Scanner(System.in);
		decisionAttribute = input.next();
		
		if (checkValid(attributes,decisionAttribute)) {
			storeData(decisionAttribute);
			attributes.remove(decisionAttribute);
			flexibleAttributes = attributes;
			
			HashSet<String> decisionValues = distinctAttributeValues.get(decisionAttribute);
			removeDecisionValueFromAttributes(decisionValues);
			
		}else{
			printMessage("Invalid attrbibute.");
			setDecisionAttribute(attributes);
		}
		
	}

	private static void removeDecisionValueFromAttributes(HashSet<String> decisionValues) {
		for(String value : decisionValues){
			HashSet<String> newHash = new HashSet<String>();
			newHash.add(value);
			Main.decisionValues.put(value, attributeValues.get(newHash));
			attributeValues.remove(newHash);
		}
	}

	private static void findRules() {
		int loopCount = 0;
		
		printMessage("\nLoop " + (++loopCount) +":");
		printMessage("--------------------------");
		printAttributeMap(attributeValues);
		
		while(!attributeValues.isEmpty()){			
			for (Map.Entry<HashSet<String>, HashSet<String>> set : attributeValues.entrySet()) {
				ArrayList<String> setKey = new ArrayList<String>();
				setKey.addAll(set.getKey());
				
				if (set.getValue().isEmpty()) {
					continue;
				}else{
					for(Map.Entry<String, HashSet<String>> decisionSet : decisionValues.entrySet()){
						if(decisionSet.getValue().containsAll(set.getValue())){
							certainRules.put(setKey, decisionSet.getKey());
							markedValues.put(setKey, set.getValue());
							break;
						}
					}
				}
				
				if(!markedValues.containsKey(setKey)){
					HashSet<String> possibleRulesSet = new HashSet<String>();
					for(Map.Entry<String, HashSet<String>> decisionSet : decisionValues.entrySet()){
						possibleRulesSet.add(decisionSet.getKey());
					}
					possibleRules.put(setKey, possibleRulesSet);
				}
				
			}
			
			removeMarkedValues();
			
			printCertainRulesMap(certainRules);
			printPossibleRulesMap(possibleRules);
			
			printMessage("\nLoop " + (++loopCount) +":");
			printMessage("--------------------------");
			
			combinePossibleRules();
		}
	}

	private static void removeMarkedValues() {
		for(Map.Entry<ArrayList<String>, HashSet<String>> markedSet : markedValues.entrySet()){
			attributeValues.remove(new HashSet<String>(markedSet.getKey()));
		}
		
	}
	
	private static void combinePossibleRules() {
		Set<ArrayList<String>> keySet = possibleRules.keySet();
		ArrayList<ArrayList<String>> keyList = new ArrayList<ArrayList<String>>();
		keyList.addAll(keySet);
		
		for(int i = 0;i<possibleRules.size();i++){
			for(int j = (i+1);j<possibleRules.size();j++){
				HashSet<String> combinedKeys = new HashSet<String>(keyList.get(i));
				combinedKeys.addAll(new HashSet<String>(keyList.get(j)));
				
				if(!checkSameGroup(combinedKeys)){
					combineAttributeValues(combinedKeys);
				}
			}
		}
		
		possibleRules.clear();
		
		printAttributeMap(reducedAttributeValues);
		
		removeRedundantValues();		
		clearAttributeValues();
		
	}

	private static boolean checkSameGroup(HashSet<String> combinedKeys) {
		ArrayList<String> keyList = new ArrayList<String>();

		for(String combinedKey : combinedKeys){
			for(Map.Entry<String, HashSet<String>> singleAttribute : distinctAttributeValues.entrySet()){
				if(singleAttribute.getValue().contains(combinedKey)){
					if(keyList.contains(singleAttribute.getKey())){
						return true;
					}else{
						keyList.add(singleAttribute.getKey());
					}
				}
				else{
					continue;
				}
			}
		}
		
		return false;
	}
	
	private static void combineAttributeValues(HashSet<String> combinedKeys) {
		HashSet<String> combinedValues = new HashSet<String>();
			
		for(Map.Entry<HashSet<String>, HashSet<String>> attributeValue : attributeValues.entrySet()){
			if(combinedKeys.containsAll(attributeValue.getKey())){
				if(combinedValues.isEmpty()){
					combinedValues.addAll(attributeValue.getValue());
				}else{
					combinedValues.retainAll(attributeValue.getValue());
				}
			}
		}
		reducedAttributeValues.put(combinedKeys, combinedValues);
	
	}

	private static void removeRedundantValues() {
		HashSet<HashSet<String>> mark = new HashSet<HashSet<String>>();
		
		for(Map.Entry<HashSet<String>, HashSet<String>> reducedAttributeValue : reducedAttributeValues.entrySet()){
			for(Map.Entry<ArrayList<String>, String> attributeValue : certainRules.entrySet()){
				
//				System.out.println("Checking " + String.join(",", reducedAttributeValue.getKey()) + " and " + String.join(",", attributeValue.getKey()) + " : " + reducedAttributeValue.getKey().containsAll(attributeValue.getKey()));
				
				if(reducedAttributeValue.getKey().containsAll(attributeValue.getKey()) || reducedAttributeValue.getValue().isEmpty()){
					printMessage(reducedAttributeValue.getKey().toString() + " is already marked!");
					
					mark.add(reducedAttributeValue.getKey());
				}
			}
		}
		
		for(HashSet<String> toRemove : mark){
			reducedAttributeValues.remove(toRemove);
		}
		
		
	}
	
	private static void clearAttributeValues() {
		 attributeValues.clear();
		 for(Map.Entry<HashSet<String>, HashSet<String>> reducedAttributeValue : reducedAttributeValues.entrySet()){
			 attributeValues.put(reducedAttributeValue.getKey(), reducedAttributeValue.getValue());
		 }
		 reducedAttributeValues.clear();
	}

}
