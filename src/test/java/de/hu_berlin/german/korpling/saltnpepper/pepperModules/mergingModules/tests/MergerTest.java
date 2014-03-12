package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepper.testFramework.PepperManipulatorTest;
import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.Merger;
import de.hu_berlin.german.korpling.saltnpepper.salt.SaltFactory;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpus;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SCorpusGraph;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCommon.sCorpusStructure.SDocument;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltSample.SaltSample;

/**
 * 
 * @author Florian Zipser
 *
 */
public class MergerTest extends PepperManipulatorTest{

	private Merger fixture= null;

	public Merger getFixture() {
		return fixture;
	}

	public void setFixture(Merger fixture) {
		this.fixture = fixture;
	}
	
	@Before
	public void setUp(){
		setFixture(new Merger());
		getFixture().setSaltProject(SaltFactory.eINSTANCE.createSaltProject());
	}
	
	/**
	 * Tests the merging on level {@link MERGING_LEVEL#MERGE_DOCUMENTS}:
	 * <pre> 
	 *   c1    |    c1      |    c1      
	 *   |     |   /  \     |   /  \     
	 *   c2    |  c2   c3   |  c2   c3   
	 *   |     | /  \   |   | /  \   |   
	 *   d1    |d1  d2  d3  |d1  d2  d3  
	 * </pre>
	 * result (autodetect):
	 * <pre>
	 *     c1
	 *    /  \     
	 *   c2   c3   
	 *  /  \   |   
	 * d1  d2  d3
	 * </pre>
	 */
	@Test
	public void test_CorpusStructure_1(){
		SCorpusGraph graph1= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1= SaltFactory.eINSTANCE.createSCorpus();
		c1.setSName("c1");
		graph1.addSNode(c1);
		SCorpus c2_1= SaltFactory.eINSTANCE.createSCorpus();
		c2_1.setSName("c2");
		graph1.addSSubCorpus(c1, c2_1);
		SDocument d1= SaltFactory.eINSTANCE.createSDocument();
		d1.setSName("d1");
		graph1.addSDocument(c2_1, d1);
		
		SCorpusGraph graph2= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_2= SaltFactory.eINSTANCE.createSCorpus();
		c1_2.setSName("c1");
		graph2.addSNode(c1_2);
		SCorpus c2_2= SaltFactory.eINSTANCE.createSCorpus();
		c2_2.setSName("c2");
		graph2.addSSubCorpus(c1_2, c2_2);
		SDocument d1_2= SaltFactory.eINSTANCE.createSDocument();
		d1_2.setSName("d1");
		graph2.addSDocument(c2_2, d1_2);
		SDocument d2_2= SaltFactory.eINSTANCE.createSDocument();
		d2_2.setSName("d2");
		graph2.addSDocument(c2_2, d2_2);
		SCorpus c3_2= SaltFactory.eINSTANCE.createSCorpus();
		c3_2.setSName("c3");
		graph2.addSSubCorpus(c1_2, c3_2);
		SDocument d3_2= SaltFactory.eINSTANCE.createSDocument();
		d3_2.setSName("d3");
		graph2.addSDocument(c3_2, d3_2);

		SCorpusGraph graph3= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_3= SaltFactory.eINSTANCE.createSCorpus();
		c1_3.setSName("c1");
		graph3.addSNode(c1_3);
		SCorpus c2_3= SaltFactory.eINSTANCE.createSCorpus();
		c2_3.setSName("c2");
		graph3.addSSubCorpus(c1_3, c2_3);
		SDocument d1_3= SaltFactory.eINSTANCE.createSDocument();
		d1_3.setSName("d1");
		graph3.addSDocument(c2_3, d1_3);
		SDocument d2_3= SaltFactory.eINSTANCE.createSDocument();
		d2_3.setSName("d2");
		graph3.addSDocument(c2_3, d2_3);
		SCorpus c3_3= SaltFactory.eINSTANCE.createSCorpus();
		c3_3.setSName("c3");
		graph3.addSSubCorpus(c1_3, c3_3);
		SDocument d3_3= SaltFactory.eINSTANCE.createSDocument();
		d3_3.setSName("d3");
		graph3.addSDocument(c3_3, d3_3);
		
		this.getFixture().getSaltProject().getSCorpusGraphs().add(graph1);
		this.getFixture().getSaltProject().getSCorpusGraphs().add(graph2);
		this.getFixture().getSaltProject().getSCorpusGraphs().add(graph3);
		
		this.start();
		
		
		/**
		 *     c1
		 *    /  \     
		 *   c2   c3   
		 *  /  \   |   
		 * d1  d2  d3
		 */
		assertEquals(4, graph1.getSCorpora().size());
		assertEquals(4, graph1.getSDocuments().size());
		assertEquals(3, graph1.getSCorpusRelations().size());
		assertEquals(4, graph1.getSCorpusDocumentRelations().size());
		
		assertEquals(1, graph1.getSRoots().size());
		assertEquals(3, graph1.getOutEdges(c1.getSId()).size());
	}
	
