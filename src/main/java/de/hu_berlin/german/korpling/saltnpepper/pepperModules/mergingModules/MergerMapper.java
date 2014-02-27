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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Graph;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Node;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SDocumentGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SPointingRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotatableElement;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;

/**
 * @author Mario Frank
 * @author Jakob Schmoling
 * @author Florian Zipser
 */
public class MergerMapper extends PepperMapperImpl implements PepperMapper{
	
	public static final String ANNO_NAME_EXTENSION = "_1";

	private static final Logger logger = Logger.getLogger(MergerMapper.class);

	@Override
	public DOCUMENT_STATUS mapSDocument() {
		this.initialize();
		
		System.out.println("mapSDocument: "+ getMappingSubjects());
		
		if (this.getMappingSubjects().size() != 0){
			MappingSubject baseDocument = null;
			
			if (this.getMappingSubjects().size() < 2){
				baseDocument= getMappingSubjects().get(0);
			}else{
				this.initialize();
				// normalize all texts
				for (MappingSubject subj : this.getMappingSubjects()){
					if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument){
						this.normalizeTextualLayer((SDocument) subj.getSElementId().getSIdentifiableElement());
					}
				}
				
				for (MappingSubject subj : this.getMappingSubjects()){
					if (subj.getSElementId().getSIdentifiableElement() instanceof SDocument){
						baseDocument= subj;
						
						SDocument sDoc= (SDocument) subj.getSElementId().getSIdentifiableElement();
						if (sDoc.equals(container.getBaseDocument())){
							System.out.println("Chose base document. It is document with id ");
//							baseDocPair = sDocPair;
//							baseDocPair.status = DOCUMENT_STATUS.IN_PROGRESS;
							baseDocument= subj;
						}
					}
				}
				
				/// base text -- < Other Document -- nonEquivalentTokens >
				Hashtable<STextualDS, Hashtable<SDocument,HashSet<SToken>>> nonEquivalentTokenSets = new Hashtable<STextualDS, Hashtable<SDocument,HashSet<SToken>>>();
				// allign all texts
//				for (DocumentStatusPair sDocPair : this.getDocumentStatusPairs()){
				for (MappingSubject subj : this.getMappingSubjects()){
					boolean hasTexts = true;
//					if (! sDocPair.sDocument.equals(container.getBaseDocument()))
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
				/// choose the perfect STextualDS of the base Document
				SDocument baseDoc = this.container.getBaseDocument();
				STextualDS baseText = null;
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
				// set the base text
				this.container.setBaseText(baseText);
				// clear the nonEquivalentTokensMap from all base text candidates which were not approved
				for (STextualDS text : this.container.getBaseDocument().getSDocumentGraph().getSTextualDSs()){
					if (! text.equals(this.container.getBaseText())){
						nonEquivalentTokenSets.remove(text);
					}
				}
				
				// merge!
//				for (DocumentStatusPair sDocPair : this.getDocumentStatusPairs())
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
							System.out.println("Finishing document: " + (SDocument)baseDocument.getSElementId().getSIdentifiableElement());
							this.container.finishDocument(sDoc);
							subj.setMappingResult(DOCUMENT_STATUS.DELETED);
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
							System.out.println("Finishing document: " + (SDocument)baseDocument.getSElementId().getSIdentifiableElement());
							this.container.finishDocument(sDoc);
							subj.setMappingResult(DOCUMENT_STATUS.DELETED);
						}
					}
				}
				// clear the table of non-equivalent tokens
				nonEquivalentTokenSets.clear();
			}
			
			System.out.println("Finishing document: " + (SDocument)baseDocument.getSElementId().getSIdentifiableElement());
			this.container.finishDocument((SDocument)baseDocument.getSElementId().getSIdentifiableElement());
			baseDocument.setMappingResult(DOCUMENT_STATUS.COMPLETED);
		}else{
			logger.warn("No documents to merge");
		}
		
		System.out.println(">>>>>>>>>>>>>>>>>><< mapping results: "+getMappingSubjects());
		return(DOCUMENT_STATUS.COMPLETED);
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

	/** This table contains the escape sequences for all characters **/
	private Hashtable<Character,String> escapeTable= null;
	
	protected char[] punctuations = {'.',',',':',';','!','?','(',')','{','}','<','>'};
	/*
	this.escapeTable.put('.', "");
	this.escapeTable.put(',', "");
	this.escapeTable.put(':', "");
	this.escapeTable.put(';', "");
	this.escapeTable.put('!', "");
	this.escapeTable.put('?', "");
	this.escapeTable.put('(', "");
	this.escapeTable.put(')', "");
	this.escapeTable.put('{', "");
	this.escapeTable.put('}', "");
	this.escapeTable.put('<', "");
	this.escapeTable.put('>', "");*/
	
	/**
	 * This method searches for the first occurence of the stringToSearchFor in the stringToSearchIn and ommits all chars
	 * in the omitCharArray. The index of the first occurence is returned.
	 * @param stringToSearchIn
	 * @param stringToSearchFor
	 * @param omitCharArray
	 * @param useIndexof If this flag is set, all omit chars are removed from both provided strings and a normal indexOf is used
	 * @return the index on success and -1 on failure
	 */
	protected static int indexOfOmitChars(String stringToSearchIn, String stringToSearchFor, char[] omitCharArray, boolean useIndexOf) {

		/* put all omit characters into a hashset */
		HashSet<Character> omitChars = new HashSet<Character>();
		for (char c : omitCharArray){
			omitChars.add(c);
		}
		
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
			for (char targetChar : stringToSearchIn.toCharArray()){
				if (!omitChars.contains(targetChar)){
					builder.append(targetChar);
				}
			}
			String targetString = builder.toString();
			return targetString.indexOf(sourceString);
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
		
		// TODO @eladrion index of with punctuation skipping
		//int offset = normalizedBaseText.toLowerCase().indexOfAnyBut(normalizedOtherText.toLowerCase(),this.punctuations);
		int offset = indexOfOmitChars(normalizedBaseText.toLowerCase(),normalizedOtherText.toLowerCase(),this.punctuations, true);
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
						} else {
							//TODO: ERROR CATCHING
						}
						
					} 
				} else {
					//TODO: ERROR CATCHING
				}
			}
			
		}
		// get base text
		return returnVal;
	}
	
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
			String stringToEscape = this.escapeTable.get(c);
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
	public static boolean isMergeable(SDocument doc1,	SDocument doc2){
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
		
		char[] punctuations = {'.',',',':',';','!','?','(',')','{','}','<','>'};
		
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
						if (indexOfOmitChars(normalizedText1, normalizedText2, punctuations, true) != -1 ||
								indexOfOmitChars(normalizedText2, normalizedText1, punctuations, true) != -1
								
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
	 * @author eladrion
	 */
	protected void normalizeTextualLayer(SDocument sDocument){
		if (sDocument == null)
			throw new PepperModuleException("Cannot normalize Text of the document since the SDocument reference is NULL");
		
		// check whether the document has any STextualDS
		EList<STextualDS> sTextualDSs = sDocument.getSDocumentGraph().getSTextualDSs();
		
		//TODO I do not understand what happens here @Mario: Can you please adopt this to new fields and methods?
//		if (sTextualDSs == null){
//			this.getDocumentStatusPairs().add(new DocumentStatusPair(sDocument,DOCUMENT_STATUS.DELETED));
//		} else if (sTextualDSs.size() == 0){
//			this.getDocumentStatusPairs().add(new DocumentStatusPair(sDocument,DOCUMENT_STATUS.DELETED));
//		}
		
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
				String stringToEscape = this.escapeTable.get(c);
				// fill the StringBuilder
				if (stringToEscape != null){
					normalizedTextBuilder.append(stringToEscape);
					countOfChangedChars += 1;
				} else {
					normalizedTextBuilder.append(c);
				}
				
				// set the normalized start value
				if (stringToEscape == null){
					currentNormalizedLeft += 1;
				} else {
					if (stringToEscape.length() > 0){
						currentNormalizedLeft += stringToEscape.length();
					}
				}
				
				if (currentToken == null)
				{// If we are currently NOT iterating over a token's interval
					currentToken = tokensMappedByLeft.get(currentLeft);
					if (currentToken != null)
					{// if a token interval begins at the current left value
						//System.out.println("Starting alignment of Token "+currentToken.getSName());
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
						// reinitialize the normalizedTokenLeft and unmark the now processed token
						currentToken = null;
					} else {
						currentTokenLength += 1;
					}
				}
				currentLeft += 1;
			}
			// now we have the normalized text
			normalizedText = normalizedTextBuilder.toString();
			System.out.println("Normalize: "+sTextualDS.getSText()+" becomes "+normalizedText);
			// add it to the tokenMergeContainer
			this.container.addNormalizedText(sDocument, sTextualDS, normalizedText, countOfChangedChars);
		}
	}
	
	/**
	 * This method initializes the mapping.
	 * @author eladrion
	 */
	protected void initialize(){
		if (this.container == null){
			this.container = new TokenMergeContainer();
		}
		
		if (this.escapeTable == null){
			this.escapeTable = new Hashtable<Character, String>();
			this.escapeTable.put(' ', "");
			this.escapeTable.put('\t', "");
			this.escapeTable.put('\n', "");
			this.escapeTable.put('\r', "");
		
			this.escapeTable.put('ä', "ae");
			this.escapeTable.put('ö', "oe");
			this.escapeTable.put('ü', "ue");
			this.escapeTable.put('ß', "ss");
			this.escapeTable.put('Ä', "Ae");
			this.escapeTable.put('Ö', "Oe");
			this.escapeTable.put('Ü', "Ue");
		
			/*
			this.escapeTable.put('.', "");
			this.escapeTable.put(',', "");
			this.escapeTable.put(':', "");
			this.escapeTable.put(';', "");
			this.escapeTable.put('!', "");
			this.escapeTable.put('?', "");
			this.escapeTable.put('(', "");
			this.escapeTable.put(')', "");
			this.escapeTable.put('{', "");
			this.escapeTable.put('}', "");
			this.escapeTable.put('<', "");
			this.escapeTable.put('>', "");*/
		}
	}
	
	protected Map<SNode, SNode> mergeTokenContent(SDocument base, SDocument other){
		Map<SNode, SNode> equiMap = new HashMap<SNode, SNode>();
		// get all matching tokens
		for (STextualDS otherText : other.getSDocumentGraph().getSTextualDSs()) {
			for (SToken baseToken : base.getSDocumentGraph().getSTokens()) {
				SToken otherToken = container.getTokenMapping(baseToken, otherText);
				if(otherToken != null){
					equiMap.put(baseToken, otherToken);
//					copyAllAnnotations(otherToken, baseToken);
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
		matchingToken = reverseMap(matchingToken);
		mergeSearch(matchingToken, base.getSDocumentGraph(), other.getSDocumentGraph());
		//mergeSpanContent(base, other);
		//mergeStructureContent(base, other);
		System.out.println(String.format("== finished merge between %s and %s", base.getSId(), other.getSId()));
		return base;
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
			System.out.println(e.getSource().getId() + " --" + e.getId() + "->" + e.getTarget().getId());
		}
		while (!searchQueue.isEmpty()) {
			SNode node = searchQueue.remove();
			visited.add(node);
			SNode otherNode = matchingToken.get(node);

			// check every parent for equivalence on the base graph side
			EList<Edge> edgesToParent = g.getInEdges(node.getId());
			for (Edge edge : edgesToParent) {
				Node parent = edge.getSource();
				System.out.println("\tchecking " + parent);
				if (edge instanceof SPointingRelation) {
					continue;
				} else if (visited.contains(parent)) {
					continue;
					
				} else {
					List<Node> children = new ArrayList<Node>();
					searchQueue.add((SNode) parent);

					for (Edge e : g.getOutEdges(parent.getId())) {
						children.add(e.getTarget());
					}
					// every other parent in the other graph is a candidate for
					// equivalence
					EList<Edge> otherEdgesToParent = otherG
							.getInEdges(otherNode.getId());
					for (Edge otherEdgetoParent : otherEdgesToParent) {
						// Check if an parent has matching children
						Node otherParent = otherEdgetoParent.getSource();
						if (matchingToken.containsKey(otherParent)) {
							copyAllAnnotations(
									(SAnnotatableElement) otherParent,
									(SAnnotatableElement) parent);
						} else{
							boolean otherParentIsEquivalent = checkEquivalenz(
									otherG, otherParent, children, matchingToken);
							if (otherParentIsEquivalent) {
								System.out.println(String.format(
										"\t match found : %s/%s", otherG
										.getSDocument().getSId(),
										otherParent.getId()));
								// take action on matching node
								matchingToken.put((SNode) otherParent,
										(SNode) parent);
								copyAllAnnotations(
										(SAnnotatableElement) otherParent,
										(SAnnotatableElement) parent);
								// TODO: move Edge?
							} else {
								nonMatchingNode.add((SNode) parent);
								// TODO: move Node?
							}
							
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * check if an Node satisfies the equivalence all criteria 
	 * 1) the number of children matches
	 * 2) every child is has an already tested equivalence 
	 * @param otherG 
	 * @param otherParent
	 * @param childConfiguration
	 * @param matchingToken
	 */
	private boolean checkEquivalenz(Graph otherG, Node otherParent, List<Node> childConfiguration,
			Map<SNode, SNode> matchingToken) {
		EList<Edge> outEdges = otherG.getOutEdges(otherParent.getId());
		// 1) the number of children matches
		if (outEdges.size() != childConfiguration.size()){
			return false;
		}
		// 2) every child is has an already tested equivalence 
		for (Edge edge : outEdges) {
			Node child = edge.getTarget();
			if(!matchingToken.containsKey(child)){
				return false;
			}
		}
		return true;
	}
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
	 * Copies annotations from one element to another. If two annotations have
	 * different values, than the method will extend the annotation name to make
	 * coping possible. Existing annotations will not be copied.
	 * 
	 * @param from
	 * @param to
	 */
	public void copyAllAnnotations(SAnnotatableElement from, SAnnotatableElement to) {
		EList<SAnnotation> fromAnnotations = from.getSAnnotations();
		if (fromAnnotations != null) {
			
			for (SAnnotation fromAnno : fromAnnotations) {
				SAnnotation toAnno = to.getSAnnotation(fromAnno.getQName());
				if (toAnno == null) {
					toAnno = (SAnnotation) fromAnno.clone();
					to.addSAnnotation(toAnno);
					System.out.println("copied anno: " + toAnno.toString());
				} else if (!toAnno.getSValue().equals(fromAnno.getSValue())) {
					toAnno = (SAnnotation) fromAnno.clone();
					toAnno.setName(toAnno.getName() + ANNO_NAME_EXTENSION);
					to.addSAnnotation(toAnno);
					logger.warn(String.format(
							"Changed annotation name \"%s\" to \"%s\"",
							fromAnno.getName(), toAnno.getName()));
				} else {
					// identical annotations
				}
			}
		}
	}
}
