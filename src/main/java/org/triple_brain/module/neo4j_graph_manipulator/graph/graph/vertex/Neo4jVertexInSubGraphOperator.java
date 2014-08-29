package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.vertex;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.neo4j.graphdb.Node;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.batch.BatchCallback;
import org.neo4j.rest.graphdb.query.QueryEngine;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.triple_brain.module.model.Image;
import org.triple_brain.module.model.UserUris;
import org.triple_brain.module.model.graph.Identification;
import org.triple_brain.module.model.graph.IdentificationPojo;
import org.triple_brain.module.model.graph.edge.Edge;
import org.triple_brain.module.model.graph.edge.EdgeOperator;
import org.triple_brain.module.model.graph.edge.EdgePojo;
import org.triple_brain.module.model.graph.vertex.*;
import org.triple_brain.module.model.json.SuggestionJson;
import org.triple_brain.module.model.suggestion.SuggestionPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.*;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeFactory;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.edge.Neo4jEdgeOperator;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.IncludedGraphElementFromExtractorQueryRow;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph.Neo4jSubGraphExtractor;

import java.net.URI;
import java.util.*;

import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource.addUpdatedLastModificationDate;
import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.map;
import static org.triple_brain.module.neo4j_graph_manipulator.graph.Neo4jRestApiUtils.wrap;

/*
* Copyright Mozilla Public License 1.1
*/
public class Neo4jVertexInSubGraphOperator implements VertexInSubGraphOperator, Neo4jOperator {

    public enum props {
        number_of_connected_edges_property_name,
        is_public,
        suggestions
    }

    private Integer depthInSubGraph = -1;
    protected Neo4jGraphElementOperator graphElementOperator;
    protected Neo4jVertexFactory vertexFactory;

    protected Neo4jEdgeFactory edgeFactory;

    protected Neo4jUtils utils;


    protected Neo4jGraphElementFactory neo4jGraphElementFactory;
    protected Node node;
    protected RestAPI restApi;
    protected QueryEngine<Map<String,Object>> queryEngine;

    @Inject
    protected Neo4jFriendlyResourceFactory friendlyResourceFactory;

