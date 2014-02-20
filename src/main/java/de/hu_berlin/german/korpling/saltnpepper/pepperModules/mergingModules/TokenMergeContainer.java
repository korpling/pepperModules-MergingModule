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

import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;

/**
 * 
 * @author eladrion
 *
 */
public class TokenMergeContainer {
		
		public class AlignedTokensMap{
			public AlignedTokensMap(){
				this.tokenLeftMap = new Hashtable<SToken, Integer>();
				this.tokenRightMap = new Hashtable<SToken, Integer>();
				this.tokensByStart = new Hashtable<Integer, SToken>();
			}
			
			private Hashtable<SToken,Integer> tokenLeftMap=null;
			private Hashtable<SToken,Integer> tokenRightMap=null;
			private Hashtable<Integer,SToken> tokensByStart=null;
			
			/**
			 * This method adds a {@link SToken} with its left and right index to the internal structures.
			 * @param tok the {@link SToken} to add
			 * @param left the start value
			 * @param right the end value
			 */
			public void addToken(SToken tok, int left, int right){
				this.tokenLeftMap.put(tok, left);
				this.tokenRightMap.put(tok, right);
				this.tokensByStart.put(left, tok);
			}
			
			/**
			 * This method searches a {@link SToken} specified by the start
			 * @param start the start value
			 * @return The {@link SToken} object or null on failure
			 */
			public SToken getTokenByStart(int start){
				return this.tokensByStart.get(start);
			}
			
			/**
			 * This method returns the length of the given {@link SToken} aligned to the normalized text.
			 * @param tok the {@link SToken} to search the length for
			 * @return the length or -1 on failure
			 */
			public int getLength(SToken tok){
				if (this.tokenLeftMap.contains(tok) && this.tokenRightMap.contains(tok)){
					return this.tokenRightMap.get(tok) - this.tokenLeftMap.get(tok);
				} else {
					return -1;
				}
				
			}
			
			/**
			 * This method returns the start index of the given {@link SToken} aligned to the normalized text.
			 * @param tok the {@link SToken} to search the start for
			 * @return the start index or -1 on failure
			 */
			public int getStart(SToken tok){
				if (this.tokenLeftMap.contains(tok)){
					return this.tokenLeftMap.get(tok);
				} else {
					return -1;
				}
				
			}
			
			/**
			 * This method returns the end index of the given {@link SToken} aligned to the normalized text.
			 * @param tok the {@link SToken} to search the end for
			 * @return the end index or -1 on failure
			 */
			public int getEnd(SToken tok){
				if (this.tokenRightMap.contains(tok)){
					return this.tokenRightMap.get(tok);
				} else {
					return -1;
				}
			}
		}
		
		SDocument baseDocument= null;
		
		STextualDS baseText=null;
		
		Hashtable<SToken,Hashtable<STextualDS,SToken>> equivalentToken= null;
		
		Hashtable<STextualDS,AlignedTokensMap> alignedTextsMap= null;
		
		Hashtable<STextualDS,String> normalizedTextMap=null;
		
		int countOfChangedChars = -1;
		
		public TokenMergeContainer(){
			this.equivalentToken = new Hashtable<SToken, Hashtable<STextualDS,SToken>>();
			this.alignedTextsMap = new Hashtable<STextualDS, AlignedTokensMap>();
			this.normalizedTextMap = new Hashtable<STextualDS, String>();
		}
		
		public void setBaseDocument(SDocument sDocument){
			this.baseDocument = sDocument;
		}
		
		public SDocument getBaseDocument(){
			return this.baseDocument;
		}
		
		public void addNormalizedText(SDocument doc, STextualDS sTextualDS, String normalizedText, int countOfChangedChars){
			if (countOfChangedChars == -1){
				this.countOfChangedChars = countOfChangedChars;
				this.baseText = sTextualDS;
				this.baseDocument = doc;
			} else {
				if (countOfChangedChars < this.countOfChangedChars)
				{ // We found a new minimum
					this.countOfChangedChars = countOfChangedChars;
					this.baseText = sTextualDS;
					this.baseDocument = doc;
				}
			}
			this.normalizedTextMap.put(sTextualDS, normalizedText);
		}
		
