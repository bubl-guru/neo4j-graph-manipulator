package org.triple_brain.module.neo4j_graph_manipulator.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Relationship;
import org.triple_brain.module.model.User;
import org.triple_brain.module.model.graph.Edge;
import org.triple_brain.module.model.graph.Vertex;

import javax.inject.Inject;
import java.net.URI;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4JEdge extends Edge {

    private Relationship relationship;
    private User owner;
    protected Neo4JGraphElement graphElement;
    @Inject
    protected Neo4JVertexFactory vertexFactory;

    @Inject
    protected static Neo4JEdgeFactory edgeFactory;

    @AssistedInject
    protected Neo4JEdge(
            @Assisted Relationship relationship,
            @Assisted User owner
    ) {
        this.relationship = relationship;
        this.owner = owner;
        graphElement = Neo4JGraphElement.withPropertyContainer(relationship);
    }

    @AssistedInject
    protected Neo4JEdge(
            @Assisted Relationship relationship,
            @Assisted User owner,
            @Assisted URI uri
    ) {
        this(relationship, owner);
        graphElement = Neo4JGraphElement.initiateProperties(
                relationship,
                uri
        );
    }

    @Override
    public Vertex sourceVertex() {
        return vertexFactory.loadUsingNodeOfOwner(
                relationship.getStartNode(),
                owner
        );
    }

    @Override
    public Vertex destinationVertex() {
        return vertexFactory.loadUsingNodeOfOwner(
                relationship.getEndNode(),
                owner
        );
    }

    @Override
    public Vertex otherVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ?
                destinationVertex() :
                sourceVertex();
    }

    @Override
    public boolean hasVertex(Vertex vertex) {
        return sourceVertex().equals(vertex) ||
                destinationVertex().equals(vertex);
    }

    @Override
    public void remove() {
        relationship.removeProperty(Neo4JUserGraph.URI_PROPERTY_NAME);
        relationship.delete();
    }

    @Override
    public String id() {
        return graphElement.id();
    }

    @Override
    public String label() {
        return graphElement.label();
    }

    @Override
    public void label(String label) {
        graphElement.label(label);
    }

    @Override
    public boolean hasLabel() {
        return graphElement.hasLabel();
    }
}
