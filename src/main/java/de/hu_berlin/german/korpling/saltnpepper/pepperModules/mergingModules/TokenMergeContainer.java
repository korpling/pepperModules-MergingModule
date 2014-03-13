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

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;

import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.STextualDS;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sDocumentStructure.SToken;

/**
 * 
 * @author eladrion
 *
 */
public class TokenMergeContainer {
		
		/**
		 * This class contains all tokens which were aligned and allows a search for specific {@link SToken} objects
		 * by their start position. Moreover, a search for the start and length of a specific {@link SToken} object
		 * is possible.
		 */
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
			 * This method returns all {@link SToken} objects contained in this {@link AlignedTokensMap}
			 * @return all contained {@link SToken} objects
			 */
			public EList<SToken> getTokens(){
				return new BasicEList(tokenLeftMap.keySet());
			}
			
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
				if (this.tokenLeftMap.containsKey(tok) && this.tokenRightMap.containsKey(tok)){
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
				if (this.tokenLeftMap.containsKey(tok)){
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
				if (this.tokenRightMap.containsKey(tok)){
					return this.tokenRightMap.get(tok);
				} else {
					return -1;
				}
			}
		}
		
		/** The base {@link SDocument} object **/
		SDocument baseDocument= null;
		
		/** The base {@link STextualDS} object **/
		STextualDS baseText=null;
		
		/** 
		 * The token equivalence map. For every {@link SToken} object which has an equivalent {@link SToken} object,
		 * the map contains an hashmap as entry which contains the equivalent {@link SToken} object as value and the
		 * {@link STextualDS} object as key. 
		 **/
		private Hashtable<SToken,Hashtable<STextualDS,SToken>> equivalentToken= null;
		
		/** The map of aligned texts which is a map with the {@link STextualDS} objects as keys and the {@link AlignedTokensMap} as value.**/
		private Hashtable<STextualDS,AlignedTokensMap> alignedTextsMap= null;
		
		/** the map of normalized texts **/
		private Hashtable<STextualDS,String> normalizedTextMap=null;
		
		int maximumNormalizedTextLength = -1;
		
		/** 
		 * This map contains a mapping from normalized index of a character to the index
		 * in the original text for every base {@link STextualDS} objects.
		 **/
		private Hashtable<STextualDS,List<Integer>> normalizedBaseTextToOriginalBaseText = null;
		
		public TokenMergeContainer(){
			this.equivalentToken = new Hashtable<SToken, Hashtable<STextualDS,SToken>>();
			this.alignedTextsMap = new Hashtable<STextualDS, AlignedTokensMap>();
			this.normalizedTextMap = new Hashtable<STextualDS, String>();
			this.normalizedBaseTextToOriginalBaseText = new Hashtable<STextualDS, List<Integer>>();
		}
		
		/**
		 * This method sets the specified mapping from normalized text to the original text for the specified 
		 * {@link STextualDS} object.
		 * @param sTextualDS The {@link STextualDS} object
		 * @param posMapping The mapping list
		 */
		public void setBaseTextPositionByNormalizedTextPosition(STextualDS sTextualDS, List<Integer> posMapping){
			if (! this.normalizedBaseTextToOriginalBaseText.containsKey(sTextualDS)){
				this.normalizedBaseTextToOriginalBaseText.put(sTextualDS, posMapping);
			}
		}
		
		/**
		 * This method returns the position of a character in the original text, specified by the given {@link STextualDS} and
		 * the given position in the normalized text.
		 * @param sTextualDS The {@link STextualDS} object
		 * @param position The position of a character in the normalized text
		 * @return The original position on success and -1 on failure
		 */
		public int getBaseTextPositionByNormalizedTextPosition(STextualDS sTextualDS, int position){
			int baseTextPosition = -1;
			if (this.normalizedBaseTextToOriginalBaseText.containsKey(sTextualDS)){
				if (this.normalizedBaseTextToOriginalBaseText.get(sTextualDS).size() > position){
					baseTextPosition = this.normalizedBaseTextToOriginalBaseText.get(sTextualDS).get(position);
				}
			}
			return baseTextPosition;
		}
		
		/**
		 * This method returns the {@link SToken} objects which overlap the base text.
		 * @return The {@link SToken} objects ov the base text
		 */
		public EList<SToken> getBaseTextToken(){
			return this.alignedTextsMap.get(this.baseText).getTokens();
		}
		
		/**
		 * This method sets the base {@link SDocument}.
		 * @param sDocument The {@link SDocument} to set as base.
		 */
		public void setBaseDocument(SDocument sDocument){
			this.baseDocument = sDocument;
		}
		
