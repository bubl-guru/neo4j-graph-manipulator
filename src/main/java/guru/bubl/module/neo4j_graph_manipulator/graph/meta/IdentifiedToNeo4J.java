/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.meta;

import guru.bubl.module.model.meta.IdentifiedTo;
import guru.bubl.module.model.User;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.search.GraphElementSearchResult;
import guru.bubl.module.neo4j_graph_manipulator.graph.Neo4jFriendlyResource;
import guru.bubl.module.neo4j_graph_manipulator.graph.Relationships;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.identification.Neo4jIdentification;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.SearchResultGetter;

import javax.inject.Inject;
import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

public class IdentifiedToNeo4J implements IdentifiedTo {

    @Inject
    protected Connection connection;

    @Override
    public Set<GraphElementSearchResult> getForIdentificationAndUser(
            Identifier identification,
            User user
    ) {
        String query = String.format(
                "START id=node:node_auto_index('%s:\"%s\" AND %s:%s') " +
                        "MATCH id<-[]-node " +
                        "OPTIONAL MATCH node<-[relation]->related_node " +
                        "WHERE not(node-[:%s]->related_node) " +
                        "RETURN " +
                        "node.uri, node.label, node.creation_date, node.number_of_visits, node.last_modification_date, " +
                        "COLLECT([related_node.label, related_node.uri, type(relation)])[0..5] as related_nodes, " +
                        "node.external_uri as external_uri, " +
                        "node.type as type limit 10",
                Neo4jIdentification.props.external_uri,
                identification.getExternalResourceUri(),
                Neo4jFriendlyResource.props.owner,
                user.username(),
                Relationships.IDENTIFIED_TO
        );
        return new HashSet<>(
                new SearchResultGetter<>(
                        query, connection
                ).get()
        );
    }
}