	/**
	 * Tests the merging on level {@link MERGING_LEVEL#MERGE_DOCUMENTS}:
	 * <pre> 
	 *   |    c1      |    c1       |  c1          
	 *   |   /  \     |   /  \      |   |          
	 *   |  c2   c3   |  c2   c3    |  c2 
	 *   | /  \   |   | /  \   |    |   |
	 *   |d1  d2  d3  |d1  d2  d3   |  d1
	 * </pre>
	 * result (autodetect):
	 * <pre>
	 *     c1
	 *    /  \     
	 *   c2   c3   
	 *  /  \   |   
	 * d1  d2  d3
	 * </pre>
	 * @throws InterruptedException 
	 */
	@Test
	public void test_CorpusStructure_2() throws InterruptedException{
		//graph 1
		SCorpusGraph graph1= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_1= SaltFactory.eINSTANCE.createSCorpus();
		c1_1.setSName("c1");
		c1_1.createSMetaAnnotation(null, "anno1", "someValue");
		graph1.addSNode(c1_1);
		SCorpus c2_1= SaltFactory.eINSTANCE.createSCorpus();
		c2_1.setSName("c2");
		c2_1.createSMetaAnnotation(null, "anno1", "someValue");
		graph1.addSSubCorpus(c1_1, c2_1);
		SDocument d1_1= SaltFactory.eINSTANCE.createSDocument();
		d1_1.setSName("d1");
		d1_1.createSMetaAnnotation(null, "anno1", "someValue");
		graph1.addSDocument(c2_1, d1_1);
		SDocument d2_1= SaltFactory.eINSTANCE.createSDocument();
		d2_1.setSName("d2");
		d2_1.createSMetaAnnotation(null, "anno1", "someValue");
		graph1.addSDocument(c2_1, d2_1);
		SCorpus c3_1= SaltFactory.eINSTANCE.createSCorpus();
		c3_1.setSName("c3");
		c3_1.createSMetaAnnotation(null, "anno1", "someValue");
		graph1.addSSubCorpus(c1_1, c3_1);
		SDocument d3_1= SaltFactory.eINSTANCE.createSDocument();
		d3_1.setSName("d3");
		d3_1.createSMetaAnnotation(null, "anno1", "someValue");
		graph1.addSDocument(c3_1, d3_1);
		this.getFixture().getSaltProject().getSCorpusGraphs().add(graph1);
		
		//graph2
		SCorpusGraph graph2= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_2= SaltFactory.eINSTANCE.createSCorpus();
		c1_2.setSName("c1");
		c1_2.createSMetaAnnotation(null, "anno2", "someValue");
		graph2.addSNode(c1_2);
		SCorpus c2_2= SaltFactory.eINSTANCE.createSCorpus();
		c2_2.setSName("c2");
		c2_2.createSMetaAnnotation(null, "anno2", "someValue");
		graph2.addSSubCorpus(c1_2, c2_2);
		SDocument d1_2= SaltFactory.eINSTANCE.createSDocument();
		d1_2.setSName("d1");
		d1_2.createSMetaAnnotation(null, "anno2", "someValue");
		graph2.addSDocument(c2_2, d1_2);
		SDocument d2_2= SaltFactory.eINSTANCE.createSDocument();
		d2_2.setSName("d2");
		d2_2.createSMetaAnnotation(null, "anno2", "someValue");
		graph2.addSDocument(c2_2, d2_2);
		SCorpus c3_2= SaltFactory.eINSTANCE.createSCorpus();
		c3_2.setSName("c3");
		c3_2.createSMetaAnnotation(null, "anno2", "someValue");
		graph2.addSSubCorpus(c1_2, c3_2);
		SDocument d3_2= SaltFactory.eINSTANCE.createSDocument();
		d3_2.setSName("d3");
		d3_2.createSMetaAnnotation(null, "anno2", "someValue");
		graph2.addSDocument(c3_2, d3_2);
		this.getFixture().getSaltProject().getSCorpusGraphs().add(graph2);
		
		//graph3
		SCorpusGraph graph3= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_3= SaltFactory.eINSTANCE.createSCorpus();
		c1_3.setSName("c1");
		c1_3.createSMetaAnnotation(null, "anno3", "someValue");
		graph3.addSNode(c1_3);
		SCorpus c2_3= SaltFactory.eINSTANCE.createSCorpus();
		c2_3.setSName("c2");
		c2_3.createSMetaAnnotation(null, "anno3", "someValue");
		graph3.addSSubCorpus(c1_3, c2_3);
		SDocument d1_3= SaltFactory.eINSTANCE.createSDocument();
		d1_3.setSName("d1");
		d1_3.createSMetaAnnotation(null, "anno3", "someValue");
		graph3.addSDocument(c2_3, d1_3);
		this.getFixture().getSaltProject().getSCorpusGraphs().add(graph3);
		
		this.start();
		
		/**
		  *     c1
		  *    /  \     
		  *   c2   c3   
		  *  /  \   |   
		  * d1  d2  d3
		 */
		assertEquals(3, graph1.getSCorpora().size());
		assertEquals(3, graph1.getSDocuments().size());
		assertEquals(2, graph1.getSCorpusRelations().size());
		assertEquals(3, graph1.getSCorpusDocumentRelations().size());
		
		assertEquals("all meta-annotations: "+c1_1.getSMetaAnnotations(),3, c1_1.getSMetaAnnotations().size());
		assertEquals("all meta-annotations: "+c2_1.getSMetaAnnotations(),3, c2_1.getSMetaAnnotations().size());
		assertEquals("all meta-annotations: "+c3_1.getSMetaAnnotations(), 2, c3_1.getSMetaAnnotations().size());
		assertEquals("all meta-annotations: "+d1_1.getSMetaAnnotations(), 3, d1_1.getSMetaAnnotations().size());
		assertEquals("all meta-annotations: "+d2_1.getSMetaAnnotations(), 2, d2_1.getSMetaAnnotations().size());
		assertEquals("all meta-annotations: "+d3_1.getSMetaAnnotations(), 2, d3_1.getSMetaAnnotations().size());
	}
	
