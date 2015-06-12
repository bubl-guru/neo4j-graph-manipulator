/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.test;

import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.GraphFactory;
import guru.bubl.module.model.graph.SubGraphPojo;
import guru.bubl.module.model.graph.UserGraph;
import guru.bubl.module.model.graph.vertex.Vertex;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphOperator;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jModule;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jWholeGraph;
import org.neo4j.graphdb.*;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import guru.bubl.module.model.test.GraphComponentTest;
import guru.bubl.module.model.test.SubGraphOperator;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgeFactory;
import guru.bubl.module.model.graph.edge.EdgeOperator;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.test.scenarios.TestScenarios;
import guru.bubl.module.model.test.scenarios.VerticesCalledABAndC;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractorFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jUserGraphFactory;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.Neo4jVertexFactory;

import javax.inject.Inject;
import java.util.*;

public class Neo4JGraphComponentTest implements GraphComponentTest {

    @Inject
    protected TestScenarios testScenarios;

    @Inject
    protected Neo4jSubGraphExtractorFactory neo4jSubGraphExtractorFactory;

    @Inject
    Neo4jWholeGraph wholeGraph;

    @Inject
    GraphDatabaseService graphDatabaseService;

    @Inject
    protected EdgeFactory edgeFactory;

    @Inject
    protected RestAPI graphDb;

    @Inject
    protected QueryEngine queryEngine;

    @Inject
    protected Neo4jVertexFactory vertexFactory;

    @Inject
    protected Neo4jUserGraphFactory neo4jUserGraphFactory;

    @Inject
    protected GraphFactory graphFactory;

    protected VertexOperator vertexA;
    protected VertexOperator vertexB;
    protected VertexOperator vertexC;

    protected static User user;

    protected static User anotherUser;
    protected static UserGraph anotherUserGraph;
    protected static VertexOperator vertexOfAnotherUser;

    protected Transaction transaction;

    protected UserGraph userGraph;

    @Override
    public void beforeClass() {

    }

    @Override
    public void before() {
        startTransaction();
        user = User.withEmail(
                "roger.lamothe@example.org"
        ).setUsername("roger_lamothe");
        anotherUser = User.withEmail(
                "colette.armande@example.org"
        ).setUsername("colette_armande");

        userGraph = neo4jUserGraphFactory.withUser(user);
        VerticesCalledABAndC verticesCalledABAndC = testScenarios.makeGraphHave3VerticesABCWhereAIsDefaultCenterVertexAndAPointsToBAndBPointsToC(
                userGraph
        );
        vertexA = verticesCalledABAndC.vertexA();
        vertexB = verticesCalledABAndC.vertexB();
        vertexC = verticesCalledABAndC.vertexC();
        anotherUserGraph = neo4jUserGraphFactory.withUser(anotherUser);
        graphFactory.createForUser(anotherUserGraph.user());
        vertexOfAnotherUser = anotherUserGraph.defaultVertex();
        vertexOfAnotherUser.label("vertex of another user");
    }

    @Override
    public void after() {
        transaction.failure();
        transaction.close();
    }

    @Override
    public void afterClass() {
        graphDatabaseService.shutdown();
        Neo4jModule.clearDb();
    }

    @Override
    public int numberOfEdgesAndVertices() {
        return numberOfVertices() +
                numberOfEdges();
    }

    @Override
    public SubGraphPojo wholeGraphAroundDefaultCenterVertex() {
        Integer depthThatShouldCoverWholeGraph = 1000;
        return neo4jSubGraphExtractorFactory.withCenterVertexAndDepth(
                vertexA.uri(),
                depthThatShouldCoverWholeGraph
        ).load();
    }


    @Override
    public SubGraphOperator wholeGraph() {
        return SubGraphOperator.withVerticesAndEdges(
                allVertices(),
                allEdges()
        );
    }

    @Override
    public void removeWholeGraph() {
        queryEngine.query(
                "START n=node:node_auto_index(" +
                        "'type:" + GraphElementType.vertex + " " +
                        "OR type:" + GraphElementType.edge + " " +
                        "OR type:" + GraphElementType.schema + " " +
                        "OR type:" + GraphElementType.property + " " +
                        "'), r=relationship(*) DELETE n, r;",
                Collections.EMPTY_MAP
        );
    }

