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

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import de.hu_berlin.german.korpling.saltnpepper.pepper.pepperExceptions.PepperModuleException;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualRelation;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;

public class MergerMapper {

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
	
	// the {@link TokenMergeContainer} instance
	private TokenMergeContainer container =null;
		
	/**
	 * This method sets the {@link TokenMergeContainer} for this mapper.
	 * @param container the {@link TokenMergeContainer}
	 */
	public void setTokenMergeContainer(TokenMergeContainer container){
		this.container = container;
	}	
	
	// This table contains the escape sequences for all characters
	private Hashtable<Character,String> escapeTable= null;
	
	/**
	 * This method aligns the normalized texts of the given {@link STextualDS} objects
	 * and <b>also</b> aligns the {@link SToken} including the creation of equivalent {@link SToken}
	 * information.
	 * @param baseText the base {@link STextualDS}
	 * @param otherText the other {@link STextualDS}
	 * @return true on success and false on failure
	 * @author eladrion
	 */
	private boolean alignTexts(STextualDS baseText, STextualDS otherText){
		if (baseText == null)
			throw new PepperModuleException("Cannot align the Text of the documents since the base SDocument reference is NULL");
		if (otherText == null)
			throw new PepperModuleException("Cannot align the Text of the documents since the other SDocument reference is NULL");
		
		
		
		boolean returnVal = false;
		// first we need the two normalized texts
		String normalizedBaseText = this.container.getNormalizedText(baseText);
		String normalizedOtherText = this.container.getNormalizedText(otherText);
		int offset = normalizedBaseText.indexOf(normalizedOtherText);
		if (offset != -1)
		{// if the normalized other text is conatined in the normalized base text
			// get the tokens of the other document sorted by the text.
			// TODO: do not use the SORT FUNCTION
			EList<SToken> sortedTokens = otherText.getSDocumentGraph().getSortedSTokenByText();
			for (SToken otherTextToken : sortedTokens)
			{
				// get the aligned token start and length
				int otherTokenStart = this.container.getAlignedTokenStart(otherText, otherTextToken);
				int otherTokenLength = this.container.getAlignedTokenLength(otherText, otherTextToken);
				if (otherTokenStart != -1 && otherTokenLength != -1)
				{ // get the base text token:
					// get the aligned token from the base document which has the start of offset+startOfOtherToken
					SToken baseTextToken = this.container.getAlignedTokenByStart(baseText, (otherTokenStart+offset));
					if (baseTextToken != null)
					{// there is some baseTextToken which has the same start
						if (this.container.getAlignedTokenLength(baseText, baseTextToken) == otherTokenLength)
						{ // start and lengths are identical. We found an equivalence class
							this.container.addTokenMapping(baseTextToken, otherTextToken, otherText);
						} else {
							//TODO: ERROR CATCHING
						}
						
					} else {
						// Nothing to do since there is no equivalent token in the base text
					}
				} else {
					//TODO: ERROR CATCHING
				}
			}
			
		}
		// get base text
		return returnVal;
	}
	
	/**
	 * This method normalizes the textual layer for the given {@link SDocument}.
	 * Note: only the normalization is done. The equivalent {@link SToken} are not
	 * determined in any way. For this functionality, you need to use {@link alignDocuments}.
	 * @param sDocument the {@link SDocument} for which the textual layer should be normalized.
	 * @author eladrion
	 */
	private void normalizeTextualLayer(SDocument sDocument){
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
			int normalizedTokenLeft=0;
			
			int currentLeft = 0;
			int currentOffset = 0;
			int countOfChangedChars = 0;
			
			// normalize the text
			for (char c : sTextualDS.getSText().toCharArray()){
				String stringToEscape = this.escapeTable.get(c);
				if (stringToEscape != null){
					normalizedTextBuilder.append(stringToEscape);
					// set new offset: 
					// for whitespaces and punctuation, it is offset += 1
					// for Umlauts, it is offset -= 1
					if (stringToEscape.length() == 0){
						currentOffset += 1;
					} else if (stringToEscape.length() == 2){
						// an Umlaut was unfold --> offset += 1
						currentOffset += 1;
					}
					currentOffset = currentOffset + (1 - stringToEscape.length());
					countOfChangedChars += 1;
				} else {
					normalizedTextBuilder.append(c);
				}
				
				if (currentToken == null)
				{// If we are currently NOT iterating over a token's interval
					currentToken = tokensMappedByLeft.get(currentLeft);
					if (currentToken != null)
					{// if a token interval begins at the current left value
						normalizedTokenLeft = currentLeft + currentOffset;
					}
				} 
				else 
				{// If we ARE currently iterating over a token's interval
					if (tokensMappedByRight.contains((currentLeft+1)))
					{// if we reached the original end-char of the token
						// beware: the SEnd value of a STextalRelation is the last char index of the token +1
						container.addAlignedToken(sTextualDS, currentToken, normalizedTokenLeft, (currentLeft+currentOffset) );
						// reinitialize the normalizedTokenLeft and unmark the now processed token
						normalizedTokenLeft = 0;
						currentToken = null;
					}
				}
				currentLeft += 1;
			}
			// now we have the normalized text
			normalizedText = normalizedTextBuilder.toString();
			// add it to the tokenMergeContainer
			this.container.addNormalizedText(sDocument, sTextualDS, normalizedText, countOfChangedChars);
		}
	}
	
	/**
	 * This method initializes the mapping.
	 * @param documents the {@link SDocument} objects to merge
	 * @author eladrion
	 */
	private void initialize(EList<SDocument> documents){
		if (this.pairs == null){
			this.pairs = new Vector<DocumentStatusPair>();
		}
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
		this.escapeTable.put('>', "");
		
		// normalize all documents
		for (SDocument doc : documents){
			this.normalizeTextualLayer(doc);
		}
	}
	
	/**
	 * Maps all documents.
	 */
	public void map(){
		
		
		
		
	}
}
