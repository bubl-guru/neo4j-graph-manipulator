/*
 * Copyright Vincent Blouin under the Mozilla Public License 1.1
 */

package org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.subgraph;

import org.triple_brain.module.model.graph.GraphElementPojo;
import org.triple_brain.module.neo4j_graph_manipulator.graph.graph.extractor.FriendlyResourceFromExtractorQueryRow;

import java.util.Map;

public class GraphElementFromExtractorQueryRow {

    private Map<String, Object> row;
    private String key;

    public static GraphElementFromExtractorQueryRow usingRowAndKey(Map<String, Object> row, String key) {
        return new GraphElementFromExtractorQueryRow(
                row,
                key
        );
    }

    protected GraphElementFromExtractorQueryRow(Map<String, Object> row, String key) {
        this.row = row;
        this.key = key;
    }

    public GraphElementPojo build() {
        GraphElementPojo graphElement = init();
        return graphElement;
    }

    private GraphElementPojo init() {
        return new GraphElementPojo(
                FriendlyResourceFromExtractorQueryRow.usingRowAndNodeKey(
                        row,
                        key
                ).build(),
                IdentificationsFromExtractorQueryRow.usingRowAndKey(
                        row,
                        key
                ).build()
        );
    }
}
