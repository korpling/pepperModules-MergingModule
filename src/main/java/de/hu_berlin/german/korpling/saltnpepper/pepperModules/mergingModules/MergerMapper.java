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

import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import de.hu_berlin.german.korpling.saltnpepper.pepper.modules.exceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.salt.graph.Edge;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotatableElement;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SAnnotation;

import de.hu_berlin.german.korpling.saltnpepper.pepper.common.DOCUMENT_STATUS;

public class MergerMapper {
	
	public static final String ANNO_NAME_EXTENSION = "_1";

	static private Logger log = Logger.getLogger(MergerMapper.class);

	public static class DocumentStatusPair{
		public DocumentStatusPair(SDocument sDocument){
			this.sDocument= sDocument;
		}
		public DocumentStatusPair(SDocument sDocument, DOCUMENT_STATUS stat){
			this.sDocument= sDocument;
			this.status = stat;
		}
		public SDocument sDocument= null;
		public DOCUMENT_STATUS status= null;
	}
	
	public List<DocumentStatusPair> pairs= null;
	
	/**
	 * Returns all documents to be mapped.
	 * @return
	 */
	public List<DocumentStatusPair> getDocumentPairs(){
		if (pairs== null){
			pairs= new Vector<DocumentStatusPair>();
		}
		return(pairs);
	}
	/** 
	 * Returns a list of {@link SDocument} objects to be merged, retrived from {@link #pairs}.
	 * @return a list of {@link SDocument} objects
	**/
	private List<SDocument> getSDocuments(){
		List<SDocument> retVal= null;
		if (pairs!= null){
			retVal= new Vector<SDocument>();
			for (DocumentStatusPair pair: pairs){
				retVal.add(pair.sDocument);
			}
		}
		return(retVal);
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
	 * @return the index on success and -1 on failure
	 */
	protected int indexOfOmitChars(String stringToSearchIn, String stringToSearchFor, char[] omitCharArray) {

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
		
		/* Initialize needed structures */
		char c = sourceString.toCharArray()[0];
		int position = 0;
		int retVal = -1;
		boolean found = false;
		
		// transform the base text into a char array
		char[] charsToSearchIn = stringToSearchIn.toCharArray();
		while (true)
		{
			// the start position of the string to search
			position = stringToSearchIn.indexOf(c,position);
			retVal = position;
			if (position == -1){ // stop, if the first char could not be found
				break;
			} 
			found = true;
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
			{ // if the found flag is still set, we are finished
				break;
			}
		}
		
		return retVal;
	}

	
	/**
	 * This method aligns the normalized texts of the given {@link STextualDS} objects
	 * and <b>also</b> aligns the {@link SToken} including the creation of equivalent {@link SToken}
	 * information.
	 * @param baseText the base {@link STextualDS}
	 * @param otherText the other {@link STextualDS}
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
		int offset = indexOfOmitChars(normalizedBaseText.toLowerCase(),normalizedOtherText.toLowerCase(),this.punctuations);
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
						
					} else {
						nonEquivalentTokenInOtherTexts.add(otherTextToken);
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
		if (sTextualDSs == null){
			this.getDocumentPairs().add(new DocumentStatusPair(sDocument,DOCUMENT_STATUS.DELETED));
		} else if (sTextualDSs.size() == 0){
			this.getDocumentPairs().add(new DocumentStatusPair(sDocument,DOCUMENT_STATUS.DELETED));
		}
		
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
		if (this.pairs == null){
			this.pairs = new Vector<DocumentStatusPair>();
		}
		
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
	
	protected void mergeTokenContent(SDocument base, SDocument other){
		 
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
		mergeTokenContent(base, other);
		mergeSpanContent(base, other);
		mergeStructureContent(base, other);
		return base;
	}
	
	/**
	 * Maps all documents.
	 */
	public void map(){
		this.initialize();
		
		if (this.getSDocuments() != null){
			DocumentStatusPair baseDocPair = null;
			
			
			if (this.getSDocuments().size() >= 2){
				this.initialize();
				
				// normalize all texts
				for (SDocument sDoc : this.getSDocuments()){
					this.normalizeTextualLayer(sDoc);
				}
				
				for (DocumentStatusPair sDocPair : this.getDocumentPairs()){
					if (sDocPair.sDocument.equals(container.getBaseDocument())){
						System.out.println("Chose base document. It is document with id ");
						baseDocPair = sDocPair;
						baseDocPair.status = DOCUMENT_STATUS.IN_PROGRESS;
					}
				} 
				
				/// base text -- < Other Document -- nonEquivalentTokens >
				Hashtable<STextualDS, Hashtable<SDocument,HashSet<SToken>>> nonEquivalentTokenSets = new Hashtable<STextualDS, Hashtable<SDocument,HashSet<SToken>>>();
				
				// allign all texts
				for (DocumentStatusPair sDocPair : this.getDocumentPairs()){
					boolean hasTexts = true;
					if (! sDocPair.sDocument.equals(container.getBaseDocument()))
					{// ignore the base document and align all other
						System.out.println("Merging document");
						if (sDocPair.sDocument.getSDocumentGraph().getSTextualDSs() != null)
						{ // there are possibly texts
							sDocPair.status = DOCUMENT_STATUS.IN_PROGRESS;
							if (sDocPair.sDocument.getSDocumentGraph().getSTextualDSs().size() > 0)
							{ // The other document has at least one text
								HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
								for (STextualDS baseText : container.getBaseDocument().getSDocumentGraph().getSTextualDSs())
								{ // for all texts of the base document
									nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
									for (STextualDS otherText : sDocPair.sDocument.getSDocumentGraph().getSTextualDSs())
									{ // allign the current base text with all texts of the other document
										this.alignTexts(baseText, otherText,nonEquivalentTokenInOtherTexts);
									}
									/// save all unique token of the other document
									if (nonEquivalentTokenSets.containsKey(baseText))
									{
										nonEquivalentTokenSets.get(baseText).put(sDocPair.sDocument, nonEquivalentTokenInOtherTexts);
									} 
									else 
									{
										Hashtable<SDocument,HashSet<SToken>> newTab = new Hashtable<SDocument, HashSet<SToken>>();
										newTab.put(sDocPair.sDocument, nonEquivalentTokenInOtherTexts);
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
								if (sDocPair.sDocument.getSDocumentGraph().getSTokens() != null)
								{ // mark all tokens of the other document as unique
									nonEquivalentTokenInOtherTexts.addAll(sDocPair.sDocument.getSDocumentGraph().getSTokens());
								}
								if (nonEquivalentTokenSets.containsKey(baseText)){
									nonEquivalentTokenSets.get(baseText).put(sDocPair.sDocument, nonEquivalentTokenInOtherTexts);
								} else {
									Hashtable<SDocument,HashSet<SToken>> newTab = new Hashtable<SDocument, HashSet<SToken>>();
									newTab.put(sDocPair.sDocument, nonEquivalentTokenInOtherTexts);
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
				
				for (StextualDS text : baseDoc.getSDocumentGraph().getSTextualDSs())
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
				for (DocumentStatusPair sDocPair : this.getDocumentPairs())
				{ // for all documents
					if (! sDocPair.sDocument.equals(container.getBaseDocument()))
					{// ignore the base document and merge the others
						if (sDocPair.sDocument.getSDocumentGraph().getSTextualDSs() != null)
						{ // there should be texts
							// get the set of tokens in the document which do not have an equivalent in the base text
							HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
							if (nonEquivalentTokenSets.get(this.container.getBaseText()) != null){
								if (nonEquivalentTokenSets.get(this.container.getBaseText()).get(sDocPair.sDocument) != null){
									nonEquivalentTokenInOtherTexts = nonEquivalentTokenSets.get(this.container.getBaseText()).get(sDocPair.sDocument);
								}
							}
							// merge the document content
							this.mergeDocumentContent(baseDocPair.sDocument,sDocPair.sDocument, nonEquivalentTokenInOtherTexts);
							// we are finished with the document. Free the memory
							System.out.println("Finishing document");
							this.container.finishDocument(sDocPair.sDocument);
							sDocPair.status = DOCUMENT_STATUS.DELETED;
						} else {
							// there are no texts. So, just copy everything into the base document graph
							HashSet<SToken> nonEquivalentTokenInOtherTexts = new HashSet<SToken>();
							if (sDocPair.sDocument.getSDocumentGraph().getSTokens() != null){
								// all tokens are unique
								nonEquivalentTokenInOtherTexts.addAll(sDocPair.sDocument.getSDocumentGraph().getSTokens());
							}
							// merge the document content
							this.mergeDocumentContent(baseDocPair.sDocument,sDocPair.sDocument, nonEquivalentTokenInOtherTexts);
							// we are finished with the document. Free the memory
							this.container.finishDocument(sDocPair.sDocument);
							System.out.println("Finishing document");
							sDocPair.status = DOCUMENT_STATUS.DELETED;
						}
					}
				}
				// clear the table of non-equivalent tokens
				nonEquivalentTokenSets.clear();
			}
			
			
			this.container.finishDocument(baseDocPair.sDocument);
			baseDocPair.status = DOCUMENT_STATUS.COMPLETED;
		}
		
		
		
		
	}
	
	/**
	 * Copies annotations from one element to another. If two annotations have
	 * different values, than the method will extend the annotation name to make
	 * coping possible. Existing annotations will not be copied.
	 * 
	 * @param from
	 * @param to
	 */
	public void copyAnnotation(SAnnotatableElement from, SAnnotatableElement to) {
		for (SAnnotation fromAnno : from.getSAnnotations()) {
			SAnnotation toAnno = to.getSAnnotation(fromAnno.getQName());
			if (toAnno == null) {
				toAnno = (SAnnotation) fromAnno.clone();
				to.addSAnnotation(toAnno);
				System.out.println("copied anno: " + toAnno);
			} else if (!toAnno.getSValue().equals(fromAnno.getSValue())) {
				toAnno = (SAnnotation) fromAnno.clone();
				toAnno.setName(toAnno.getName() + ANNO_NAME_EXTENSION);
				to.addSAnnotation(toAnno);
				log.warn(String.format(
						"Changed annotation name \"%s\" to \"%s\"",
						fromAnno.getName(), toAnno.getName()));
			} else {
				// identical annotations
			}
		}
	}
}
