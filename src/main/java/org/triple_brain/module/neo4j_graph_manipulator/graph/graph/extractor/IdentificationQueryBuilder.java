/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor;

import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.Neo4jGraphElementOperator;

public class IdentificationQueryBuilder {

    public static String identificationReturnQueryPart(String prefix) {
        return QueryUtils.getPropertyUsingContainerNameQueryPart(
                prefix,
                Neo4jGraphElementOperator.props.identifications.toString()
        );
    }
}
