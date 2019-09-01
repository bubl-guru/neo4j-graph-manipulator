/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.GraphElementType;
import guru.bubl.module.model.graph.ShareLevel;
import guru.bubl.module.model.graph.edge.Edge;
import guru.bubl.module.model.graph.edge.EdgePojo;
import guru.bubl.module.model.graph.subgraph.SubGraphPojo;
import guru.bubl.module.model.graph.vertex.VertexInSubGraph;
import guru.bubl.module.model.graph.vertex.VertexInSubGraphPojo;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.UserGraphNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.edge.EdgeOperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.IdentificationQueryBuilder;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.extractor.QueryUtils;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexInSubGraphOperatorNeo4j;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import java.net.URI;
import java.util.*;

import static org.neo4j.driver.v1.Values.parameters;

public class SubGraphExtractorNeo4j {

    public final static String
            INCLUDED_VERTEX_QUERY_KEY = "iv",
            INCLUDED_EDGE_QUERY_KEY = "ie",
            GRAPH_ELEMENT_QUERY_KEY = "ge";
    private URI centerBubbleUri;
    private Integer depth;
    private Integer resultsLimit;
    private SubGraphPojo subGraph = SubGraphPojo.withVerticesAndEdges(
            new HashMap<>(),
            new HashMap<>()
    );

    private Set<ShareLevel> inShareLevels = new HashSet<>();

