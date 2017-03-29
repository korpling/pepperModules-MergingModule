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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.core.SelfTestDesc;
import org.corpus_tools.pepper.exceptions.PepperFWException;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.modules.DocumentController;
import org.corpus_tools.pepper.modules.MappingSubject;
import org.corpus_tools.pepper.modules.PepperManipulator;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperMapperController;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.peppermodules.mergingModules.util.Multimap;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.util.SaltUtil;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(name = "MergerComponent", factory = "PepperManipulatorComponentFactory")
public class Merger extends PepperManipulatorImpl implements PepperManipulator {
	public static final String MODULE_NAME = "Merger";
	private static final Logger logger = LoggerFactory.getLogger(MODULE_NAME);

	public Merger() {
		super();
		setName(MODULE_NAME);
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI("https://github.com/korpling/pepperModules-MergingModule"));
		setDesc("The Merger allows to merge an unbound number of corpora to a single corpus. ");
		setProperties(new MergerProperties());
	}

	@Override
	public boolean isReadyToStart() {
		if (getModuleController().getJob().getMaxNumberOfDocuments() < 2) {
			throw new PepperModuleException(this,
					"The merger cannot work with less than 2 documents in main memory in parallel. Please check the property '"
							+ PepperConfiguration.PROP_MAX_AMOUNT_OF_SDOCUMENTS + "' in the Pepper configuration. ");
		}
		return (true);
	};

	@Override
	public SelfTestDesc getSelfTestDesc() {
		getProperties().setPropertyValue(MergerProperties.PROP_FIRST_AS_BASE, true);
		getProperties().setPropertyValue(MergerProperties.PROP_COPY_NODES, true);
		final URI baseInputCorpus = getResources().appendSegment("selfTests").appendSegment("in");
		final URI morphCorpus = baseInputCorpus.appendSegment("morph");
		final URI rstCorpus = baseInputCorpus.appendSegment("rst");
		final URI syntaxCorpus = baseInputCorpus.appendSegment("syntax");
		final URI expectedCorpus = getResources().appendSegment("selfTests").appendSegment("expected");

		return SelfTestDesc.create().withInputCorpusPath(rstCorpus).withInputCorpusPath(syntaxCorpus)
				.withInputCorpusPath(morphCorpus).withExpectedCorpusPath(expectedCorpus).build();
	}

	/**
	 * A table containing the import order for {@link Identifier} corresponding
	 * to {@link SDocument} and {@link SCorpus} nodes corresponding to the
	 * {@link SCorpusGraph} they are contained in.
	 **/
	private Map<SCorpusGraph, List<Identifier>> importOrder = null;

	/**
	 * a map containing all mapping partners ({@link SCorpus} and
	 * {@link SDocument} nodes) corresponding to their sId.
	 **/
	protected Multimap mappingTable = null;

	/**
	 * Determines which {@link SCorpusGraph} is the base corpus graph, in which
	 * everything has to be merged in.
	 **/
	private SCorpusGraph baseCorpusStructure = null;

	/**
	 * Returns the {@link SCorpusGraph} is the base corpus graph, in which
	 * everything has to be merged in.
	 * 
	 * @return
	 */
	public SCorpusGraph getBaseCorpusStructure() {
		return baseCorpusStructure;
	}

	/**
	 * Sets the {@link SCorpusGraph} is the base corpus graph, in which
	 * everything has to be merged in.
	 * 
	 * @param baseCorpusStructure
	 */
	public void setBaseCorpusStructure(SCorpusGraph baseCorpusStructure) {
		this.baseCorpusStructure = baseCorpusStructure;
	}

	/**
	 * Creates a table of type {@link Multimap}, which contains a slot of
	 * matching elements as value. The key is the {@link Identifier}. Only real
	 * existing elements are contained in table.
	 */
	protected synchronized void createMapping() {
		if (mappingTable == null) {
			setBaseCorpusStructure(getSaltProject().getCorpusGraphs().get(0));
			// initialize importOrder
			importOrder = new HashMap<>();
			for (SCorpusGraph graph : getSaltProject().getCorpusGraphs()) {
				importOrder.put(graph, new ArrayList<Identifier>());
			}

			mappingTable = new Multimap();
			// TODO add mapping properties to table
			for (SCorpusGraph graph : getSaltProject().getCorpusGraphs()) {
				if (!graph.getCorpora().isEmpty()) {
					for (SCorpus sCorpus : graph.getCorpora()) {
						// TODO check if sCorpus.getId() is contained in
						// mapping properties
						mappingTable.put(sCorpus.getId(), sCorpus);
					}
					for (SDocument sDocument : graph.getDocuments()) {

						// TODO check if sDocument.getId() is contained in
						// mapping properties
						mappingTable.put(sDocument.getId(), sDocument);
					}
				}
			}
			// compute import order
			// for each corpus graph create an empty list in listOfLists
			List<List<List<SNode>>> listOfLists = new ArrayList<>(getSaltProject().getCorpusGraphs().size());
			for (int i = 0; i < getSaltProject().getCorpusGraphs().size(); i++) {
				listOfLists.add(new ArrayList<List<SNode>>());
			}
			// for each id in mappingTable add their nodes to mappingTable
			for (String key : mappingTable.keySet()) {
				List<SNode> nodes = mappingTable.get(key);
				listOfLists.get(nodes.size() - 1).add(nodes);
			}

			// create import order in descending order of listOfLists
			for (int i = getSaltProject().getCorpusGraphs().size(); i > 0; i--) {
				List<List<SNode>> list = listOfLists.get(i - 1);
				for (List<SNode> nodes : list) {
					for (SNode node : nodes) {
						if (node instanceof SDocument) {
							importOrder.get(((SDocument) node).getGraph()).add(node.getIdentifier());
						}
					}
				}
			}
		}
	}

	/**
	 * Creates an import order for each {@link SCorpusGraph} object. The order
	 * for given {@link SCorpusGraph} objects is very similar or equal, in case
	 * they contain the same {@link SDocument}s (the ones to be merged).
	 */
	@Override
	public List<Identifier> proposeImportOrder(SCorpusGraph sCorpusGraph) {
		List<Identifier> retVal = null;
		if (sCorpusGraph != null) {
			if (getSaltProject().getCorpusGraphs().size() > 1) {
				createMapping();
				if (importOrder != null) {
					retVal = importOrder.get(sCorpusGraph);
				}
			}
		}
		return (retVal);
	}

	/** This table stores all corresponding mergable {@link Identifier}. */
	private Map<String, List<Identifier>> givenSlots = null;

	/**
	 * For each {@link SCorpus} and {@link SDocument} in mapping table which has
	 * no corresponding one in base corpus-structure one is created.
	 */
	private void enhanceBaseCorpusStructure() {
		Set<String> keys = mappingTable.keySet();
		if ((keys != null) && (keys.size() > 0)) {
			for (String key : keys) {
				List<SNode> slot = mappingTable.get(key);
				boolean noBase = true;
				boolean isDoc = true;
				for (SNode node : slot) {
					if (node != null) {
						if (node instanceof SCorpus) {
							isDoc = false;
							if (((SCorpus) node).getGraph().equals(getBaseCorpusStructure())) {
								noBase = false;
								break;
							}
						} else if (node instanceof SDocument) {
							isDoc = true;
							if (((SDocument) node).getGraph().equals(getBaseCorpusStructure())) {
								noBase = false;
								break;
							}
						}
					}
				}
				if (noBase) {
					if (isDoc) {
						getBaseCorpusStructure().createCorpus(URI.createURI(key).trimSegments(1));
						SDocument doc = getBaseCorpusStructure().createDocument(URI.createURI(key));
						doc.setDocumentGraph(SaltFactory.createSDocumentGraph());
					} else {
						getBaseCorpusStructure().createCorpus(URI.createURI(key));
					}
				}
			}
		}
	}

	// =========================> synchronization to avoid deadlocks in mapper
	private volatile Lock mergerMappersLock = new ReentrantLock();
	private volatile Condition mergerMappersCondition = mergerMappersLock.newCondition();
	private volatile int numberOfMergerMappers = 0;

	/**
	 * waits until less or equal merger-mappers are active than the half of the
	 * maximal amount of documents are this behavior should prevent from
	 * possible deadlocks in merger mapper, when a base document is blocked and
	 * the mapper waits for a permission to load the 'other' document in main
	 * memory. For instance when 2 mappers are active and only 2 documents are
	 * allowed to be loaded: when both mappers have loaded the base document no
	 * place is left for the 'other document', so they will block each other.
	 */
	private void waitForMergerMapper() {
		mergerMappersLock.lock();
		try {
			if (getModuleController() != null && getModuleController().getJob() != null) {
				while (numberOfMergerMappers >= Double
						.valueOf(Math.floor(getModuleController().getJob().getMaxNumberOfDocuments() / 2)).intValue()) {
					mergerMappersCondition.await();
				}
			}
			numberOfMergerMappers++;
		} catch (InterruptedException e) {
			throw new PepperModuleException(this,
					"A problem occured in deadlock permission for merger mapper processes. ", e);
		} finally {
			mergerMappersLock.unlock();
		}
	}

	/**
	 * Reduces the internal count of active mappers by one.
	 * 
	 * @see #waitForMergerMapper()
	 */
	public void releaseMergerMapper() {
		mergerMappersLock.lock();
		try {
			numberOfMergerMappers--;
			mergerMappersCondition.signal();
		} finally {
			mergerMappersLock.unlock();
		}
	}

	// ===========================< synchronization to avoid deadlocks in mapper
	/**
	 * a set of {@link Identifier} corresponding to documents for which the
	 * merging have not been started
	 **/
	private Set<String> documentsToMerge = new HashSet<>();

	/**
	 * {@inheritDoc PepperModule#start()} Overrides parent method, to enable the
	 * parallel working in more than one {@link DocumentController} objects at a
	 * time.
	 */
	@Override
	public void start() throws PepperModuleException {
		if (getSaltProject() == null) {
			throw new PepperFWException("No salt project was set in module '" + getName() + ", " + getVersion() + "'.");
		}
		if (mappingTable == null) {
			// nothing to be done here

			logger.warn("[Merger] Cannot merge corpora or documents, since only one corpus structure is given. ");

			boolean isStart = true;
			Identifier sElementId = null;
			DocumentController documentController = null;
			while ((isStart) || (sElementId != null)) {
				isStart = false;
				documentController = this.getModuleController().next();
				if (documentController == null) {
					break;
				}
				sElementId = documentController.getDocumentId();
				getModuleController().complete(documentController);
			}
			this.end();

			return;
		}
		enhanceBaseCorpusStructure();
		if ((logger.isDebugEnabled()) && (mappingTable != null)) {
			StringBuilder mergerMapping = new StringBuilder();
			mergerMapping.append("Computed mapping for merging:\n");
			for (String key : mappingTable.keySet()) {
				List<SNode> partners = mappingTable.get(key);
				mergerMapping.append("\t");
				boolean isFirst = true;
				mergerMapping.append("(");
				for (SNode partner : partners) {
					if (!isFirst) {
						mergerMapping.append(", ");
					} else {
						isFirst = false;
					}
					mergerMapping.append(SaltUtil.getGlobalId(partner.getIdentifier()));
				}
				mergerMapping.append(")");
				mergerMapping.append("\n");
			}
			logger.debug("[Merger] " + mergerMapping.toString());
		}
		// creating new thread group for mapper threads
		setMapperThreadGroup(new ThreadGroup(Thread.currentThread().getThreadGroup(), this.getName() + "_mapperGroup"));
		givenSlots = new Hashtable<>();
		boolean isStart = true;
		Identifier sElementId = null;
		DocumentController documentController = null;
		while ((isStart) || (sElementId != null)) {
			isStart = false;
			documentController = getModuleController().next();
			if (documentController == null) {
				break;
			}
			sElementId = documentController.getDocumentId();
			getDocumentId2DC().put(SaltUtil.getGlobalId(sElementId), documentController);

			List<SNode> mappableSlot = mappingTable.get(sElementId.getId());
			List<Identifier> givenSlot = givenSlots.get(sElementId.getId());
			if (givenSlot == null) {
				givenSlot = new ArrayList<>();
				givenSlots.put(sElementId.getId(), givenSlot);
			}
			givenSlot.add(sElementId);
			logger.trace("[Merger] New document has arrived {}. ", SaltUtil.getGlobalId(sElementId));
			documentsToMerge.add(SaltUtil.getGlobalId(sElementId));

			// send all documents to sleep
			if (logger.isTraceEnabled()) {
				logger.trace("[Merger] " + "Waiting for further documents, {} documents are in queue. ",
						documentsToMerge.size());
			}
			documentController.sendToSleep_FORCE();
			// this is a bit hacky, but necessary
			if (documentController.isAsleep()) {
				getModuleController().getJob().releaseDocument(documentController);
				logger.trace("[Merger] " + "Sent document '{}' to sleep. ", documentController.getGlobalId());
			} else {
				logger.warn("Was not able to send document '{}' to sleep. ", documentController.getGlobalId());
			}
			if (givenSlot.size() == mappableSlot.size()) {
				try {
					for (Identifier sDocumentId : givenSlot) {
						DocumentController docController = getDocumentId2DC().get(SaltUtil.getGlobalId(sDocumentId));
						if (docController == null) {
							throw new PepperModuleException(this, "Cannot find a document controller for document '"
									+ SaltUtil.getGlobalId(sDocumentId) + "' in list: " + getDocumentId2DC() + ". ");
						}
						documentsToMerge.remove(docController.getGlobalId());
					}
					// waits until enough spaces for documents is available to
					// start mapper
					waitForMergerMapper();

					start(sElementId);
				} catch (Exception e) {
					throw new PepperModuleException(
							"Any exception occured while merging documents corresponding to '" + sElementId + "'. ", e);
				}
			}
		}

		Collection<PepperMapperController> controllers = null;
		Set<PepperMapperController> alreadyWaitedFor = new HashSet<>();
		// wait until all documents are processed
		controllers = Collections.synchronizedCollection(this.getMapperControllers().values());
		for (PepperMapperController controller : controllers) {
			try {
				controller.join();
				alreadyWaitedFor.add(controller);
			} catch (InterruptedException e) {
				throw new PepperFWException(
						"Cannot wait for mapper thread '" + controller + "' in " + this.getName() + " to end. ", e);
			}
		}

		Collection<SCorpus> corpora = Collections.synchronizedCollection(getBaseCorpusStructure().getCorpora());
		for (SCorpus sCorpus : corpora) {
			start(sCorpus.getIdentifier());
		}
		// wait until all corpora are processed
		for (PepperMapperController controller : controllers) {
			try {
				controller.join();
				alreadyWaitedFor.add(controller);
			} catch (InterruptedException e) {
				throw new PepperFWException(
						"Cannot wait for mapper thread '" + controller + "' in " + this.getName() + " to end. ", e);
			}
		}
		end();
	}

	/**
	 * Removes all corpus-structures except the base corpus-structure
	 */
	@Override
	public void end() throws PepperModuleException {
		List<SCorpusGraph> removeCorpusStructures = new ArrayList<>();
		Iterator<SCorpusGraph> it = getSaltProject().getCorpusGraphs().iterator();
		while (it.hasNext()) {
			SCorpusGraph graph = it.next();
			if (graph != getBaseCorpusStructure()) {
				removeCorpusStructures.add(graph);
			}
		}
		if (removeCorpusStructures.size() > 0) {
			for (SCorpusGraph graph : removeCorpusStructures) {
				getSaltProject().removeCorpusGraph(graph);
			}
		}
		if (getSaltProject().getCorpusGraphs().size() != 1) {
			logger.warn(
					"Could not remove all corpus-structures from salt project which are not the base corpus-structure. Left structures are: '"
							+ removeCorpusStructures + "'. ");
		}
	}

	/**
	 * Creates a {@link PepperMapper} of type {@link MergerMapper}. Therefore
	 * the table {@link #givenSlots} must contain an entry for the given
	 * {@link Identifier}. The create methods passes all documents and corpora
	 * given in the entire slot to the {@link MergerMapper}.
	 **/
	@Override
	public PepperMapper createPepperMapper(Identifier sElementId) {
		MergerMapper mapper = new MergerMapper();
		if (sElementId.getIdentifiableElement() instanceof SDocument) {
			mapper.setMerger(this);
			if ((givenSlots == null) || (givenSlots.size() == 0)) {
				throw new PepperModuleException(this,
						"This should not have been happend and seems to be a bug of module. The problem is, that 'givenSlots' is null or empty in method 'createPepperMapper()'");
			}
			List<Identifier> givenSlot = givenSlots.get(sElementId.getId());
			if (givenSlot == null) {
				throw new PepperModuleException(this,
						"This should not have been happend and seems to be a bug of module. The problem is, that a 'givenSlot' in 'givenSlots' is null or empty in method 'createPepperMapper()'. The sElementId '"
								+ sElementId + "' was not contained in list: " + givenSlots);
			}
			boolean noBase = true;
			for (Identifier id : givenSlot) {
				MappingSubject mappingSubject = new MappingSubject();
				mappingSubject.setIdentifier(id);
				mappingSubject.setMappingResult(DOCUMENT_STATUS.IN_PROGRESS);

				if (sElementId.getIdentifiableElement() instanceof SDocument) {
					DocumentController documentController = getDocumentId2DC().get(SaltUtil.getGlobalId(id));
					mappingSubject.setDocumentController(documentController);
				}
				mapper.getMappingSubjects().add(mappingSubject);
				if (getBaseCorpusStructure() == (((SDocument) id.getIdentifiableElement()).getGraph())) {
					noBase = false;
				}
			}
			if (noBase) {// no corpus in slot containing in base
							// corpus-structure was found
				MappingSubject mappingSubject = new MappingSubject();
				SNode baseSNode = getBaseCorpusStructure().getNode(sElementId.getId());
				if (baseSNode == null) {
					throw new PepperModuleException(this, "Cannot create a mapper for '"
							+ SaltUtil.getGlobalId(sElementId) + "', since no base SNode was found. ");
				}
				mappingSubject.setIdentifier(baseSNode.getIdentifier());
				mappingSubject.setMappingResult(DOCUMENT_STATUS.IN_PROGRESS);
				mapper.getMappingSubjects().add(mappingSubject);
			}
		} else if (sElementId.getIdentifiableElement() instanceof SCorpus) {
			List<SNode> givenSlot = mappingTable.get(sElementId.getId());
			if (givenSlot == null) {
				throw new PepperModuleException(this,
						"This should not have been happend and seems to be a bug of module. The problem is, that a 'givenSlot' in 'givenSlots' is null or empty in method 'createPepperMapper()'. The sElementId '"
								+ sElementId + "' was not contained in list: " + givenSlots);
			}
			boolean noBase = true;
			for (SNode sCorpus : givenSlot) {
				MappingSubject mappingSubject = new MappingSubject();
				mappingSubject.setIdentifier(sCorpus.getIdentifier());
				mappingSubject.setMappingResult(DOCUMENT_STATUS.IN_PROGRESS);
				mapper.getMappingSubjects().add(mappingSubject);
				if (getBaseCorpusStructure().equals(((SCorpus) sCorpus).getGraph())) {
					noBase = false;
				}
			}
			if (noBase) {// no corpus in slot containing in base
							// corpus-structure was found
				MappingSubject mappingSubject = new MappingSubject();
				mappingSubject.setIdentifier(getBaseCorpusStructure().getNode(sElementId.getId()).getIdentifier());
				mappingSubject.setMappingResult(DOCUMENT_STATUS.IN_PROGRESS);
				mapper.getMappingSubjects().add(mappingSubject);
			}
		}
		mapper.setBaseCorpusStructure(getBaseCorpusStructure());
		return (mapper);
	}
}
