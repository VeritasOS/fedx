package demos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternMatch {

	
	
	static String message = "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n<html><head><title>502 Proxy Error</title></head><body>";
	protected static Pattern httpErrorPattern = Pattern.compile(".*<title>(.*)</title>.*", Pattern.DOTALL);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {


		Matcher m = httpErrorPattern.matcher(message);
		if (m.matches()) {
			message = "HTTP Error: " + m.group(1);
		} else {
			System.out.println("No http error found");
		}

		System.out.println("Message: " + message);
	}

}
