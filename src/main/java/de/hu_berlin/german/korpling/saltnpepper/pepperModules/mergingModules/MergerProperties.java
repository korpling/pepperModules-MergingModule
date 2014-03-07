package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperties;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperty;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.Merger.MERGING_LEVEL;

public class MergerProperties extends PepperModuleProperties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4689923498383429801L;
	
	public static final String PREFIX= "merger.";
	public static final String PROP_MERGING_LEVEL= PREFIX+"merging.level";
	public static final String PROP_PUNCTUATIONS= PREFIX+"punctuations";
	public static final String PROP_ESCAPE_MAPPING= PREFIX+"escapeMapping";

	/** Default punctuation characters **/
	public static final String  PUNCTUATION_DEFAULT = "'.',',',':',';','!','?','(',')','{','}','<','>'";
	
	public static final String ESCAPE_MAPPING_DEFAULT= 		"\" \": \"\", "
														+ "\"\t\": \"\", "
														+"\"\t\": \"\", "
														+"\"\n\": \"\", "
														+"\"\r\": \"\", "
														+"\"ä\": \"ae\", "
														+"\"ö\": \"oe\", "
														+"\"ü\": \"ue\", "
														+"\"ß\": \"ss\", "
														+"\"Ä\": \"Ae\", "
														+"\"Ö\": \"Oe\", "
														+"\"Ü\": \"Ue\", ";
	
	public MergerProperties()
	{
		this.addProperty(new PepperModuleProperty<String>(
				PROP_MERGING_LEVEL,
				String.class,
				"Determines the level of merging, the level can be one of the following types '"
						+ MERGING_LEVEL.MERGE_CORPUS_GRAPHS
						+ "' just copies all corpus graphs to a single one, '"
						+ MERGING_LEVEL.MERGE_DOCUMENT_PATHS
						+ "' merges all corpus graphs into a single corpus graph, but does not merge the documents and '"
						+ MERGING_LEVEL.MERGE_DOCUMENTS
						+ "' merges all possible documents (by given table or autodetect)",
				MERGING_LEVEL.MERGE_DOCUMENTS.toString()));
		
		this.addProperty(new PepperModuleProperty<String>(
				PROP_PUNCTUATIONS,
				String.class,
				"Determines the punctuation characters used to be ignored for merging. The value is a comma separated list, each entry must be surrounded by a quot: 'PUNCTUATION' (, 'PUNCTUATION')* .",
				PUNCTUATION_DEFAULT));
		this.addProperty(new PepperModuleProperty<String>(
				PROP_ESCAPE_MAPPING,
				String.class,
				"Determines the mapping used in normalization step, to map special characters like umlauts. This value is a comma separated list of mappings: \"REPLACED_CHARACTER\" : \"REPLACEMENT\" (, \"REPLACED_CHARACTER\" : \"REPLACEMENT\")*",
				ESCAPE_MAPPING_DEFAULT));
	}
	
	public MERGING_LEVEL getMergingLevel(){
		MERGING_LEVEL retVal= null;
		PepperModuleProperty<?> prop= getProperty(PROP_MERGING_LEVEL);
		if (prop!= null){
			if (prop.getValue()!= null){
				String level= prop.getValue().toString();
				if (	(level!= null)&&
						(!level.isEmpty()))
					retVal= MERGING_LEVEL.valueOf(level);
			}
		}
		return(retVal);
	}
	
	/** punctuation characters specified by the user. If the user didn't specify any**/
	private Set<Character> punctuations= null;
	
	/**
	 * Returns all punctuation characters specified by the user. If the user didn't specify any
	 * punctuations, the defaults are used 
	 * @return
	 */
	public synchronized Set<Character> getPunctuations(){
		if (punctuations== null){
			PepperModuleProperty<String> prop= (PepperModuleProperty<String>) getProperty(PROP_PUNCTUATIONS);
			
			String puncString= prop.getValue();
			if (	(puncString!= null)&&
					(!puncString.isEmpty())){
				punctuations= new HashSet<Character>();
				boolean quoteStarted= false;
				for (char ch: puncString.toCharArray()){
					if ('\'' == ch){
						quoteStarted= !quoteStarted;
					}else{
						if (	(','==ch)&&
								(!quoteStarted)){
							//doNothing
						}
						punctuations.add(ch);
					}
					
				}
			}
		}
		return(punctuations);
	}
	/** a map of characters to be escaped and the corresponding replacement String.**/
	private Map<String,String> escapeMapping= null;
	/**
	 * Returns a map of characters to be escaped and the corresponding replacement String. This map is computed from the property
	 * {@link #PROP_ESCAPE_MAPPING}, which has the form: \"REPLACED_CHARACTER\" : \"REPLACEMENT\" (, \"REPLACED_CHARACTER\" : \"REPLACEMENT\").  
	 * @return
	 */
	public Map<String,String> getEscapeMapping(){
//		Hashtable<Character,String> escapeTable = new Hashtable<Character, String>();
		if (escapeMapping== null){
			PepperModuleProperty<String> prop= (PepperModuleProperty<String>) getProperty(PROP_ESCAPE_MAPPING);
			
			String escaping= prop.getValue();
			if (	(escaping!= null)&&
					(!escaping.isEmpty())){
				escapeMapping= new Hashtable<String, String>();
				
				String[] singleMappings= escaping.split(",");
				if (singleMappings.length > 0){
					for (String singleMapping: singleMappings){
						String[] parts= singleMapping.split(":");{
							if (parts.length== 2){
								escapeMapping.put(parts[0].trim().replace("\"", ""), parts[1].trim().replace("\"", ""));
							}
						}
					}
				}
				
//				boolean quoteStarted= false;
//				for (char ch: escaping.toCharArray()){
//					String key= null;
//					String value= null;
//					if (','== ch){
//						key= null;
//						value= null;
//					}else if ('\"'==ch){
//						quoteStarted= !quoteStarted;
//					}
//				}
			}
		}
		return(escapeMapping);
	}
		
}
