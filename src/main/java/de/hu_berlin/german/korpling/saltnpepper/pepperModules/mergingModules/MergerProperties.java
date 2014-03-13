package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperties;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperModuleProperty;

public class MergerProperties extends PepperModuleProperties {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4689923498383429801L;
	
	public static final String PREFIX= "merger.";
	public static final String PROP_PUNCTUATIONS= PREFIX+"punctuations";
	public static final String PROP_ESCAPE_MAPPING= PREFIX+"escapeMapping";
	/** If this property is set to 'true', the base document is always the one, which belongs to the first SCorpusGraph (the first importer in Pepper workflow description). The value either could be 'true' or false. **/
	public static final String PROP_FIRST_AS_BASE= PREFIX+"firstAsBase";

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
				PROP_PUNCTUATIONS,
				String.class,
				"Determines the punctuation characters used to be ignored for merging. The value is a comma separated list, each entry must be surrounded by a quot: 'PUNCTUATION' (, 'PUNCTUATION')* .",
				PUNCTUATION_DEFAULT));
		this.addProperty(new PepperModuleProperty<String>(
				PROP_ESCAPE_MAPPING,
				String.class,
				"Determines the mapping used in normalization step, to map special characters like umlauts. This value is a comma separated list of mappings: \"REPLACED_CHARACTER\" : \"REPLACEMENT\" (, \"REPLACED_CHARACTER\" : \"REPLACEMENT\")*",
				ESCAPE_MAPPING_DEFAULT));
		this.addProperty(new PepperModuleProperty<Boolean>(
				PROP_FIRST_AS_BASE,
				Boolean.class,
				"If this property is set to 'true', the base document is always the one, which belongs to the first SCorpusGraph (the first importer in Pepper workflow description). The value either could be 'true' or 'false'. If this value is set to false, the base document is computed automically (normally the one with the largest primary text).",
				false, false));
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
			}
		}
		return(escapeMapping);
	}
	/**
	 * If this property is set to 'true', the base document is always the one, which belongs to the 
	 * first SCorpusGraph (the first importer in Pepper workflow description). 
	 * The value either could be 'true' or false.
	 * @return
	 */
	public Boolean isFirstAsBase(){
		PepperModuleProperty<String> prop= (PepperModuleProperty<String>) getProperty(PROP_FIRST_AS_BASE);
		return(Boolean.valueOf(prop.getValue()));
	}
}
