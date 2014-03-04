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
	
	public static final String ESCAPE_MAPPING_DEFAULT= 		"' ': \"\", "
														+ "'\t': \"\", "
														+"'\t': \"\", "
														+"'\n': \"\", "
														+"'\r': \"\", "
														+"'ä': \"ae\", "
														+"'ö': \"oe\", "
														+"'ü': \"ue\", "
														+"'ß': \"ss\", "
														+"'Ä': \"Ae\", "
														+"'Ö': \"Oe\", "
														+"'Ü': \"Ue\", ";
	
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
				"Determines the mapping used in normalization step, to map special characters like umlauts. This value is a comma separated list of mappings: 'REPLACED_CHARACTER' : \"REPLACEMENT\" (, 'REPLACED_CHARACTER' : \"REPLACEMENT\")*",
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
	
	/**
	 * Returns all punctuation characters specified by the user. If the user didn't specify any
	 * punctuations, the defaults are used 
	 * @return
	 */
	public Set<Character> getPunctuations(){
		PepperModuleProperty<String> prop= (PepperModuleProperty<String>) getProperty(PROP_PUNCTUATIONS);
		
		Set<Character> retVal= null;
		String puncString= prop.getValue();
		if (	(puncString!= null)&&
				(!puncString.isEmpty())){
			retVal= new HashSet<Character>();
			boolean quoteStarted= false;
			for (char ch: puncString.toCharArray()){
				if ('\'' == ch){
					quoteStarted= !quoteStarted;
				}else{
					if (	(','==ch)&&
							(!quoteStarted)){
						//doNothing
					}
					retVal.add(ch);
				}
				
			}
		}
		return(retVal);
		
//		/** This table contains the escape sequences for all characters **/
//		private Hashtable<Character,String> escapeTable= null;
//		
//		protected char[] punctuations = {'.',',',':',';','!','?','(',')','{','}','<','>'};
	}
	
	/**
	 * Returns a map of characters to be escaped and the corresponding replacement String.  
	 * @return
	 */
	public Map<Character,String> getEscapeMapping(){
		Hashtable<Character,String> escapeTable = new Hashtable<Character, String>();
		
		String escapeMapping= 	"' ': \"\", "
								+ "'\t': \"\", "
								+"'\t': \"\", "
								+"'\n': \"\", "
								+"'\r': \"\", "
								+"'ä': \"ae\", "
								+"'ö': \"oe\", "
								+"'ü': \"ue\", "
								+"'ß': \"ss\", "
								+"'Ä': \"Ae\", "
								+"'Ö': \"Oe\", "
								+"'Ü': \"Ue\", ";
		escapeTable.put(' ', ""); 
		escapeTable.put('\t', "");
		escapeTable.put('\n', "");
		escapeTable.put('\r', "");
	
		escapeTable.put('ä', "ae");
		escapeTable.put('ö', "oe");
		escapeTable.put('ü', "ue");
		escapeTable.put('ß', "ss");
		escapeTable.put('Ä', "Ae");
		escapeTable.put('Ö', "Oe");
		escapeTable.put('Ü', "Ue");
		return(escapeTable);
	}
		
}
