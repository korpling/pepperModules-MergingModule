/**
 * Copyright 2009 Humboldt University of Berlin, INRIA.
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

package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.io.ObjectInputStream.GetField;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.MappingSubject;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.PepperMapper;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.impl.PepperMapperImpl;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Label;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.LabelableElement;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Node;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SPointingRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SRelation;

/**
 * @author Mario Frank
 * @author Jakob Schmoling
 * @author Florian Zipser
 */
public class MergerMapper extends PepperMapperImpl implements PepperMapper{
	
	public static final String LABEL_NAME_EXTENSION = "_1";

	private static final Logger logger = Logger.getLogger(MergerMapper.class);

	protected boolean isTestMode = false;
	
	/**
	 * This method chooses the base {@link SDocument}.
	 * @return The base {@link SDocument}
	 */
	private MappingSubject chooseBaseDocument(){
		MappingSubject baseDocument= null;
		for (MappingSubject subj : this.getMappingSubjects()){
			if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument){
				
				SDocument sDoc= (SDocument) subj.getSElementId().getSIdentifiableElement();
				if (sDoc.equals(container.getBaseDocument())){
					System.out.println("Chose base document. It is document with id "+ container.getBaseDocument().getSId());
					baseDocument= subj;
					baseDocument.setMappingResult(DOCUMENT_STATUS.IN_PROGRESS);
				}
			}
		}
		return baseDocument;
	}
	
	/**
	 * This method chooses a base {@link STextualDS} for the given base {@link SDocument} heuristically by determining the
	 * {@link STextualDS} which has the least count of tokens which do not have an equivalent in all other documents.
	 * @param baseDoc The base {@link SDocument}
	 * @param nonEquivalentTokenSets The set of tokens which do not have an equivalent in the base {@link STextualDS}
	 * @return The {@link STextualDS} which is suited best to be the base {@link STextualDS}
	 */
	private STextualDS chooseBaseText(SDocument baseDoc, Hashtable<STextualDS, Hashtable<SDocument,HashSet<SToken>>> nonEquivalentTokenSets){
		STextualDS baseText= null;
		int minimalNonEquivalentTokens = -1;
		for (STextualDS text : baseDoc.getSDocumentGraph().getSTextualDSs())
		{ // for all texts of the base document
			Hashtable<SDocument,HashSet<SToken>> nonEQTokensInOtherDoc = nonEquivalentTokenSets.get(text);
			if (nonEQTokensInOtherDoc != null)
			{ // there is a set of non-equivalent token for the current base text
				int countOfNonEquivalentTokens = 0;
				for (SDocument otherDoc : nonEQTokensInOtherDoc.keySet())
				{ // count the number of tokens of all documents which do not have an equivalent in the current base text
					countOfNonEquivalentTokens += nonEQTokensInOtherDoc.get(otherDoc).size();
				} // count the number of tokens of all documents which do not have an equivalent in the current base text
				if (minimalNonEquivalentTokens == -1)
				{ // if the minimalNonEquivalentTokens value is -1, we did not process a document, yet. initialize
					minimalNonEquivalentTokens = countOfNonEquivalentTokens;
					baseText = text;
				} // if the minimalNonEquivalentTokens value is -1, we did not process a document, yet. initialize
				else 
				{ // there is some base text
					if (minimalNonEquivalentTokens > countOfNonEquivalentTokens)
					{ // if there are less non-equivalent tokens for this base text than for some other, set this text as base text
						minimalNonEquivalentTokens = countOfNonEquivalentTokens;
						baseText = text;
					} // if there are less non-equivalent tokens for this base text than for some other, set this text as base text
				}
			} // there is a set of non-equivalent token for the current base text
			
		}
		if (baseText != null){
			System.out.println("Chose base text. It is text with id "+ baseText.getSId());
		}
		return baseText;
	}
	
	/**
	 * This method tries to align all texts of all {@link SDocument} objects to the base {@link STextualDS}.
	 * @return The data structure which contains all {@link SToken} objects contained in the {@link SDocument} which do not have an equivalent
	 * in the {@link STextualDS} specified as key.
	 */
	private Hashtable<STextualDS, Hashtable<SDocument, HashSet<SToken>>> allignAllTexts(){
		Hashtable<STextualDS, Hashtable<SDocument, HashSet<SToken>>> nonEquivalentTokenSets = new Hashtable<STextualDS, Hashtable<SDocument,HashSet<SToken>>>();
		for (MappingSubject subj : this.getMappingSubjects()){
			boolean hasTexts = true;
			SDocument sDoc= (SDocument) subj.getSElementId().getSIdentifiableElement();
			if (! sDoc.equals(container.getBaseDocument()))
			{// ignore the base document and align all other
				if (sDoc.getSDocumentGraph().getSTextualDSs() != null)
				{ // there are possibly texts
					subj.setMappingResult(DOCUMENT_STATUS.IN_PROGRESS);
					if (sDoc.getSDocumentGraph().getSTextualDSs().size() > 0)
					{ // The other document has at least one text
						HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
						for (STextualDS baseText : container.getBaseDocument().getSDocumentGraph().getSTextualDSs())
						{ // for all texts of the base document
							nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
							// initialize the set of nonEquivalent token. Initially, all token do not have an equivalent.
							// in alignTexts, tokens which DO have an equivalent are removed from the set
							if (sDoc.getSDocumentGraph().getSTokens() != null){
								nonEquivalentTokenInOtherTexts.addAll(sDoc.getSDocumentGraph().getSTokens());
							}
							for (STextualDS otherText : sDoc.getSDocumentGraph().getSTextualDSs())
							{ // allign the current base text with all texts of the other document
								this.alignTexts(baseText, otherText,nonEquivalentTokenInOtherTexts);
							}
							/// save all unique token of the other document
							if (nonEquivalentTokenSets.containsKey(baseText))
							{
								nonEquivalentTokenSets.get(baseText).put(sDoc, nonEquivalentTokenInOtherTexts);
							} 
							else 
							{
								Hashtable<SDocument,HashSet<SToken>> newTab = new Hashtable<SDocument, HashSet<SToken>>();
								newTab.put(sDoc, nonEquivalentTokenInOtherTexts);
								nonEquivalentTokenSets.put(baseText, newTab);
							}
						} // for all texts of the base document
					} // The other document has at least one text 
					else 
					{ // The other document has NO text
						hasTexts = false;
					} // The other document has NO text
				} // there are possibly texts
				else 
				{ // The other document has NO text
					hasTexts = false;
				} // The other document has NO text
				
				if (! hasTexts){
					HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
					for (STextualDS baseText : container.getBaseDocument().getSDocumentGraph().getSTextualDSs()){
						nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
						if (sDoc.getSDocumentGraph().getSTokens() != null)
						{ // mark all tokens of the other document as unique
							nonEquivalentTokenInOtherTexts.addAll(sDoc.getSDocumentGraph().getSTokens());
						}
						if (nonEquivalentTokenSets.containsKey(baseText)){
							nonEquivalentTokenSets.get(baseText).put(sDoc, nonEquivalentTokenInOtherTexts);
						} else {
							Hashtable<SDocument,HashSet<SToken>> newTab = new Hashtable<SDocument, HashSet<SToken>>();
							newTab.put(sDoc, nonEquivalentTokenInOtherTexts);
							nonEquivalentTokenSets.put(baseText, newTab);
						}
					}
				}
			} 
		}
		return nonEquivalentTokenSets;
	}
	
	@Override
	public DOCUMENT_STATUS mapSDocument() {
		this.initialize();
		System.out.println("MERGING SDOCUMENT: "+getMappingSubjects());
		
		merge();
		return(DOCUMENT_STATUS.COMPLETED);
	}
	
	public void merge() {
		if (this.getMappingSubjects().size() != 0){
			MappingSubject baseDocument = null;
			
			if (this.getMappingSubjects().size() < 2){
				baseDocument= getMappingSubjects().get(0);
			}else{
				this.initialize();
				// normalize all texts
				for (MappingSubject subj : this.getMappingSubjects()){
					if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument){
						SDocument sDoc= (SDocument) subj.getSElementId().getSIdentifiableElement();
						this.normalizeTextualLayer(sDoc);
					}
				}
				baseDocument = this.chooseBaseDocument();
				if (baseDocument == null){
					throw new PepperModuleException("Could not choose a base SDocument");
				}
				
				// allign all texts and create the nonEquivalentTokenSets
				/// base text -- < Other Document -- nonEquivalentTokens >
				Hashtable<STextualDS, Hashtable<SDocument,HashSet<SToken>>> nonEquivalentTokenSets = this.allignAllTexts();;
				
				/// choose the perfect STextualDS of the base Document
				SDocument baseDoc = this.container.getBaseDocument();
				STextualDS baseText = chooseBaseText(baseDoc, nonEquivalentTokenSets);
				if (baseText == null){
					throw new PepperModuleException("Could not choose a base STextualDS");
				}
				// set the base text
				this.container.setBaseText(baseText);
				
				// clear the nonEquivalentTokensMap from all base text candidates which were not approved
				for (STextualDS text : this.container.getBaseDocument().getSDocumentGraph().getSTextualDSs()){
					if (! text.equals(this.container.getBaseText())){
						nonEquivalentTokenSets.remove(text);
					}
				}
				
				// merge!
				for (MappingSubject subj : this.getMappingSubjects())
				{ // for all documents
					SDocument sDoc= (SDocument) subj.getSElementId().getSIdentifiableElement();
					if (! sDoc.equals(container.getBaseDocument()))
					{// ignore the base document and merge the others
						System.out.println("Merging document: " + sDoc);
						if (sDoc.getSDocumentGraph().getSTextualDSs() != null)
						{ // there should be texts
							System.out.println("\ttext based search");
							// get the set of tokens in the document which do not have an equivalent in the base text
							HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
							if (nonEquivalentTokenSets.get(this.container.getBaseText()) != null){
								if (nonEquivalentTokenSets.get(this.container.getBaseText()).get(sDoc) != null){
									nonEquivalentTokenInOtherTexts = nonEquivalentTokenSets.get(this.container.getBaseText()).get(sDoc);
								}
							}
							// merge the document content
							this.mergeDocumentContent((SDocument)baseDocument.getSElementId().getSIdentifiableElement(), sDoc, nonEquivalentTokenInOtherTexts);
							// we are finished with the document. Free the memory
							if (! this.isTestMode){
								System.out.println("Finishing document: " + (SDocument)baseDocument.getSElementId().getSIdentifiableElement());
								this.container.finishDocument(sDoc);
								subj.setMappingResult(DOCUMENT_STATUS.DELETED);
							}
						} else {
							// there are no texts. So, just copy everything into the base document graph
							System.out.println("\tno text found");
							HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
							if (sDoc.getSDocumentGraph().getSTokens() != null){
								// all tokens are unique
								nonEquivalentTokenInOtherTexts.addAll(sDoc.getSDocumentGraph().getSTokens());
							}
							// merge the document content
							this.mergeDocumentContent((SDocument)baseDocument.getSElementId().getSIdentifiableElement(),sDoc, nonEquivalentTokenInOtherTexts);
							// we are finished with the document. Free the memory
							
							if (! this.isTestMode){
								System.out.println("Finishing document: " + (SDocument)baseDocument.getSElementId().getSIdentifiableElement());
								this.container.finishDocument(sDoc);
								subj.setMappingResult(DOCUMENT_STATUS.DELETED);
							}
							
						}
					} 
				}
				// clear the table of non-equivalent tokens
				nonEquivalentTokenSets.clear();
			}
			
			
			if (baseDocument!= null){
				if (! this.isTestMode){
					System.out.println("Finishing document: " + (SDocument)baseDocument.getSElementId().getSIdentifiableElement());
					this.container.finishDocument((SDocument)baseDocument.getSElementId().getSIdentifiableElement());
					baseDocument.setMappingResult(DOCUMENT_STATUS.COMPLETED);
				}
			}else{
				// nothing to be merged
				int i= 0;
				for (MappingSubject subj: getMappingSubjects()){
					if (i==0){
						subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
					}else{
						subj.setMappingResult(DOCUMENT_STATUS.DELETED);
					}
					i++;
				}
			}
		}else{
			logger.warn("No documents to merge");
		}
		
		// print the count of STextualDS for which there is an equivalent token 
		/*
		System.out.println("Base document has "+ this.container.getBaseDocument().getSDocumentGraph().getSTokens().size()+" tokens");
		System.out.println("There are equivalence entries for "+this.container.getEquivalenceMap().size()+" tokens of the base document");
		System.out.println("The base text has "+this.container.getAlignedTokens(this.container.getBaseText()).getTokens().size());
		for (SToken tok : this.container.getBaseDocument().getSDocumentGraph().getSTokens()){
			System.out.println("Base token "+tok.getSName()+ " has an equivalent in "+this.container.getEquivalenceMap().get(tok).size() +" texts");
			for (STextualDS text : this.container.getEquivalenceMap().get(tok).keySet()){
				System.out.println("Token "+this.container.getEquivalenceMap().get(tok).get(text).getSId()+ " is the equivalent in text " +text.getSElementId());
			}
		}*/
		
		System.out.println(">>>>>>>>>>>>>>>>>><< mapping results: "+getMappingSubjects());
	}
	
	@Override
	public DOCUMENT_STATUS mapSCorpus() {
		System.out.println("MERGING SCORPUS: "+ getMappingSubjects());
		
		for (MappingSubject subj: getMappingSubjects()){
			if (subj.getSElementId().getSIdentifiableElement() instanceof SCorpus){
				//TODO move all annotations (SMetaAnnotation)
				subj.setMappingResult(DOCUMENT_STATUS.COMPLETED);
			}
		}
		
		return(DOCUMENT_STATUS.COMPLETED);
	}
	
	/** the {@link TokenMergeContainer} instance **/
	protected TokenMergeContainer container =null;
	
	
	public TokenMergeContainer getContainer() {
		return container;
	}
	
	/**
	 * This method searches for the first occurence of the stringToSearchFor in the stringToSearchIn and ommits all chars
	 * in the omitCharArray. The index of the first occurence is returned.
	 * @param stringToSearchIn
	 * @param stringToSearchFor
	 * @param omitCharArray
	 * @param useIndexof If this flag is set, all omit chars are removed from both provided strings and a normal indexOf is used
	 * @return the index on success and -1 on failure
	 */
	protected int indexOfOmitChars(String stringToSearchIn, String stringToSearchFor, boolean useIndexOf, Set<Character> omitChars) {

		// TODO @eladrion clean up
		/* put all omit characters into a hashset */
		
		
		/* remove all omit chars from the stringToSearchFor */
		StringBuilder builder = new StringBuilder();
		for (char sourceChar : stringToSearchFor.toCharArray()){
			if (!omitChars.contains(sourceChar)){
				builder.append(sourceChar);
			}
		}
		String sourceString = builder.toString();
		
		if (useIndexOf){
			builder = new StringBuilder();
			List<Integer> normalizedToOriginalMapping = new Vector<Integer>();
			int start = 0;
			for (char targetChar : stringToSearchIn.toCharArray()){
				if (!omitChars.contains(targetChar)){ // no omit char
					normalizedToOriginalMapping.add(start);
					builder.append(targetChar);
				} else { // omit char
				}
				start += 1;
			}
			String targetString = builder.toString();
			int index = targetString.indexOf(sourceString);
			if (index != -1){
				return normalizedToOriginalMapping.get(index);
			} else {
				return index;
			}
			
		}
		
		/* Initialize needed structures */
		char c = sourceString.toCharArray()[0];
		
		/*
		// get all indexes of the source char
		List<Integer> indexes = new Vector<Integer>();
		int charIndex = 0;
		for (char targetChar : stringToSearchIn.toCharArray()){
			if (c == targetChar){
				indexes.add(charIndex);
			}
			charIndex++;
		}
		if (indexes.isEmpty()){
			return -1;
		}
		
		for (Integer targetStartPosition : indexes){
			char[] charsToSearchIn = stringToSearchIn.toCharArray();
			
			int successfulMatchCount = 0;
			for (char sourceChar : sourceString.toCharArray())
			{ // for all chars of the string to search
				/// search the char in the target string and omit all omit chars in the target string
				boolean foundChar = false;
				for (int i = targetStartPosition ; i < stringToSearchIn.length() ; i++)
				{ // search the current char and ignore all chars to omit in the target string
					char targetChar = charsToSearchIn[i];
					if (omitChars.contains(targetChar))
					{ // ignore
						continue;
					} // ignore
					else 
					{ // do not ignore
						if (targetChar == sourceChar)
						{ // we found the matching char
							successfulMatchCount++;
							foundChar = true;
							break;
						} // we found the matching char
						else
						{ // the char is wrong
							foundChar = false;
							//position++;
							break;
						} // the char is wrong
					} // do not ignore
				} // search the current char and ignore all chars to omit in the target string
				if (! foundChar)
				{ // if a char could not be found, stop the search in the current subString
					retVal = -1;
					break;
				} // if a char could not be found, stop the search in the current subString
			}
		}
		*/
		int position = 0;
		int retVal = -1;
		boolean found = false;
		
		// transform the base text into a char array
		char[] charsToSearchIn = stringToSearchIn.toCharArray();
		while (true)
		{
			// the start position of the string to search
			position = stringToSearchIn.indexOf(c,position);
			retVal = position; // initialize the return value to the found position
			if (position == -1){ // stop, if the first char could not be found
				break;
			} 
			
			// we guess that we found a complete match
			found = true;
			// initialize the count of matched chars
			int successfulMatchCount = 0;
			for (char sourceChar : sourceString.toCharArray())
			{ // for all chars of the string to search
				/// search the char in the target string and omit all omit chars in the target string
				boolean foundChar = false;
				for (int i = position ; i < stringToSearchIn.length() ; i++)
				{ // search the current char and ignore all chars to omit in the target string
					char targetChar = charsToSearchIn[i];
					if (omitChars.contains(targetChar))
					{ // ignore
						position++;
						continue;
					} // ignore
					else 
					{ // do not ignore
						if (targetChar == sourceChar)
						{ // we found the matching char
							successfulMatchCount++;
							foundChar = true;
							position++;
							break;
						} // we found the matching char
						else
						{ // the char is wrong
							foundChar = false;
							//position++;
							break;
						} // the char is wrong
					} // do not ignore
				} // search the current char and ignore all chars to omit in the target string
				if (! foundChar)
				{ // if a char could not be found, stop the search in the current subString
					retVal = -1;
					break;
				} // if a char could not be found, stop the search in the current subString
			}
			if (found)
			{ // if the found flag is still set, we are finished, if the successfulMatchCount is equal to the source string
				if (successfulMatchCount == sourceString.length()){
					break;
				}
			}
		}
		
		return retVal;
	}

	
	/**
	/**
	 * This method aligns the normalized texts of the given {@link STextualDS} objects
	 * and <b>also</b> aligns the {@link SToken} including the creation of equivalent {@link SToken}
	 * information. If a {@link SToken} has an equivalent {@link SToken} in the base text, it is removed from
	 * the nonEquivalentTokenInOtherTexts set.
	 * @param baseText the base {@link STextualDS}
	 * @param otherText the other {@link STextualDS}
	 * @param nonEquivalentTokenInOtherTexts A HashSet which contains all tokens which do not have an equivalent in the base text
	 * @return true on success and false on failure
	 * @author eladrion
	 */
	protected boolean alignTexts(STextualDS baseText, STextualDS otherText, HashSet<SToken> nonEquivalentTokenInOtherTexts){
		if (baseText == null)
			throw new PepperModuleException("Cannot align the Text of the documents since the base SDocument reference is NULL");
		if (otherText == null)
			throw new PepperModuleException("Cannot align the Text of the documents since the other SDocument reference is NULL");
		
		boolean returnVal = false;
		// first we need the two normalized texts
		String normalizedBaseText = this.container.getNormalizedText(baseText);
		String normalizedOtherText = this.container.getNormalizedText(otherText);
		
		// set the mapping of the normalized base text to the original base text
		if (this.container.getBaseTextPositionByNormalizedTextPosition(this.container.getBaseText(), 0) == -1){
			this.container.setBaseTextPositionByNormalizedTextPosition(baseText,this.createBaseTextNormOriginalMapping(this.container.getBaseText()));
		}
		
		int offset = indexOfOmitChars(normalizedBaseText.toLowerCase(),normalizedOtherText.toLowerCase(),true, ((MergerProperties)getProperties()).getPunctuations());
		if (offset != -1)
		{// if the normalized other text is conatined in the normalized base text
			returnVal = true;
			//System.out.println("Text to merge has an offset of "+offset);
			// get the tokens of the other text.
			EList<SToken> textTokens = new BasicEList<SToken>();
			for (Edge e : otherText.getSDocumentGraph().getInEdges(otherText.getSId())){
				if (e instanceof STextualRelation){
					textTokens.add(((STextualRelation)e).getSToken());
				}
			}
			
			/*
			for (SToken baseTextToken : this.container.getBaseTextToken()){
				System.out.println("Base text token ("+baseTextToken.getSName()+") start and length: "+this.container.getAlignedTokenStart(this.container.baseText, baseTextToken)+"/"+this.container.getAlignedTokenLength(this.container.baseText, baseTextToken));
			}*/
			
			for (SToken otherTextToken : textTokens)
			{
				// get the aligned token start and length
				int otherTokenStart = this.container.getAlignedTokenStart(otherText, otherTextToken);
				int otherTokenLength = this.container.getAlignedTokenLength(otherText, otherTextToken);
				
				if (otherTokenStart != -1 && otherTokenLength != -1)
				{ // get the base text token:
					// get the aligned token from the base document which has the start of offset+startOfOtherToken
					//System.out.println("Other token ("+otherTextToken.getSName()+") start and length: "+otherTokenStart+"/"+otherTokenLength);
					SToken baseTextToken = this.container.getAlignedTokenByStart(baseText, (otherTokenStart+offset));
					
					if (baseTextToken != null)
					{// there is some baseTextToken which has the same start
						//System.out.println("Base Token "+ baseTextToken.getSName() + " and other token "+otherTextToken.getSName()+ "have the same start");
						//System.out.println("Lengths are: "+this.container.getAlignedTokenLength(baseText, baseTextToken)+ " and "+otherTokenLength);
						if (this.container.getAlignedTokenLength(baseText, baseTextToken) == otherTokenLength)
						{ // start and lengths are identical. We found an equivalence class
							this.container.addTokenMapping(baseTextToken, otherTextToken, otherText);
							nonEquivalentTokenInOtherTexts.remove(otherTextToken);
						} // start and lengths are identical. We found an equivalence class
						else 
						{ // start is identical but the length is not. No equvalence
						} // start is identical but the length is not. No equvalence
					}
					else
					{ // start is not identical. No equvalence
					} // start is not identical. No equvalence
				} 
				else 
				{ // the other token has either no start or no length -> ERROR
					this.logger.error("The SToken "+otherText.getSId()+" of the STextualDS "+otherText.getSId()+ " has no proper start or length. It was probably not aligned correctly.");
				} // the other token has either no start or no length -> ERROR
			}
			
		}
		// get base text
		return returnVal;
	}
	
	/**
	 * This method creates a reverse mapping list for the given Text.
	 * If the given text is normalized including the removal of whitespaces, the position of
	 * characters is changed. But if the position of a character in the original text is needed,
	 * we need more information. This method generates the needed information.
	 * @param sTextualDS The {@link STextualDS}
	 * @return A list of integers.
	 * 	The integer at index i specifies the position of the i'th character of the normalized text
	 *  in the original text. Example: Let c be the second character in the original text and a whitespace
	 *  the first character in the original text. Since the whitespace is removed, c is the first character
	 *  in the normalized text. The first element of the returned list will contain the number 2 since c was
	 *  the second char, originally.  
	 */
	protected List<Integer> createBaseTextNormOriginalMapping(STextualDS sTextualDS){
		/**
		 * Example1: dipl: " this is"
		 *                  01234567
		 *           norm: "thisis"
		 *                  012345
		 *                 0->1
		 *                 1->2,...
		 * Example2: dipl: " thäs is"
		 *                  01234567
		 *           norm: "thaesis"
		 *                  0123456
		 *                 0->1
		 *                 1->2
		 *                 2->3
		 *                 3->3
		 *                 4->4
		 *                 5->6
		 *                 6->7
		 */
		List<Integer> normalizedToOriginalMapping = new Vector<Integer>();
		int start = 0;
		for (char c : sTextualDS.getSText().toCharArray()){
			String stringToEscape = ((MergerProperties)getProperties()).getEscapeMapping().get(c);
			if (stringToEscape == null){
				normalizedToOriginalMapping.add(start);
			} else {
				if (stringToEscape.length() > 0){
					for (char x : stringToEscape.toCharArray())
					{// one char is mapped to many. all chars have the same index in the original text
						normalizedToOriginalMapping.add(start);
					}
				} 
				else 
				{ // one char is mapped to the empty string. 
					// do nothing
				}
			}
			start += 1;
		}
		return normalizedToOriginalMapping;
	}
	
	/**
	 * This method checks whether the provided documents are mergeable. I.e., it 
	 * is checked whether at least one text of the one document is alignible with at
	 * least one text of the other document.
	 * @param doc1 Some {@link SDocument}
	 * @param doc2 Some {@link SDocument}
	 * @return true, if one of the {@link SDocument} objects is meargeable into the other and false, else
	 */
	public boolean isMergeable(SDocument doc1,	SDocument doc2){
		boolean retVal = false;
		Hashtable<Character,String> escapeTable = new Hashtable<Character, String>();
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
		
		EList<STextualDS> doc1Texts = doc1.getSDocumentGraph().getSTextualDSs();
		EList<STextualDS> doc2Texts = doc2.getSDocumentGraph().getSTextualDSs();
		if (doc1Texts != null && doc2Texts != null)
		{ // both documents should have texts
			if ( (!doc1Texts.isEmpty()) && (!doc2Texts.isEmpty()))
			{ // both documents do have at least one text
				for (STextualDS text1 : doc1Texts){
					String normalizedText1 = normalizeText(text1, escapeTable);
					for (STextualDS text2 : doc2Texts){
						String normalizedText2 = normalizeText(text1, escapeTable);
						if (indexOfOmitChars(normalizedText1, normalizedText2, true, ((MergerProperties)getProperties()).getPunctuations()) != -1 ||
								indexOfOmitChars(normalizedText2, normalizedText1, true, ((MergerProperties)getProperties()).getPunctuations()) != -1
								
						){
							retVal = true;
							break;
						}
					}
					if (retVal){
						break;
					}
				}
			} // both documents do have at least one text
			else 
			{ // one document does not have a text. We can merge
				retVal = true;
			} // one document does not have a text. We can merge
			
		} // both documents should have texts
		else 
		{ // at least one document obviously does not have a text. We can merge.
			retVal = true;
		} // at least one document obviously does not have a text. We can merge.

		return retVal;
	}
	
	/**
	 * This method normalizes the text specified by the given {@link STextualDS}.
	 * @param sTextualDS  the {@link STextualDS} to normalize
	 * @return The normalized text
	 */
	protected static String normalizeText(STextualDS sTextualDS, Hashtable<Character,String> escapeTable){
		String normalizedText = null;
		StringBuilder normalizedTextBuilder = new StringBuilder();
		// normalize the text
		for (char c : sTextualDS.getSText().toCharArray()){
			String stringToEscape = escapeTable.get(c);
			// fill the StringBuilder
			if (stringToEscape != null){
				normalizedTextBuilder.append(stringToEscape);
			} else {
				normalizedTextBuilder.append(c);
			}
		}
		// now we have the normalized text
		normalizedText = normalizedTextBuilder.toString();
		return normalizedText;
	}
	
	/**
	 * This method normalizes the textual layer for the given {@link SDocument}.
	 * Note: only the normalization is done. The equivalent {@link SToken} are not
	 * determined in any way. For this functionality, you need to use {@link alignDocuments}.
	 * @param sDocument the {@link SDocument} for which the textual layer should be normalized.
	 */
	protected void normalizeTextualLayer(SDocument sDocument){
		if (sDocument == null)
			throw new PepperModuleException("Cannot normalize Text of the document since the SDocument reference is NULL");
		if (sDocument.getSDocumentGraph()!= null){
			// check whether the document has any STextualDS
			EList<STextualDS> sTextualDSs = sDocument.getSDocumentGraph().getSTextualDSs();
			
			// create maps which give fast access to a token which is specified by it's original left/right value
			Hashtable<Integer,SToken> tokensMappedByLeft = new Hashtable<Integer, SToken>();
			Hashtable<Integer,SToken> tokensMappedByRight = new Hashtable<Integer, SToken>();
			for (STextualRelation textRel : sDocument.getSDocumentGraph().getSTextualRelations()){
				tokensMappedByLeft.put(textRel.getSStart(), textRel.getSToken());
				tokensMappedByRight.put(textRel.getSEnd(), textRel.getSToken());
			}
			
			// normalize all textual datasources
			for (STextualDS sTextualDS : sTextualDSs){
				
				String normalizedText = null;
				StringBuilder normalizedTextBuilder = new StringBuilder();
				
				SToken currentToken= null;
				
				int currentLeft = 0;
				int countOfChangedChars = 0;
				int currentTokenLength = 1;
				int currentNormalizedLeft = 0;
				
				// normalize the text
				for (char c : sTextualDS.getSText().toCharArray()){
					String stringToEscape = ((MergerProperties)getProperties()).getEscapeMapping().get(c);
					// fill the StringBuilder
					if (stringToEscape != null){
						normalizedTextBuilder.append(stringToEscape);
						countOfChangedChars += 1;
						currentNormalizedLeft += stringToEscape.length();
					} else {
						normalizedTextBuilder.append(c);
						currentNormalizedLeft += 1;
					}
					
					if (currentToken == null)
					{// If we are currently NOT iterating over a token's interval
						currentToken = tokensMappedByLeft.get(currentLeft);
						if (currentToken != null)
						{// if a token interval begins at the current left value
							//System.out.println("Starting alignment of Token "+currentToken.getSName());
							//System.out.println("Found char of token: "+c);
							currentTokenLength = 1;
						}
					} 
					else 
					{// If we ARE currently iterating over a token's interval
						//System.out.println("Aligning Token "+currentToken.getSName()+ " . Current left: "+currentLeft);
						if (tokensMappedByRight.containsKey(currentLeft))
						{// if we reached the original end-char of the token
							//System.out.println("Ending alignment of Token "+currentToken.getSName());
							// beware: the SEnd value of a STextalRelation is the last char index of the token +1
							//container.addAlignedToken(sTextualDS, currentToken, normalizedTokenLeft, normalizedTokenLeft+currentTokenLength );
							container.addAlignedToken(sTextualDS, currentToken, currentNormalizedLeft, currentNormalizedLeft+currentTokenLength );
							//System.out.println("Ended alignment of token "+ currentToken.getSName());
							// reinitialize the normalizedTokenLeft and unmark the now processed token
							currentToken = null;
						} else {
							//System.out.println("Found char of token: "+c);
							currentTokenLength += 1;
						}
					}
					currentLeft += 1;
				}
				if (currentToken != null)
				{ // there is a token which is not closed yet. do it now
					container.addAlignedToken(sTextualDS, currentToken, currentNormalizedLeft, currentNormalizedLeft+currentTokenLength );
					//System.out.println("Ended alignment of token "+ currentToken.getSName());
				}
				// now we have the normalized text
				normalizedText = normalizedTextBuilder.toString();
				System.out.println("Normalize: "+sTextualDS.getSText()+" becomes "+normalizedText);
				// add it to the tokenMergeContainer
				this.container.addNormalizedText(sDocument, sTextualDS, normalizedText);
			}
		}
	}
	
	/**
	 * This method initializes the mapping.
	 */
	protected void initialize(){
		if (this.container == null){
			this.container = new TokenMergeContainer();
		}
	}
	
	protected Map<SNode, SNode> mergeTokenContent(SDocument base, SDocument other){
		Map<SNode, SNode> equiMap = new HashMap<SNode, SNode>();
		// get all matching tokens
		for (STextualDS otherText : other.getSDocumentGraph().getSTextualDSs()) {
			for (SToken baseToken : base.getSDocumentGraph().getSTokens()) {
				SToken otherToken = container.getTokenMapping(baseToken, otherText);
				if(otherToken != null){
					equiMap.put( otherToken,baseToken);
				} else{
					// TODO: copy token
				}
			}
		}
		return equiMap;
	}
	
	protected void mergeSpanContent(SDocument base, SDocument other){
		
	}
	
	protected void mergeStructureContent(SDocument base, SDocument other){
		
	}
	
	/**
	 * This method merges the Document content of the other {@link SDocument} to the base {@link SDocument} and
	 * uses the set of {@link SToken} which are contained in the other {@link SDocument} but not in the base {@link SDocument}
	 * to determine which {@link SToken} has no equivalent in the base {@link SDocument}.
	 * @param base
	 * @param other
	 * @param nonEquivalentTokenInOtherTexts
	 * @return
	 */
	protected SDocument mergeDocumentContent(SDocument base, SDocument other, HashSet<SToken> nonEquivalentTokenInOtherTexts){
		//chooseFinalBaseText();
		System.out.println(String.format("== Start merge between %s and %s", base.getSId(), other.getSId()));
		logger.debug(String.format("Start merge between %s and %s", base.getSId(), other.getSId()));
		Map<SNode,SNode> matchingToken = mergeTokenContent(base, other);
		// TODO: may use the reversed map only?
//		matchingToken = reverseMap(matchingToken);
		System.out.println("== Matching token:");
		for (Entry<SNode, SNode> node : matchingToken.entrySet()) {
			System.out.println(String.format("%s\t-->\t%s", node.getKey(),node.getValue()));
		}
		mergeSearch(matchingToken, base.getSDocumentGraph(), other.getSDocumentGraph());
		//mergeSpanContent(base, other);
		//mergeStructureContent(base, other);
		moveAll(matchingToken,base.getSDocumentGraph(), other.getSDocumentGraph());
		System.out.println(String.format("== finished merge between %s and %s", base.getSId(), other.getSId()));
		return base;
	}
	
	private void moveAll(Map<SNode, SNode> matchingToken,
			SDocumentGraph base, SDocumentGraph other) {
		// Move nodes first 
		for (SNode otherNode : other.getSNodes()) {
			if(matchingToken.containsKey(otherNode)){
				System.out.println("merging SNode" + otherNode.getSId());
				SNode match = matchingToken.get(otherNode);
				// change edges 
				updateEdges(other, match, match.getSId());
				// change annotations
				moveAllLabels(otherNode, match);
				// change layers?
				
			}else{
				System.out.println("Moving SNode " + otherNode.getSId());
				String oldID = otherNode.getSId();
				otherNode.setSGraph(base);
				updateEdges(other, otherNode, oldID);
			}
		}
		// edges can not be moved before every node is moved
		for (SRelation otherRelation : other.getSRelations()) {
			System.out.println("Moving edge" + otherRelation.getSId());
			otherRelation.setSGraph(base);
		}
		
	}

	private void updateEdges(SDocumentGraph other, SNode otherNode, String oldID) {
		for (Edge relation : other.getInEdges(oldID)) {
			relation.setTarget(otherNode);
		}
		for (Edge relation : other.getOutEdges(oldID)) {
			relation.setSource(otherNode);
		}
	}

	/**
	 * breath first search for matching tokens
	 * 
	 * @param matchingToken
	 * @param g
	 * @param otherG
	 */
	private void mergeSearch(Map<SNode, SNode> matchingToken, SDocumentGraph g,
			SDocumentGraph otherG) {
		// for every equivalent token:
		Queue<SNode> searchQueue = new LinkedList<SNode>();
		searchQueue.addAll(matchingToken.keySet());
		List<SNode> nonMatchingNode = new LinkedList<SNode>();
		Set<Node> visited = new HashSet<Node>();
		System.out.println(String.format("Start merge search (tokens:%s):", searchQueue.size()));
		for (Edge e : g.getEdges()) {
			System.out.println(e.getSource().getId() + "\t--" + e.getId() + "->\t" + e.getTarget().getId());
		}
		while (!searchQueue.isEmpty()) {
			SNode node = searchQueue.remove();
			SNode otherNode = matchingToken.get(node);
			visited.add(node);
//			simultaneousEquivalenceCheck(node, otherNode, matchingToken);
//			System.out.println("Before: " + g.getOutEdges(otherNode.getSId()).size());
			
			otherNode.setGraph(g);
//			System.out.println("After: " + g.getOutEdges(otherNode.getSId()).size());

			// check every parent for equivalence on the base graph side
//			EList<Edge> in = otherG.getInEdges(otherNode.getSId());
			EList<Edge> out = otherG.getOutEdges(otherNode.getSId());
//			out.addAll(in);
			for (Edge edge : out) {
				Node parent = edge.getSource();
				Node target = edge.getTarget();
				
				if(matchingToken.containsKey(parent)){
					edge.setSource(matchingToken.get(parent));
					edge.setGraph(g);
				}
				if(matchingToken.containsKey(target)){
					System.out.println("Before: " + edge.getTarget());
//					edge.setTarget(matchingToken.get(parent));
//					edge.setGraph(g);
					System.out.println("After: " + edge.getTarget());
				}
				
				Node[] nodes = {parent, target};
				for (Node n : nodes) {
					System.out.println("\tchecking " + parent);
					if (edge instanceof SPointingRelation) {
						continue;
					} else if (visited.contains(parent)) {
						continue;
						
					} else {
						searchQueue.add((SNode) parent);
						
						
						
					}
				}
				
			} // end for
//			
			for (SNode unmatched : nonMatchingNode) {
//				unmatched.set
			}
		}
		
	}
	
	class NodeParameters{
		String canonicalClassName;
		Map<String, Integer> outgoingCount;
		Map<String, Integer> inboundCount;
		
		public NodeParameters(SNode n, SGraph g) {
			// TODO Auto-generated constructor stub
			canonicalClassName = n.getClass().getCanonicalName();
			addEdges(inboundCount, g.getInEdges(n.getSId()));
			addEdges(outgoingCount, g.getOutEdges(n.getSId()));
		}

		private void addEdges(Map<String, Integer> map, EList<Edge> edges) {
			for (Edge e : edges) {
				Integer i;
				if (map.containsKey(e.getClass().getCanonicalName())) {
					i = map.get(e.getClass().getCanonicalName()) + 1;
				} else {
					i = 1;
				}
				inboundCount.put(e.getClass().getCanonicalName(), i);
			}
		}
		
		@Override
		public boolean equals(Object other) {
			if (!(other instanceof NodeParameters)){
				return false;
			}
			NodeParameters param = (NodeParameters) other;
			if (!param.canonicalClassName.equals(this.canonicalClassName)){
				return false;
			}
			if (param.outgoingCount.size() != this.outgoingCount.size()){
				return false;
			}
			if (!isMapEqual(param.inboundCount,this.inboundCount)){
				return false;
			}
			if (!isMapEqual(param.outgoingCount,this.outgoingCount)){
				return false;
			}
			return true;
		}
		
		private boolean isMapEqual(Map<String, Integer> map1, Map<String, Integer> map2) {
			// TODO Auto-generated method stub
			for (String key : map1.keySet()) {
				if (map1.get(key) != map2.get(key)){
					return false;
				}
			}
			return true;
		}
	}
	