		public String getNormalizedText(STextualDS sTextualDS){
			return this.normalizedTextMap.get(sTextualDS);
		}
		
		public STextualDS getBaseText(){
			return this.baseText;
		}
		
		public SToken getAlignedTokenByStart(STextualDS sTextualDS, int start){
			SToken tok = null;
			if (this.alignedTextsMap.contains(sTextualDS)){
				this.alignedTextsMap.get(sTextualDS).getTokenByStart(start);
			}
			return tok;
		}
		
		public int getAlignedTokenStart(STextualDS sTextualDS,SToken sToken){
			int returnVal = -1;
			if (this.alignedTextsMap.contains(sTextualDS)){
				AlignedTokensMap map = this.alignedTextsMap.get(sTextualDS);
				returnVal = map.getStart(sToken);
			}
			return returnVal;
		}
		
		public int getAlignedTokenLength(STextualDS sTextualDS,SToken sToken){
			int returnVal = -1;
			if (this.alignedTextsMap.contains(sTextualDS)){
				AlignedTokensMap map = this.alignedTextsMap.get(sTextualDS);
				returnVal = map.getLength(sToken);
			}
			return returnVal;
		}
		
		public void addAlignedToken(STextualDS text, SToken tok, int left, int right){
			if (this.alignedTextsMap.contains(text)){
				this.alignedTextsMap.get(text).addToken(tok, left, right);
			} else {
				AlignedTokensMap map = new AlignedTokensMap();
				map.addToken(tok, left, right);
				this.alignedTextsMap.put(text, map);
				
			}
		}
		
		public void addTokenMapping(SToken baseTextToken, SToken otherTextToken, STextualDS otherSText){
			if (this.equivalentToken.get(baseTextToken) != null)
			{// there is a mapping for the base text token
				if (! this.equivalentToken.get(baseTextToken).contains(otherSText))
				{ // there is no mapping for the base text token in the other document. Add the mapping
					this.equivalentToken.get(baseTextToken).put(otherSText, otherTextToken);
				}
			} 
			else 
			{// there is currently no mapping for the specified base text token
				Hashtable<STextualDS,SToken> newMapping = new Hashtable<STextualDS, SToken>();
				newMapping.put(otherSText, otherTextToken);
				this.equivalentToken.put(baseTextToken, newMapping);
			}
		}
		
		/**
		 * This method gives access to the TokenMergeContainer and returns the equivalent {@link SToken} in the
		 * specified {@link STextualDS} for the specified {@link SToken} in the base text/document.
		 * @param baseTextToken the {@link SToken} for which an equivalent {@link SToken} shall be searched
		 * @param otherSText the {@link STextualDS} to search for an equivalent token
		 * @return the equivalent {@link SToken} if existent and null, else.
		 */
		public SToken getTokenMapping(SToken baseTextToken, STextualDS otherSText){
			SToken equivalentToken = null;
			if (this.equivalentToken.contains(baseTextToken))
			{// the base text token has an equivalent in SOME other document
				/// get the equivalent token of the other document if there is one
				equivalentToken = this.equivalentToken.get(baseTextToken).get(otherSText);
			}
			return equivalentToken;
		}
		
		/**
		 * This method frees the memory used by the specified {@SDocument} in the {@link TokenMergeContainer}.
		 * @param sDocument The {@SDocument}
		 */
		public void finishDocument(SDocument sDocument){
			if (this.alignedTextsMap.contains(sDocument)){
				this.alignedTextsMap.remove(sDocument);
			}
			if (this.normalizedTextMap.contains(sDocument)){
				this.normalizedTextMap.remove(sDocument);
			}
			for (SToken tok : this.equivalentToken.keySet()){
				if (this.equivalentToken.get(tok).contains(sDocument)){
					this.equivalentToken.get(tok).remove(sDocument);
				}
			}
			
		}
	

}
