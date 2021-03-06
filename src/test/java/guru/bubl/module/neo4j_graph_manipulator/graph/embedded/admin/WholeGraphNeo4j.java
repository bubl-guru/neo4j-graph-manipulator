/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.embedded.admin;

import guru.bubl.module.model.User;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.graph.graph_element.GraphElementOperator;
import guru.bubl.module.model.graph.graph_element.GraphElementOperatorFactory;
import guru.bubl.module.model.graph.relation.RelationOperator;
import guru.bubl.module.model.graph.tag.TagFactory;
import guru.bubl.module.model.graph.tag.TagOperator;
import guru.bubl.module.model.graph.vertex.VertexOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.relation.RelationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.vertex.VertexFactoryNeo4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import static org.neo4j.driver.Values.parameters;

public class WholeGraphNeo4j implements WholeGraph {

    @Inject
    protected Driver driver;

    @Inject
    protected VertexFactoryNeo4j neo4jVertexFactory;

    @Inject
    protected RelationFactoryNeo4j neo4jEdgeFactory;

    @Inject
    protected GraphElementOperatorFactory graphElementFactory;

    @Inject
    protected TagFactory tagFactory;

    @Override
    public Set<VertexOperator> getAllVertices() {
        return getAllVerticesOfUserOrNot(null);
    }

    @Override
    public Set<VertexOperator> getAllVerticesOfUser(User user) {
        return getAllVerticesOfUserOrNot(user);
    }

    private Set<VertexOperator> getAllVerticesOfUserOrNot(User user) {
        String query = String.format(
                "MATCH(n:Vertex%s) RETURN n.uri as uri",
                user == null ? "" : " {owner:$username}"
        );
        Set<VertexOperator> vertices = new HashSet<>();
        try (Session session = driver.session()) {
            Result rs = session.run(
                    query,
                    user == null ? parameters() : parameters(
                            "username",
                            user.username()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                vertices.add(
                        neo4jVertexFactory.withUri(
                                URI.create(
                                        record.get("uri").asString()
                                )
                        )
                );
            }
            return vertices;
        }
    }

    @Override
    public Set<RelationOperator> getAllEdges() {
        return getAllEdgesOfUserOrNot(null);
    }

    @Override
    public Set<RelationOperator> getAllEdgesOfUser(User user) {
        return getAllEdgesOfUserOrNot(user);
    }

    private Set<RelationOperator> getAllEdgesOfUserOrNot(User user) {
        String query = String.format(
                "MATCH(n:Edge%s) RETURN n.uri as uri",
                user == null ? "" : "{owner:$username}"
        );
        Set<RelationOperator> edges = new HashSet<>();
        try (Session session = driver.session()) {
            Result rs = session.run(
                    query,
                    user == null ? parameters() : parameters(
                            "username",
                            user.username()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                edges.add(
                        neo4jEdgeFactory.withUri(
                                URI.create(
                                        record.get("uri").asString()
                                )
                        )
                );
            }
            return edges;
        }
    }

    @Override
    public Set<GraphElementOperator> getAllGraphElements() {
        return getAllGraphElementsOfUserOrNot(null);
    }

    Set<GraphElementOperator> getAllGraphElementsOfUserOrNot(User user) {
        String query = String.format(
                "MATCH(n:GraphElement%s) RETURN n.uri as uri",
                user == null ? "" : " {owner:$owner}"
        );
        Set<GraphElementOperator> graphElements = new HashSet<>();
        try (Session session = driver.session()) {
            Result rs = session.run(
                    query,
                    user == null ? parameters() : parameters(
                            "owner", user.username()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                graphElements.add(
                        graphElementFactory.withUri(
                                URI.create(
                                        record.get("uri").asString()
                                )
                        )
                );
            }
            return graphElements;
        }
    }

    @Override
    public Set<TagOperator> getAllTags() {
        return getAllTagsOfUserOrNot(null);
    }

    @Override
    public Set<TagOperator> getAllTagsOfUser(User user) {
        return getAllTagsOfUserOrNot(user);
    }

    private Set<TagOperator> getAllTagsOfUserOrNot(User user) {
        String query = String.format(
                "MATCH(n:Meta%s) RETURN n.uri as uri",
                user == null ? "" : "{owner:$username}"
        );
        Set<TagOperator> identifications = new HashSet<>();
        try (Session session = driver.session()) {
            Result rs = session.run(
                    query,
                    user == null ? parameters() : parameters(
                            "username",
                            user.username()
                    )
            );
            while (rs.hasNext()) {
                Record record = rs.next();
                identifications.add(
                        tagFactory.withUri(
                                URI.create(
                                        record.get("uri").asString()
                                )
                        )
                );
            }
            return identifications;
        }
    }
}
