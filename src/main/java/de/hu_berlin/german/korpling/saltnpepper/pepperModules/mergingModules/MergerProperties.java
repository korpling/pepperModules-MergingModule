package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

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
		
}
