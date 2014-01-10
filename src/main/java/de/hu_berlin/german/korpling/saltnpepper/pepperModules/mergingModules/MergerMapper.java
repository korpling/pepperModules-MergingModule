package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules;

import java.util.List;
import java.util.Vector;

import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;

public class MergerMapper {

	public static class DocumentStatusPair{
		public DocumentStatusPair(SDocument sDocument){
			this.sDocument= sDocument;
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
	 * Maps all documents.
	 */
	public void map(){
		
	}
}