    @Override
    public boolean graphContainsLabel(String label) {
        return anyNodeContainsLabel(label) ||
                anyRelationshipContainsLabel(label);
    }

    protected boolean anyNodeContainsLabel(String label) {
        for (Node node : allNodes()) {
            if (hasLabel(node, label)) {
                return true;
            }
        }
        return false;
    }

    protected boolean anyRelationshipContainsLabel(String label) {
        for (Relationship relationship : allRelationships()) {
            if (hasLabel(relationship, label)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasLabel(PropertyContainer propertyContainer, String label) {
        try {
            String labelProperty = Neo4jFriendlyResource.props.label.toString();
            return propertyContainer.hasProperty(
                    labelProperty
            ) &&
                    propertyContainer.getProperty(labelProperty).equals(label);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public void user(User user) {
        this.user = user;
    }

    @Override
    public UserGraph userGraph() {
        return userGraph;
    }

    @Override
    public VertexOperator vertexA() {
        return vertexFactory.withUri(
                vertexA.uri()
        );
    }

    @Override
    public void setDefaultVertexAkaVertexA(VertexOperator vertexA) {
        this.vertexA = vertexA;
    }

    @Override
    public VertexOperator vertexB() {
        return vertexFactory.withUri(
                vertexB.uri()
        );
    }

    @Override
    public VertexOperator vertexC() {
        return vertexFactory.withUri(
                vertexC.uri()
        );
    }

    @Override
    public VertexOperator vertexOfAnotherUser() {
        return vertexOfAnotherUser;
    }

    @Override
    public VertexInSubGraphPojo vertexInWholeConnectedGraph(Vertex vertex) {
        return (VertexInSubGraphPojo) wholeGraphAroundDefaultCenterVertex().vertexWithIdentifier(
                vertex.uri()
        );
    }

    @Override
    public EdgePojo edgeInWholeGraph(Edge edge) {
        return (EdgePojo) wholeGraphAroundDefaultCenterVertex().edgeWithIdentifier(
                edge.uri()
        );
    }

    protected Set<VertexInSubGraphOperator> allVertices() {
        Set<VertexInSubGraphOperator> vertices = new HashSet<VertexInSubGraphOperator>();
        Iterator<VertexInSubGraphOperator> iterator= wholeGraph.getAllVertices();
        while(iterator.hasNext()) {
            vertices.add(
                    iterator.next()
            );
        }
        return vertices;
    }

    protected Set<Node> allNodes() {
        Set<Node> nodes = new HashSet<Node>();
        QueryResult<Map<String, Object>> result = queryEngine.query(
                "START n = node(*) " +
                        " RETURN n",
                Collections.EMPTY_MAP
        );
        Iterator<Map<String, Object>> iterator=result.iterator();
        while(iterator.hasNext()) {
            Map<String,Object> row= iterator.next();
            nodes.add(
                    (Node) row.get("n")
            );
        }
        return nodes;
    }

    protected Set<Relationship> allRelationships() {
        Set<Relationship> relationships = new HashSet<Relationship>();
        QueryResult<Map<String,Object>> result = queryEngine.query(
                "START r = relationship(*) " +
                        " RETURN r",
                Collections.EMPTY_MAP
        );
        Iterator<Map<String, Object>> iterator=result.iterator();
        while (iterator.hasNext()) {
            Map<String,Object> row= iterator.next();
            relationships.add(
                    (Relationship) row.get("r")
            );
        }
        return relationships;
    }

    protected int numberOfVertices() {
        return allVertices().size();
    }

    protected Set<EdgeOperator> allEdges() {
        Set<EdgeOperator> edges = new HashSet<EdgeOperator>();
        Iterator<EdgeOperator> iterator = wholeGraph.getAllEdges();
        while(iterator.hasNext()){
            edges.add(
                    iterator.next()
            );
        }
        return edges;
    }

    protected int numberOfEdges() {
        return allEdges().size();
    }

    private void startTransaction() {
        transaction = graphDb.beginTx();
    }
}