	/**
	 * Tests the merging of three corpus graphs having some document content
	 * 
	 * 	 c1     |    c1    |     c1
	 *  /  \    |     |    |    /  \
	 * d1  d2   |     d1   |   d1  d2
	 */
	@Test
	public void testMerge3CorpusGraphsWithDocumentStructure(){
		
		//graph 1
		SCorpusGraph g1= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_1= SaltFactory.eINSTANCE.createSCorpus();
		c1_1.setSName("c1");
		g1.addSNode(c1_1);
		SDocument d1_1= g1.createSDocument(c1_1, "d1");
		d1_1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(d1_1);
		SaltSample.createTokens(d1_1);
		SaltSample.createMorphologyAnnotations(d1_1);
		SDocument d2_1= g1.createSDocument(c1_1, "d2");
		d2_1.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(d2_1);
		getFixture().getSaltProject().getSCorpusGraphs().add(g1);
		
		//graph 2
		SCorpusGraph g2= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_2= SaltFactory.eINSTANCE.createSCorpus();
		c1_2.setSName("c1");
		g2.addSNode(c1_2);
		SDocument d1_2= g2.createSDocument(c1_2, "d1");
		d1_2.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(d1_2);
		SaltSample.createTokens(d1_2);
		SaltSample.createSyntaxStructure(d1_2);
		SaltSample.createSyntaxAnnotations(d1_2);
		getFixture().getSaltProject().getSCorpusGraphs().add(g2);

		//graph 3
		SCorpusGraph g3= SaltFactory.eINSTANCE.createSCorpusGraph();
		SCorpus c1_3= SaltFactory.eINSTANCE.createSCorpus();
		c1_3.setSName("c1");
		g3.addSNode(c1_3);
		SDocument d1_3= g3.createSDocument(c1_3, "d1");
		d1_3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(d1_3);
		SaltSample.createTokens(d1_3);
		SaltSample.createInformationStructureSpan(d1_3);
		SaltSample.createInformationStructureAnnotations(d1_3);
		SDocument d2_3= g3.createSDocument(c1_3, "d2");
		d2_3.setSDocumentGraph(SaltFactory.eINSTANCE.createSDocumentGraph());
		SaltSample.createPrimaryData(d2_3);
		SaltSample.createTokens(d2_3);
		getFixture().getSaltProject().getSCorpusGraphs().add(g3);

		this.start();
		
		assertEquals(1, getFixture().getSaltProject().getSCorpusGraphs().get(0).getSCorpora().size());
		assertEquals(2, getFixture().getSaltProject().getSCorpusGraphs().get(0).getSDocuments().size());
		
		assertEquals(2, getFixture().getSaltProject().getSCorpusGraphs().get(0).getSDocuments().get(0).getSDocumentGraph().getSNodes().size());
		assertEquals(2, getFixture().getSaltProject().getSCorpusGraphs().get(0).getSDocuments().get(0).getSDocumentGraph().getSRelations().size());
	}
}