    protected Session session;

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Session session,
            @Assisted URI centerBubbleUri,
            @Assisted("depth") Integer depth
    ) {
        this(
                session,
                centerBubbleUri,
                depth,
                null
        );
    }

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Session session,
            @Assisted URI centerBubbleUri,
            @Assisted("depth") Integer depth,
            @Assisted("resultsLimit") Integer resultsLimit
    ) {
        this.session = session;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = depth;
        this.resultsLimit = resultsLimit;
        inShareLevels.clear();
        inShareLevels.add(ShareLevel.PRIVATE);
    }

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Session session,
            @Assisted URI centerBubbleUri,
            @Assisted Set<ShareLevel> inShareLevels
    ) {
        this.session = session;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = 1;
        this.inShareLevels = inShareLevels;
    }

    @AssistedInject
    protected SubGraphExtractorNeo4j(
            Session session,
            @Assisted URI centerBubbleUri,
            @Assisted Set<ShareLevel> inShareLevels,
            @Assisted Integer depth
    ) {
        this.session = session;
        this.centerBubbleUri = centerBubbleUri;
        this.depth = depth;
        this.inShareLevels = inShareLevels;
    }

    public SubGraphPojo load() {
        StatementResult rs = session.run(
                queryToGetGraph(),
                parameters(
                        "centerUri", centerBubbleUri.toString()
                )
        );
        while (rs.hasNext()) {
            Record record = rs.next();
            switch (getGraphElementTypeFromRow(record)) {
                case Vertex:
                    ShareLevel shareLevel = ShareLevel.get(record.get("ge.shareLevel").asInt());
                    if (!inShareLevels.contains(shareLevel)) {
                        break;
                    }
                    addVertexUsingRow(
                            record,
                            shareLevel
                    );
                    break;
                case Edge:
                    addEdgeUsingRow(
                            record
                    );
                    break;
            }
        }
        Iterator<EdgePojo> it = subGraph.edges().values().iterator();
        while (it.hasNext()) {
            EdgePojo edge = it.next();
            Boolean hasSourceVertex = subGraph.vertices().containsKey(
                    edge.sourceVertex().uri()
            );
            Boolean hasDestinationVertex = subGraph.vertices().containsKey(
                    edge.destinationVertex().uri()
            );
            if (!hasSourceVertex || !hasDestinationVertex) {
                it.remove();
            }
        }
        return subGraph;
    }

    private GraphElementType getGraphElementTypeFromRow(Record record) {
        List<String> types = (List) record.get("type").asList();
        GraphElementType type = null;
        for (String typeStr : types) {
            GraphElementType graphElementType = GraphElementType.valueOf(typeStr);
            if (!GraphElementType.commonTypes.contains(graphElementType)) {
                type = graphElementType;
            }
        }
        return type;
    }

    private VertexInSubGraph addVertexUsingRow(Record row, ShareLevel shareLevel) {
        VertexInSubGraph vertex = new VertexFromExtractorQueryRow(
                row,
                SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY
        ).build(shareLevel);
        subGraph.addVertex(
                (VertexInSubGraphPojo) vertex
        );
        return vertex;
    }

    private String queryToGetGraph() {
        return
                "MATCH(start_node:Resource{uri:$centerUri}) " +
                        getMatchQueryPart() +
//                        "OPTIONAL MATCH (" + GRAPH_ELEMENT_QUERY_KEY + ")-[:HAS_INCLUDED_VERTEX]->(" + INCLUDED_VERTEX_QUERY_KEY + ") " +
//                        "OPTIONAL MATCH(" + GRAPH_ELEMENT_QUERY_KEY + ")-[:HAS_INCLUDED_EDGE]->(" + INCLUDED_EDGE_QUERY_KEY + ") " +
                        "OPTIONAL MATCH (ge)-[idr:IDENTIFIED_TO]->(id) " +
                        "RETURN " +
                        vertexAndEdgeCommonQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        vertexReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        edgeReturnQueryPart(GRAPH_ELEMENT_QUERY_KEY) +
                        IdentificationQueryBuilder.identificationReturnQueryPart() +
                        "labels(ge) as type ";
    }

    private String getMatchQueryPart() {
        if (UserUris.isUriOfAnIdentifier(centerBubbleUri)) {
            return "MATCH (start_node)<-[:" +
                    Relationships.IDENTIFIED_TO +
                    "]-(it) " +
                    "MATCH (it)<-[:" +
                    Relationships.SOURCE_VERTEX + "|" +
                    Relationships.DESTINATION_VERTEX + "*0.." + depth +
                    "]->(" + SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY + ") ";

        } else {
            return "MATCH (start_node)<-[:" +
                    Relationships.SOURCE_VERTEX +
                    "|" + Relationships.DESTINATION_VERTEX + "*0.." + depth * 2 +
                    "]->(" + SubGraphExtractorNeo4j.GRAPH_ELEMENT_QUERY_KEY + ") ";
        }
    }

    private String vertexAndEdgeCommonQueryPart(String prefix) {
        return FriendlyResourceQueryBuilder.returnQueryPartUsingPrefix(prefix);
    }

    private String edgeReturnQueryPart(String prefix) {
        return edgeSpecificPropertiesQueryPartUsingPrefix(prefix);
    }

    private String vertexReturnQueryPart(String prefix) {
        return vertexSpecificPropertiesQueryPartUsingPrefix(prefix) +
//                includedVertexQueryPart(INCLUDED_VERTEX_QUERY_KEY) +
//                includedEdgeQueryPart(INCLUDED_EDGE_QUERY_KEY) +
                FriendlyResourceQueryBuilder.imageReturnQueryPart(prefix);
    }

    private static String includedVertexQueryPart(String key) {
        return "COLLECT([" +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        key,
                        UserGraphNeo4j.URI_PROPERTY_NAME
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        key,
                        FriendlyResourceNeo4j.props.label.toString()
                ) +
                "]) as " + key + ", ";
    }

    private static String includedEdgeQueryPart(String key) {
        return "COLLECT([" +
                edgeSpecificPropertiesQueryPartUsingPrefix(key) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        key,
                        UserGraphNeo4j.URI_PROPERTY_NAME
                ) +
                QueryUtils.getLastPropertyUsingContainerNameQueryPart(
                        key,
                        FriendlyResourceNeo4j.props.label.toString()
                ) +
                "]) as " + key + ", ";
    }

    public static String includedElementQueryPart(String key) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                UserGraphNeo4j.URI_PROPERTY_NAME
        ) + QueryUtils.getPropertyUsingContainerNameQueryPart(
                key,
                FriendlyResourceNeo4j.props.label.toString()
        );
    }

    public static String edgeSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                EdgeOperatorNeo4j.props.source_vertex_uri.toString()
        ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        EdgeOperatorNeo4j.props.destination_vertex_uri.toString()
                );
    }

    private String vertexSpecificPropertiesQueryPartUsingPrefix(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                VertexInSubGraphOperatorNeo4j.props.number_of_connected_edges_property_name.toString()
        ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexInSubGraphOperatorNeo4j.props.nb_public_neighbors.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexInSubGraphOperatorNeo4j.props.nb_friend_neighbors.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        VertexInSubGraphOperatorNeo4j.props.suggestions.name()
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "childrenIndexes"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "colors"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "font"
                ) +
                QueryUtils.getPropertyUsingContainerNameQueryPart(
                        prefix,
                        "shareLevel"
                );
    }

    private Edge addEdgeUsingRow(Record row) {
        EdgePojo edge = (EdgePojo) EdgeFromExtractorQueryRow.usingRow(
                row
        ).build();
        subGraph.addEdge(edge);
        return edge;
    }
}