    @AssistedInject
    protected Neo4jVertexInSubGraphOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jUtils utils,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted Node node
    ) {
        this(
                vertexFactory,
                edgeFactory,
                utils,
                neo4jGraphElementFactory,
                queryEngine,
                restApi,
                neo4jGraphElementFactory.withNode(
                        node
                ).uri()
        );
        this.node = node;
    }

    @AssistedInject
    protected Neo4jVertexInSubGraphOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jUtils utils,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted URI uri
    ) {
        this.vertexFactory = vertexFactory;
        this.edgeFactory = edgeFactory;
        this.utils = utils;
        this.neo4jGraphElementFactory = neo4jGraphElementFactory;
        this.queryEngine = queryEngine;
        this.restApi = restApi;
        this.graphElementOperator = neo4jGraphElementFactory.withUri(
                uri
        );
    }

    @AssistedInject
    protected Neo4jVertexInSubGraphOperator(
            Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jUtils utils,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            @Assisted String ownerUserName
    ) {
        this(
                vertexFactory,
                edgeFactory,
                utils,
                neo4jGraphElementFactory,
                queryEngine,
                restApi,
                new UserUris(ownerUserName).generateVertexUri()
        );
        create();
    }

    @AssistedInject
    protected Neo4jVertexInSubGraphOperator(
            final Neo4jVertexFactory vertexFactory,
            Neo4jEdgeFactory edgeFactory,
            Neo4jUtils utils,
            Neo4jGraphElementFactory neo4jGraphElementFactory,
            QueryEngine queryEngine,
            RestAPI restApi,
            final @Assisted Set<Vertex> includedVertices,
            final @Assisted Set<Edge> includedEdges
    ) throws IllegalArgumentException {
        this(
                vertexFactory,
                edgeFactory,
                utils,
                neo4jGraphElementFactory,
                queryEngine,
                restApi,
                new UserUris(
                        includedVertices.iterator().next().ownerUsername()
                ).generateVertexUri()
        );
        if (includedVertices.size() <= 1) {
            throw new IllegalArgumentException(
                    "A minimum number of 2 vertices is required to create a vertex from included vertices"
            );
        }
        create();
        setIncludedVertices(
                includedVertices
        );
        setIncludedEdges(
                includedEdges
        );
    }

    @Override
    public boolean hasEdge(Edge edge) {
        Neo4jFriendlyResource edgeFriendlyResource = friendlyResourceFactory.withUri(
                edge.uri()
        );
        QueryResult<Map<String,Object>> result = queryEngine.query(
                queryPrefix() + ", " +
                        edgeFriendlyResource.addToSelectUsingVariableName("edge") +
                        "MATCH " +
                        "n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-edge " +
                        "RETURN edge",
                map()
        );
        return result.iterator().hasNext();
    }

    @Override
    public EdgeOperator edgeThatLinksToDestinationVertex(Vertex destinationVertex) {
        Neo4jFriendlyResource destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() + ", " +
                        destinationVertexOperator.addToSelectUsingVariableName("d") +
                        "MATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-r, " +
                        "r-[:SOURCE_VERTEX|DESTINATION_VERTEX]->d " +
                        "RETURN r.uri as uri",
                map()
        );
        Iterator<Map<String, Object>> it = result.iterator();
        if (!it.hasNext()) {
            throw new RuntimeException(
                    "Edge between vertex with " + uri() +
                            " and vertex with uri " + destinationVertex.uri() +
                            " was not found"
            );
        }
        return edgeFactory.withUri(URI.create(
                it.next().get("uri").toString()
        ));
    }

    @Override
    public Boolean hasDestinationVertex(Vertex destinationVertex) {
        Neo4jFriendlyResource destinationVertexOperator = friendlyResourceFactory.withUri(
                destinationVertex.uri()
        );
        QueryResult<Map<String,Object>> result = queryEngine.query(
                queryPrefix() + ", " +
                        destinationVertexOperator.addToSelectUsingVariableName("d") +
                        "MATCH n<-[:SOURCE_VERTEX]-r, " +
                        "r-[:DESTINATION_VERTEX]->d " +
                        "RETURN n",
                map()
        );
        return result.iterator().hasNext();
    }

    @Override
    public EdgeOperator addVertexAndRelation() {
        final Neo4jVertexInSubGraphOperator self = this;
        return restApi.executeBatch(new BatchCallback<EdgeOperator>() {
            @Override
            public EdgeOperator recordBatch(RestAPI batchRestApi) {
                URI newVertexUri = new UserUris(
                        ownerUsername()
                ).generateVertexUri();
                Neo4jVertexInSubGraphOperator newVertexOperator = vertexFactory.withUri(
                        newVertexUri
                );
                self.incrementNumberOfConnectedEdges();
                newVertexOperator.createUsingInitialValues(
                        map(
                                props.number_of_connected_edges_property_name.name(),
                                1
                        )
                );
                Neo4jEdgeOperator edgeOperator = edgeFactory.withSourceAndDestinationVertex(
                        self,
                        newVertexOperator
                );
                edgeOperator.create();
                return edgeOperator;
            }
        });
    }

    @Override
    public EdgeOperator addRelationToVertex(final Vertex destinationVertex) {
        final VertexOperator thisVertex = this;
        return restApi.executeBatch(new BatchCallback<EdgeOperator>() {
            @Override
            public EdgeOperator recordBatch(RestAPI batchRestApi) {
                incrementNumberOfConnectedEdges();
                (
                        (Neo4jVertexInSubGraphOperator) destinationVertex
                ).incrementNumberOfConnectedEdges();
                EdgeOperator edge = edgeFactory.withSourceAndDestinationVertex(
                        thisVertex,
                        destinationVertex
                );
                edge.create();
                return edge;
            }
        });
    }

    @Override
    public void remove() {
        restApi.executeBatch(new BatchCallback<Object>() {
            @Override
            public Object recordBatch(RestAPI restAPI) {
                queryEngine.query(
                        queryPrefix() +
                                "MATCH " +
                                "n<-[:SOURCE_VERTEX]-(edge), " +
                                "edge-[:DESTINATION_VERTEX]->(vertex) " +
                                "SET " +
                                "vertex.number_of_connected_edges_property_name = " +
                                "vertex.number_of_connected_edges_property_name - 1",
                        map()
                );
                queryEngine.query(
                        queryPrefix() +
                                "MATCH " +
                                "n<-[:DESTINATION_VERTEX]-(edge), " +
                                "edge-[:SOURCE_VERTEX]->(vertex) " +
                                "SET " +
                                "vertex.number_of_connected_edges_property_name = " +
                                "vertex.number_of_connected_edges_property_name - 1",
                        map()
                );
                queryEngine.query(
                        queryPrefix() +
                                "MATCH " +
                                "n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge), " +
                                "edge-[edge_relation]-() " +
                                "MATCH " +
                                "n-[vertex_relation]-() " +
                                "DELETE " +
                                "vertex_relation, n, " +
                                "edge_relation, edge",
                        map()
                );
                return null;
            }
        });
    }

    @Override
    public String ownerUsername() {
        return graphElementOperator.ownerUsername();
    }

    @Override
    public Set<EdgeOperator> connectedEdges() {
        Set<EdgeOperator> edges = new HashSet<>();
        QueryResult<Map<String,Object>> result = queryEngine.query(
                queryPrefix() +
                        "MATCH n<-[:SOURCE_VERTEX|DESTINATION_VERTEX]-(edge) " +
                        "RETURN edge.uri as uri",
                map()
        );
        for (Map<String, Object> uriMap : result) {
            edges.add(edgeFactory.withUri(
                    URI.create(
                            uriMap.get(
                                    "uri"
                            ).toString()
                    )
            ));
        }
        return edges;
    }

    @Override
    public Integer getNumberOfConnectedEdges() {
        QueryResult<Map<String,Object>> result = queryEngine.query(
                queryPrefix() + "return n." + props.number_of_connected_edges_property_name + " as result",
                map()
        );
        return Integer.valueOf(
                result.iterator().next().get("result").toString()
        );
    }

    @Override
    public void setNumberOfConnectedEdges(Integer numberOfConnectedEdges) {
        getNode().setProperty(
                props.number_of_connected_edges_property_name.name(),
                numberOfConnectedEdges
        );
    }

    protected void incrementNumberOfConnectedEdges() {
        queryEngine.query(
                queryPrefix() +
                        "SET n.number_of_connected_edges_property_name = " +
                        "n.number_of_connected_edges_property_name + 1",
                map()
        );
    }

    @Override
    public void setSuggestions(Set<SuggestionPojo> suggestions){
        queryEngine.query(
                queryPrefix() +
                        "SET n." + props.suggestions + "= { " + props.suggestions + "} ",
                map(
                        props.suggestions.name(), SuggestionJson.multipleToJson(suggestions).toString()
                )
        );
    }

    @Override
    public void addSuggestions(final Set<SuggestionPojo> suggestions) {
        Set<SuggestionPojo> current = getSuggestions();
        current.addAll(suggestions);
        setSuggestions(current);
    }

    @Override
    public Set<SuggestionPojo> getSuggestions() {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() +
                        "return n.`" + props.suggestions + "` as suggestions",
                map()
        );
        Object suggestionsValue = result.iterator().next().get("suggestions");
        if(suggestionsValue == null){
            return new HashSet<>();
        }
        return SuggestionJson.fromJsonArray(
                suggestionsValue.toString()
        );
    }

    @Override
    public IdentificationPojo addType(Identification type) {
        return graphElementOperator.addType(type);
    }

    @Override
    public void removeIdentification(final Identification friendlyResource) {
        QueryResult<Map<String,Object>> cypherResult = restApi.executeBatch(new BatchCallback<QueryResult<Map<String,Object>>>() {
            @Override
            public QueryResult<Map<String, Object>> recordBatch(RestAPI restAPI) {
                URI friendlyResourceUri = friendlyResource.uri();
                Neo4jFriendlyResource friendlyResourceOperator = friendlyResourceFactory.withUri(
                        friendlyResourceUri
                );
                String friendlyResourceSelect = friendlyResourceOperator.addToSelectUsingVariableName(
                        "f"
                );
                return queryEngine.query(queryPrefix() + ", " + friendlyResourceSelect + " MATCH (n)-[r]->(f) " +
                        "DELETE r " +
                        "SET " + Neo4jFriendlyResource.LAST_MODIFICATION_QUERY_PART,
                        addUpdatedLastModificationDate(map())
                );
            }
        });
        Iterator<Map<String, Object>> it = cypherResult.iterator();
        if (!it.hasNext()) {
            return;
        }
        Map<String,Object> results = it.next();
        Integer numberOriginsBeforeDeletion = new Integer(
                results.get("number_of_origins").toString()
        );
        if (1 == numberOriginsBeforeDeletion) {
            URI suggestionUri = URI.create(
                    results.get("uri").toString()
            );
            friendlyResourceFactory.withUri(
                    suggestionUri
            ).remove();
        }
    }

    @Override
    public Map<URI, Identification> getAdditionalTypes() {
        return graphElementOperator.getAdditionalTypes();
    }

    @Override
    public Map<URI, Identification> getIdentifications() {
        return graphElementOperator.getIdentifications();
    }

    @Override
    public IdentificationPojo addSameAs(Identification friendlyResourceImpl) {
        return graphElementOperator.addSameAs(friendlyResourceImpl);
    }

    @Override
    public Map<URI, Identification> getSameAs() {
        return graphElementOperator.getSameAs();
    }

    @Override
    public Map<URI,Identification> getGenericIdentifications() {
        return graphElementOperator.getGenericIdentifications();
    }

    @Override
    public Boolean isPublic() {
        return (Boolean) getNode().getProperty(
                props.is_public.name()
        );
    }

    @Override
    public void makePublic() {
        getNode().setProperty(
                props.is_public.name(),
                true
        );
        graphElementOperator.updateLastModificationDate();
    }

    @Override
    public void makePrivate() {
        getNode().setProperty(
                props.is_public.name(),
                false
        );
        graphElementOperator.updateLastModificationDate();
    }

    @Override
    public Map<URI, Vertex> getIncludedVertices() {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() +
                        "MATCH " +
                        "n-[:" + Relationships.HAS_INCLUDED_VERTEX + "]->included_vertex " +
                        "RETURN " +
                        Neo4jSubGraphExtractor.includedElementQueryPart("included_vertex")+
                        "'dummy_return'",
                map()
        );
        Map<URI, Vertex> includedVertices = new HashMap<>();
        for (Map<String, Object> row : result) {
            IncludedGraphElementFromExtractorQueryRow extractor = new IncludedGraphElementFromExtractorQueryRow(
                    row,
                    "included_vertex"
            );
            URI uri = extractor.getUri();
            includedVertices.put(
                    uri ,
                    new VertexInSubGraphPojo(
                            uri,
                            extractor.getLabel()
                    )
            );
        }
        return includedVertices;
    }

    @Override
    public Map<URI, Edge> getIncludedEdges() {
        QueryResult<Map<String, Object>> result = queryEngine.query(
                queryPrefix() +
                        "MATCH " +
                        "n-[:" + Relationships.HAS_INCLUDED_EDGE + "]->included_edge " +
                        "RETURN " +
                        Neo4jSubGraphExtractor.includedElementQueryPart("included_edge") +
                        "'dummy_return'",
                map()
        );
        Map<URI, Edge> includedEdges = new HashMap<>();
        for (Map<String, Object> row : result) {
            IncludedGraphElementFromExtractorQueryRow extractor = new IncludedGraphElementFromExtractorQueryRow(
                    row,
                    "included_edge"
            );
            URI uri = extractor.getUri();
            includedEdges.put(
                    uri ,
                    new EdgePojo(
                            uri,
                            extractor.getLabel()
                    )
            );
        }
        return includedEdges;
    }

    public void setIncludedVertices(Set<Vertex> includedVertices) {
        for (Vertex vertex : includedVertices) {
            getNode().createRelationshipTo(
                    utils.getFromUri(vertex.uri()),
                    Relationships.HAS_INCLUDED_VERTEX
            );
        }
    }

    public void setIncludedEdges(Set<Edge> includedEdges) {
        for (Edge edge : includedEdges) {
            getNode().createRelationshipTo(
                    utils.getFromUri(edge.uri()),
                    Relationships.HAS_INCLUDED_EDGE
            );
        }
    }

    @Override
    public Date creationDate() {
        return graphElementOperator.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return graphElementOperator.lastModificationDate();
    }

    @Override
    public String getOwner() {
        return graphElementOperator.getOwner();
    }

    @Override
    public URI uri() {
        return graphElementOperator.uri();
    }

    @Override
    public String label() {
        return graphElementOperator.label();
    }

    @Override
    public Set<Image> images() {
        return graphElementOperator.images();
    }

    @Override
    public Boolean gotImages() {
        return graphElementOperator.gotImages();
    }

    @Override
    public String comment() {
        return graphElementOperator.comment();
    }

    @Override
    public void comment(String comment) {
        graphElementOperator.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return graphElementOperator.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        graphElementOperator.addImages(
                images
        );
    }

    @Override
    public void label(String label) {
        graphElementOperator.label(label);
    }

    @Override
    public boolean hasLabel() {
        return graphElementOperator.hasLabel();
    }

    @Override
    public IdentificationPojo addGenericIdentification(Identification friendlyResource) {

        return graphElementOperator.addGenericIdentification(friendlyResource);
    }

    @Override
    public boolean equals(Object vertexToCompareAsObject) {
        return graphElementOperator.equals(vertexToCompareAsObject);
    }

    @Override
    public int hashCode() {
        return graphElementOperator.hashCode();
    }

    @Override
    public Integer minDistanceFromCenterVertex() {
        return depthInSubGraph;
    }

    @Override
    public VertexInSubGraph setMinDistanceFromCenterVertex(Integer minDistanceFromCenterVertex) {
        this.depthInSubGraph = minDistanceFromCenterVertex;
        return this;
    }

    @Override
    public void create() {
        createUsingInitialValues(map());
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        Map<String, Object> props = addCreationProperties(
                values
        );
        queryEngine.query(
                "create (n:vertex {props})", wrap(props)
        );
    }

    @Override
    public String queryPrefix() {
        return graphElementOperator.queryPrefix();
    }

    @Override
    public Node getNode() {
        if (null == node) {
            return graphElementOperator.getNode();
        }
        return node;
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        Map<String, Object> newMap = map(
                props.is_public.name(), false,
                props.number_of_connected_edges_property_name.name(), 0
        );
        newMap.putAll(
                map
        );
        newMap = graphElementOperator.addCreationProperties(
                newMap
        );
        return newMap;
    }

    @Override
    public URI getExternalResourceUri() {
        return graphElementOperator.getExternalResourceUri();
    }
}