		/**
		 * This method returns the base {@link SDocument} object.
		 * @return The base {@link SDocument} or null, if there is no base {@link SDocument}
		 */
		public SDocument getBaseDocument(){
			return this.baseDocument;
		}
		
		/**
		 * This method adds the normalized text, specified by the parameter, for the specified {@link STextualDS} of the specified
		 * {@link SDocument} object. 
		 * @param doc The {@link SDocument} object containing the given text
		 * @param sTextualDS The {@link STextualDS} for which a normalized text should be added
		 * @param normalizedText The normalized text for the given {@link STextualDS}
		 */
		public void addNormalizedText(SDocument doc, STextualDS sTextualDS, String normalizedText){
			if (maximumNormalizedTextLength == -1){
				this.maximumNormalizedTextLength = normalizedText.length();
				this.baseText = sTextualDS;
				this.baseDocument = doc;
			} else {
				if (normalizedText.length() > this.maximumNormalizedTextLength)
				{ // We found a new minimum
					this.maximumNormalizedTextLength = normalizedText.length();
					this.baseText = sTextualDS;
					this.baseDocument = doc;
				}
			}
			System.out.println("!!!!!set basedocument to " + doc);
			System.out.println("!!!!!set base text to \""+this.baseText.getSText()+"\"");
			this.normalizedTextMap.put(sTextualDS, normalizedText);
		}
		
		/**
		 * This method searches the normalized text for the given {@link STextualDS}.
		 * @param sTextualDS The {@link STextualDS} to search the normalized text for.
		 * @return The normalized text or null, if there is no normalized text for the given {@link STextualDS}
		 */
		public String getNormalizedText(STextualDS sTextualDS){
			return this.normalizedTextMap.get(sTextualDS);
		}
		
		/**
		 * This method returns the base text.
		 * @return The base text or null, if there is no such text.
		 */
		public STextualDS getBaseText(){
			return this.baseText;
		}
		
		/**
		 * This method sets the base text to the one specified by the given {@link STextualDS}
		 * @param text The {@link STextualDS} to set the base text to.
		 */
		public void setBaseText(STextualDS text){
			this.baseText = text;
		}
		
		/**
		 * This method returns the {@link SToken} object which is located in the given normalized {@link STextualDS}
		 * at the given start position.
		 * @param sTextualDS The {@link STextualDS} which contains the {@link SToken} at the start position
		 * @param start The start position of the {@link SToken} in the {@link STextualDS}.
		 * @return The {@link SToken} which is located at position start in the given {@link STextualDS} or null, if 
		 * there is no such {@link SToken}.
		 */
		public SToken getAlignedTokenByStart(STextualDS sTextualDS, int start){
			SToken tok = null;
			if (this.alignedTextsMap.containsKey(sTextualDS)){
				tok = this.alignedTextsMap.get(sTextualDS).getTokenByStart(start);
			}
			return tok;
		}
		
		/**
		 * This method returns the start for the given {@link SToken} object in the normalized version
		 * of the given {@link STextualDS}.
		 * @param sTextualDS The {@link STextualDS} to search the start position of the given {@link SToken} for
		 * @param sToken The {@link SToken} to search the start position for
		 * @return The position of the {@link SToken} in the normalized version of the given {@link STextualDS} or -1,
		 * if the {@link SToken} has no position in the given {@link STextualDS}.
		 */
		public int getAlignedTokenStart(STextualDS sTextualDS,SToken sToken){
			int returnVal = -1;
			if (this.alignedTextsMap.containsKey(sTextualDS)){
				AlignedTokensMap map = this.alignedTextsMap.get(sTextualDS);
				returnVal = map.getStart(sToken);
			}
			return returnVal;
		}
		
		/**
		 * This method returns the length for the given {@link SToken} object in the normalized version
		 * of the given {@link STextualDS}.
		 * @param sTextualDS The {@link STextualDS} to search the length of the given {@link SToken} for
		 * @param sToken The {@link SToken} to search the length for
		 * @return The length of the {@link SToken} in the normalized version of the given {@link STextualDS} or -1,
		 * if the {@link SToken} has length in the given {@link STextualDS}.
		 */
		public int getAlignedTokenLength(STextualDS sTextualDS,SToken sToken){
			int returnVal = -1;
			if (this.alignedTextsMap.containsKey(sTextualDS)){
				AlignedTokensMap map = this.alignedTextsMap.get(sTextualDS);
				returnVal = map.getLength(sToken);
			}
			return returnVal;
		}
		
