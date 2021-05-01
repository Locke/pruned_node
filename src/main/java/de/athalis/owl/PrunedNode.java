package de.athalis.owl;

import openllet.core.KnowledgeBase;
import openllet.core.OpenlletOptions;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import java.io.File;
import java.util.stream.Stream;

class PrunedNode {
    public static void main(String[] args) {
        OpenlletOptions.TRACK_BRANCH_EFFECTS = true;

        PrunedNode app = new PrunedNode();

        //File processFile = new File("./t1.owl");
        //File processFile = new File("./t2.owl");
        //File processFile = new File("./t3.owl");
        //File processFile = new File("./t4.owl");
        //File processFile = new File("./t5.owl");
        //File processFile = new File("./t6.owl");
        //File processFile = new File("./t8.owl");

        File processFile = new File("./npe.owl");

        try {
            app.printProcesses(processFile);
        }
        catch (final Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }



    private final OWLOntologyManager manager;

    private final OWLClass CLASS_PROCESSMODEL;
    private final OWLDataProperty DATA_PROPERTY_HASMODELCOMPONENTID;

    private PrunedNode() {
        manager = OWLManager.createConcurrentOWLOntologyManager();

        OWLDataFactory factory = manager.getOWLDataFactory();

        CLASS_PROCESSMODEL = factory.getOWLClass(IRI.create("http://www.i2pm.net/standard-pass-ont#PASSProcessModel"));
        DATA_PROPERTY_HASMODELCOMPONENTID = factory.getOWLDataProperty(IRI.create("http://www.i2pm.net/standard-pass-ont#hasModelComponentID"));
    }

    private void printProcesses(File file) throws OWLOntologyCreationException {
        OWLOntology ont = manager.loadOntologyFromOntologyDocument(file);

        OWLDataFactory factory = manager.getOWLDataFactory();

        ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
        OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ont, config);

        try {
            long startTime = System.nanoTime();

            if (!reasoner.isConsistent()) {
                throw new IllegalArgumentException("inconsistent process file!");
            }

            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            reasoner.instances(CLASS_PROCESSMODEL).forEach(p -> {
                // NOTE: any change seems to trigger the error, originally I had a SubClass axiom but DataProperty seems easier here for the example
                manager.addAxiom(ont, factory.getOWLDataPropertyAssertionAxiom(DATA_PROPERTY_HASMODELCOMPONENTID, p, "AddingEdgeTest"));
            });

            reasoner.flush();

            if (!reasoner.isConsistent()) {
                throw new InternalError("inconsistent after adding a ModelID! took: " + (System.nanoTime() - startTime)/1e6 + " ms");
            }

            final KnowledgeBase kb = reasoner.getKB();
            kb.realize();
            kb.classify();

            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            System.out.println("precomputeInferences succeeded :) took " + (System.nanoTime() - startTime)/1e6 + " ms");

            Stream<OWLNamedIndividual> processes = reasoner.instances(CLASS_PROCESSMODEL);

            processes.forEach(System.out::println);
        }
        finally {
            reasoner.dispose();
            manager.removeOntology(ont);
        }
    }
}
