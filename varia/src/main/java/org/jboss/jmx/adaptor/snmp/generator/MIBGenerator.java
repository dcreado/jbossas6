/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
//package  org.jboss.jmx.adaptor.snmp.generator;

/**
 * This class serves as the entry point for MIB Generation. It recieves input from the CLI, 
 * parses the xml and creates an appropriate Generator object to actually write the MIB.
 * For now, only xml > mib conversions are supported.
 * 
 * TODO: Add switch for MIB to XML conversions, using Mibble
 * @author thauser
 *
 */

package org.jboss.jmx.adaptor.snmp.generator;

public class MIBGenerator {
	/**
	 * Get the filenames from the command line. Parse the input file, whether it be an mib or an xml. 
	 * Create the needed objects for either one, and create a new generator object with the information.
	 * Write the file using the generator. 
	 * TODO: add checks for the input arguments
	 * 
	 * @param args
	 * @author Tom Hauser
	 */
	public static void main (String [] args){
		CmdLineParser cmdParser = new CmdLineParser();
		Parser parser = new Parser();
		/*
		 * Adds the desired options to the command line parser.
		 * Options:
		 * 		-h , --help : display the usage information
		 *      -a , --attributes : indicate the name of attributes.xml, if any
		 * 		-n , --notifications : indicate the name of the notification.xml file, if any.
	     *      -m , --mib : indicate the desired name of the MIB file. this can be a path.
		 */
		CmdLineParser.Option help = cmdParser.addBooleanOption('h', "help");
		CmdLineParser.Option attributes = cmdParser.addStringOption('a', "attributes");
		CmdLineParser.Option notifications = cmdParser.addStringOption('n', "notifications");
		CmdLineParser.Option output = cmdParser.addStringOption('o', "output");
		CmdLineParser.Option module = cmdParser.addStringOption('m', "module");
		
	 	try {
	 		cmdParser.parse(args);
	 	}
	 	catch (Exception e){
	 		printUsageMessage();
	 		System.exit(1);
	 	}
	 	Boolean isHelp = (Boolean)cmdParser.getOptionValue(help, Boolean.FALSE);
	 	String aFile = (String)cmdParser.getOptionValue(attributes);
	 	String nFile = (String)cmdParser.getOptionValue(notifications);
	 	String oFile = (String)cmdParser.getOptionValue(output);
	 	String moduleName = (String)cmdParser.getOptionValue(module);
	 	
	 	if (moduleName == null){
	 		moduleName = "JBOSS-AS-MIB";
	 	}
	 	
	 	if (isHelp){
	 		printUsageMessage();
	 		System.exit(0);
	 	}

	 	parser = new Parser(aFile, nFile);
		parser.parse();
		Generator generator = new Generator(oFile, moduleName, parser.getMaList(), parser.getMbList(), parser.getNmList());
		generator.writeFile();		
	}
	
	
	private static void printUsageMessage(){
		System.out.println("Usage: java -jar mib-generator.jar [FLAG][FILENAME] .. [FLAG][FILENAME] .. [FLAG][FILENAME]\n" +
				"[FLAG]s:\n-h , --help : display the usage information\n" +
				"-a , --attributes : indicate the name of the snmp-adaptor formatted attributes.xml, if any.\n" +
				"-n , --notifications : indicate the name of the snmp-adaptor formatted notification.xml file, if any.\n" +
				"-o , --output : indicate the desired name of the output MIB file. This can be a path.\n" +
				"-m , --module : indicate the desired name of the output MIB module. This is the name that the SNMP manager will know the MIB by. \n" +
				"                Defaults to JBOSS-AS-MIB if not specified\n"+
				"Example: java -jar mib-generator.jar -a attributes.xml -n notifications.xml -m /home/user/TEST-MIB.mib");
	}
	
}