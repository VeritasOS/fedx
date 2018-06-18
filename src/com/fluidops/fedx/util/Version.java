/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.util;

import java.net.URI;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.log4j.Logger;


/**
 * Version utility: read the version from the Jar's manifest file.
 * 
 * @author Andreas Schwarte
 *
 */
public class Version {

	protected static Logger log = Logger.getLogger(Version.class);
	
	/* fields with default values (i.e. if not started from jar) */
	protected static String project = "FedX";
	protected static String date = "88.88.8888";
	protected static String longVersion = "8.8 (build 8888)";
	protected static String build = "8888";
	protected static String version = "FedX 8.8";
	protected static String contact = "info@fluidops.com";
	protected static String companyName = "fluid Operations AG";
	protected static String productName = "fluid FedX";
	
    
	static {
		
		try {
			String jarPath = getJarPath();
			
			if (jarPath!=null) {
				
				JarFile jar = new JarFile(jarPath);
				
				Manifest buildManifest = jar.getManifest();
	    	    if(buildManifest!=null) {
	    	    	project = buildManifest.getMainAttributes().getValue("project");
	    	    	date = buildManifest.getMainAttributes().getValue("date");
	    	        longVersion = buildManifest.getMainAttributes().getValue("version");
	    	        build =  buildManifest.getMainAttributes().getValue("build");		// roughly svn version
	    	        version = buildManifest.getMainAttributes().getValue("ProductVersion");
	    	        contact =  buildManifest.getMainAttributes().getValue("ProductContact");  	       
	    	        companyName = buildManifest.getMainAttributes().getValue("CompanyName");
	    	        productName = buildManifest.getMainAttributes().getValue("ProductName");
	    	    }
	    	    
	    	    jar.close();
			}
		} catch (Exception e) {
			log.warn("Error while reading version from jar manifest.", e);
			; 	// ignore
		}
	}
	
	protected static String getJarPath() {

		URL url = Version.class.getResource("/com/fluidops/fedx/util/Version.class");
		String urlPath = url.getPath();
		// url is something like file:/[Pfad_der_JarFile]!/[Pfad_der_Klasse] 
		
		// not a jar, e.g. when started from eclipse
		if (!urlPath.contains("!")) {
			return null;
		}
		
		try {
			URI uri = new URI(url.getPath().split("!")[0]);
			return uri.getPath();
		} catch (Exception e) {
			log.warn("Error while retrieving jar path", e);
			return null;
		}
	}
	
	/**
	 * @return
	 * 		the version string, i.e. 'FedX 1.0 alpha (build 1)'
	 */
	public static String getVersionString() {
		return project + " " + longVersion;
	}
	
	/**
	 * print information to Stdout
	 */
	public static void printVersionInformation() {
		System.out.println("Version Information: " + project + " " + longVersion);
	}


	
	public static String getProject() {
		return project;
	}

	public static String getDate() {
		return date;
	}

	public static String getLongVersion() {
		return longVersion;
	}

	public static String getBuild() {
		return build;
	}

	public static String getVersion() {
		return version;
	}

	public static String getContact() {
		return contact;
	}

	public static String getCompanyName() {
		return companyName;
	}

	public static String getProductName() {
		return productName;
	}
	
	
    /**
     * Prints the version info.
     * @param args
     */
	public static void main(String[] args) {
	    printVersionInformation();
	}
	
}