		/**
		 * This method returns the map of aligned {@link SToken} objects for the specified {@link STextualDS}. 
		 * @param text The {@link STextualDS} to search the map of aligned tokens for.
		 * @return The {@link AlignedTokensMap} for the specified {@link STextualDS} or null if there is no such map.
		 */
		public AlignedTokensMap getAlignedTokens(STextualDS text){
			return this.alignedTextsMap.get(text);
		}
		
		/**
		 * This method adds the given {@link SToken} which is contained in the given {@link STextualDS} at the
		 * normalized left and right position. 
		 * @param text The {@link STextualDS} which contains the given {@link SToken}
		 * @param tok The {@link SToken} object to add
		 * @param left The start of the {@link SToken} in the normalized version of the {@link STextualDS}
		 * @param right The end of the {@link SToken} in the normalized version of the {@link STextualDS}
		 */
		public void addAlignedToken(STextualDS text, SToken tok, int left, int right){
			//System.out.println("Aligned Token "+tok.getSName() +" in SText "+ text.getSElementId()+ " with start/end:"+left+"/"+right);
			if (this.alignedTextsMap.containsKey(text)){
				this.alignedTextsMap.get(text).addToken(tok, left, right);
			} else {
				AlignedTokensMap map = new AlignedTokensMap();
				map.addToken(tok, left, right);
				this.alignedTextsMap.put(text, map);
				
			}
		}
		
		/**
		 * This method adds an equivalence mapping for the given {@link SToken} of the base text to the given
		 * {@link SToken} in some other {@link STextualDS} which is specified as parameter
		 * @param baseTextToken The base {@link SToken} to add a mapping for.
		 * @param otherTextToken The {@link SToken} which is a mapping for the base {@link SToken}
		 * @param otherSText The {@link STextualDS} which is connected to the mapped {@link SToken}
		 */
		public void addTokenMapping(SToken baseTextToken, SToken otherTextToken, STextualDS otherSText){
			if (otherTextToken.equals(baseTextToken)){
				System.out.println("Sorry, you tried to add a token as it's own mapping");
				return;
			}
		System.out.println("Adding mapping for base text token "
				+ baseTextToken.getSDocumentGraph().getSDocument().getSId()
				+ "/"
				+ baseTextToken.getSName() + " and token "
				+ otherTextToken.getSDocumentGraph().getSDocument().getSId()
				+ "/"
				+ otherTextToken.getSName());
		if (this.equivalentToken.get(baseTextToken) != null)
			{// there is a mapping for the base text token
				if (! this.equivalentToken.get(baseTextToken).containsKey(otherSText))
				{ // there is no mapping for the base text token in the other document. Add the mapping
					//if (! this.equivalentToken.get(baseTextToken).containsKey(arg0))
					this.equivalentToken.get(baseTextToken).put(otherSText, otherTextToken);
				} 
				else 
				{ // there is a mapping for the base token in the otherSText
					if (this.equivalentToken.get(baseTextToken).get(otherSText).equals(otherTextToken)){
						System.out.println("Info: There is already a mapping for the base SToken "+baseTextToken.getSName()+" in STextualDS "+otherSText.getSName()+ " to token "+otherTextToken.getSName());
					} else {
						System.out.println("Warning: There is already a mapping for the base SToken "+baseTextToken.getSName()+" in STextualDS "+otherSText.getSName()+ " to some other token which is not "+otherTextToken.getSName());
					}
					
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
			if (this.equivalentToken.containsKey(baseTextToken))
			{// the base text token has an equivalent in SOME other document
				/// get the equivalent token of the other document if there is one
				equivalentToken = this.equivalentToken.get(baseTextToken).get(otherSText);
			}
			return equivalentToken;
		}
		
		/**
		 * This method returns the map of equivalences for the base text tokens.
		 * @return The equivalence map.
		 */
		public Hashtable<SToken,Hashtable<STextualDS,SToken>> getEquivalenceMap(){
			return this.equivalentToken;
		}
		
		/**
		 * This method frees the memory used by the specified {@SDocument} in the {@link TokenMergeContainer}.
		 * @param sDocument The {@SDocument}
		 */
		public void finishDocument(SDocument sDocument){
			if (this.alignedTextsMap.containsKey(sDocument)){
				this.alignedTextsMap.remove(sDocument);
			}
			if (this.normalizedTextMap.containsKey(sDocument)){
				this.normalizedTextMap.remove(sDocument);
			}
			if (sDocument== this.baseDocument){
				this.normalizedBaseTextToOriginalBaseText.clear();
			}
			for (SToken tok : this.equivalentToken.keySet()){
				Hashtable<STextualDS, SToken> map = this.equivalentToken.get(tok);
				if (map != null && map.containsKey(sDocument)){
					this.equivalentToken.get(tok).remove(sDocument);
				}
			}
			
		}
	

}
