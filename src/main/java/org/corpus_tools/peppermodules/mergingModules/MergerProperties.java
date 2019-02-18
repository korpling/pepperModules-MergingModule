/**
 * Copyright 2015 Humboldt-Universität zu Berlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.corpus_tools.peppermodules.mergingModules;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

@SuppressWarnings("serial")
public class MergerProperties extends PepperModuleProperties {
	public static final String PROP_PUNCTUATIONS = "punctuations";
	public static final String PROP_ESCAPE_MAPPING = "escapeMapping";
	public static final String PROP_COPY_NODES = "copyNodes";
	public static final String PROP_ONLY_MERGE_TEXTS_WITH_SAME_NAME = "onlyMergeTextsWithSameName";

	/**
	 * If this property is set to 'true', the base document is always the one, which
	 * belongs to the first SCorpusGraph (the first importer in Pepper workflow
	 * description). The value either could be 'true' or false.
	 **/
	public static final String PROP_FIRST_AS_BASE = "firstAsBase";

	/** Default punctuation characters **/
	public static final String PUNCTUATION_DEFAULT = "'.',',',':',';','!','?','(',')','{','}','<','>'";

	public static final String ESCAPE_MAPPING_DEFAULT = "\" \": \"\", " + //
			"\"\t\": \"\", " //
			+ "\"\t\": \"\", " //
			+ "\"\n\": \"\", "//
			+ "\"\r\": \"\", "//
			+ "\"ä\": \"ae\", "//
			+ "\"ö\": \"oe\", "//
			+ "\"ü\": \"ue\", "//
			+ "\"ß\": \"ss\", "//
			+ "\"Ä\": \"Ae\", "//
			+ "\"Ö\": \"Oe\", "//
			+ "\"Ü\": \"Ue\", ";

	public MergerProperties() {
		this.addProperty(new PepperModuleProperty<String>(PROP_PUNCTUATIONS, String.class,
				"Determines the punctuation characters used to be ignored for detecting equal textual data. The value is a comma separated list, each entry must be surrounded by a quot: 'PUNCTUATION' (, 'PUNCTUATION')* .",
				PUNCTUATION_DEFAULT));
		this.addProperty(new PepperModuleProperty<String>(PROP_ESCAPE_MAPPING, String.class,
				"Determines the mapping used in normalization step, to map special characters like umlauts. This value is a comma separated list of mappings: \"REPLACED_CHARACTER\" : \"REPLACEMENT\" (, \"REPLACED_CHARACTER\" : \"REPLACEMENT\")*",
				ESCAPE_MAPPING_DEFAULT));
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_FIRST_AS_BASE, Boolean.class,
				"If this property is set to 'true', the base document is always the one, which belongs to the first SCorpusGraph (the first importer in Pepper workflow description). The value either could be 'true' or 'false'. If this value is set to false, the base document is computed automically (normally the one with the largest primary text).",
				false, false));
		this.addProperty(new PepperModuleProperty<Boolean>(PROP_COPY_NODES, Boolean.class,
				"Determines if SSpan and SStructure nodes should be copied or merged. Merged means to move all annotations to the equivalent in base document. If value is true they will be copied.",
				false, false));

		this.addProperty(PepperModuleProperty.create().withName(PROP_ONLY_MERGE_TEXTS_WITH_SAME_NAME).withType(Boolean.class)
				.withDescription("If \"true\", only merge texts that have the same name").withDefaultValue(false)
				.isRequired(false).build());
	}

	/**
	 * punctuation characters specified by the user. If the user didn't specify any
	 **/
	private Set<Character> punctuations = null;

	/**
	 * Returns all punctuation characters specified by the user. If the user didn't
	 * specify any punctuations, the defaults are used
	 * 
	 * @return
	 */
	public synchronized Set<Character> getPunctuations() {
		if (punctuations == null) {
			PepperModuleProperty<String> prop = (PepperModuleProperty<String>) getProperty(PROP_PUNCTUATIONS);

			String puncString = prop.getValue();
			if ((puncString != null) && (!puncString.isEmpty())) {
				punctuations = new HashSet<Character>();
				boolean quoteStarted = false;
				for (char ch : puncString.toCharArray()) {
					if ('\'' == ch) {
						quoteStarted = !quoteStarted;
					} else {
						if ((',' == ch) && (!quoteStarted)) {
							// doNothing
						}
						punctuations.add(ch);
					}

				}
			}
		}
		return (punctuations);
	}

	/**
	 * a map of characters to be escaped and the corresponding replacement String.
	 **/
	private Map<String, String> escapeMapping = null;

	/**
	 * Returns a map of characters to be escaped and the corresponding replacement
	 * String. This map is computed from the property {@link #PROP_ESCAPE_MAPPING},
	 * which has the form: \"REPLACED_CHARACTER\" : \"REPLACEMENT\" (,
	 * \"REPLACED_CHARACTER\" : \"REPLACEMENT\").
	 * 
	 * @return
	 */
	public Map<String, String> getEscapeMapping() {
		if (escapeMapping == null) {
			PepperModuleProperty<String> prop = (PepperModuleProperty<String>) getProperty(PROP_ESCAPE_MAPPING);

			String escaping = prop.getValue();
			if ((escaping != null) && (!escaping.isEmpty())) {
				escapeMapping = new Hashtable<String, String>();

				String[] singleMappings = escaping.split(",");
				if (singleMappings.length > 0) {
					for (String singleMapping : singleMappings) {
						String[] parts = singleMapping.split(":");
						{
							if (parts.length == 2) {
								escapeMapping.put(parts[0].trim().replace("\"", ""), parts[1].trim().replace("\"", ""));
							}
						}
					}
				}
			}
		}
		return (escapeMapping);
	}

	/**
	 * If this property is set to 'true', the base document is always the one, which
	 * belongs to the first SCorpusGraph (the first importer in Pepper workflow
	 * description). The value either could be 'true' or false.
	 * 
	 * @return
	 */
	public Boolean isFirstAsBase() {
		PepperModuleProperty<Boolean> prop = (PepperModuleProperty<Boolean>) getProperty(PROP_FIRST_AS_BASE);
		return (Boolean.valueOf(prop.getValue()));
	}

	/**
	 * Determines if SSpan and SStructure nodes should be copied or merged. Merged
	 * means to move all annotations to the equivalent in base document. If value is
	 * true they will be copied .
	 * 
	 * @return
	 */
	public Boolean isCopyNodes() {
		PepperModuleProperty<Boolean> prop = (PepperModuleProperty<Boolean>) getProperty(PROP_COPY_NODES);
		return (Boolean.valueOf(prop.getValue()));
	}
	
	public Boolean isOnlyMergeTextWithSameName() {
		PepperModuleProperty<Boolean> prop = (PepperModuleProperty<Boolean>) getProperty(PROP_ONLY_MERGE_TEXTS_WITH_SAME_NAME);
		return (Boolean.valueOf(prop.getValue()));
	}

}
