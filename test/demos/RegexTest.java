package demos;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {

	
	/**
	 * Regex pattern to identify http error codes from the title of the returned document:
	 * 
	 * <code>
	 * Matcher m = httpErrorPattern.matcher("[..] <title>503 Service Unavailable</title> [..]");
	 * if (m.matches()) {
	 * 		System.out.println("HTTP Error: " + m.group(1);
	 * }
	 * </code>
	 */
	protected static Pattern httpErrorPattern = Pattern.compile(".*<title>(.*)</title>.*", Pattern.DOTALL);
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		String s = "hello<title>503 Service unavailable</title>bjk";
		s = "\n<head><title>502 Proxy Error</title></head>";
		
		Matcher m = httpErrorPattern.matcher(s);
		
		if (m.matches()) {
			System.out.println("Matches!");
			System.out.println("Group: " + m.group(1));
		} else {
			System.out.println("No error");
		}

	}

}