//	/**
//	 * check if an Node satisfies the equivalence all criteria 
//	 * 1) the number of children matches
//	 * 2) every child is has an already tested equivalence 
//	 * @param otherG 
//	 * @param otherParent
//	 * @param childConfiguration
//	 * @param matchingToken
//	 */
//	private boolean checkEquivalenz(Graph otherG, Node otherParent, List<Node> childConfiguration,
//			Map<SNode, SNode> matchingToken) {
//		EList<Edge> outEdges = otherG.getOutEdges(otherParent.getId());
//		// 1) the number of children matches
//		if (outEdges.size() != childConfiguration.size()){
//			return false;
//		}
//		// 2) every child is has an already tested equivalence 
//		for (Edge edge : outEdges) {
//			Node child = edge.getTarget();
//			if(!matchingToken.containsKey(child)){
//				return false;
//			}
//		}
//		return true;
//	}
	/**
	 * Adds every map entry in reversed order
	 * @param map
	 * @return 
	 */
	private Map<SNode, SNode> reverseMap(Map<SNode, SNode> map) {
		Map<SNode, SNode> ret = new HashMap<SNode, SNode>();
		for (Map.Entry<SNode, SNode> entry : map.entrySet()) {
			ret.put(entry.getValue(), entry.getKey());
		}
		return ret;
		
	}
	
	/**
	 * moves annotations from one element to another. 
	 * 
	 * If two annotations have different values, 
	 * than the method will extend the annotation name to make
	 * coping possible. 
	 * 
	 * Existing annotations will not be copied, but removed from the first element.
	 * 
	 * @param from
	 * @param to
	 */
	public void moveAllLabels(LabelableElement from, LabelableElement to) {
		EList<Label> fromAnnotations = from.getLabels();
		if (fromAnnotations != null) {
			
			while (!from.getLabels().isEmpty()) {
				// actually I should not be able to remove labels directly, don't I?
				Label fromLabel = from.getLabels().remove(0);
				Label toLabel = to.getLabel(fromLabel.getQName());
				if (toLabel == null) {
					// add as new label
					to.addLabel(fromLabel);
					System.out.println("moved anno: " + fromLabel.toString());
				} else {
					Object fromVal = fromLabel.getValue();
					Object toVal = toLabel.getValue();
					if (fromVal == toVal && fromVal == null){
						fromLabel.setQName(fromLabel.getQName() + LABEL_NAME_EXTENSION);
					} else if (!fromVal.equals(toVal)) {
						// same name but different values
						fromLabel.setQName(fromLabel.getQName()
								+ LABEL_NAME_EXTENSION);
						to.addLabel(fromLabel);
						logger.warn(String
								.format("Changed annotation name to\"%s\" from \"%s\" because %s != %s",
										fromLabel.getQName(),
										toLabel.getQName(), toLabel.getValue(),
										fromLabel.getValue()));
					} else {
						System.out.println(String.format(
								"found same annotation:\n\t%s %s to\n%s %s",
								fromLabel.getQName(), toLabel.getQName(),
								toLabel.getQName(), toLabel.getValue()));
					}
				}
//				else {
//					// identical annotations
////					from.removeLabel(fromLabel.getQName());
//				}
			}
		}
	}
}
