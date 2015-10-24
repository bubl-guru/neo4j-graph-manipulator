/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.admin;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.common_utils.NoExRun;
import guru.bubl.module.model.WholeGraph;
import guru.bubl.module.model.admin.WholeGraphAdmin;
import guru.bubl.module.model.graph.Identification;
import guru.bubl.module.model.graph.IdentificationOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.graph.Neo4jIdentification;

import java.sql.Connection;

public class Neo4jWholeGraphAdmin implements WholeGraphAdmin {

    protected Connection connection;
    protected WholeGraph wholeGraph;

    @AssistedInject
    protected Neo4jWholeGraphAdmin(
            Connection connection,
            @Assisted WholeGraph wholeGraph
    ) {
        this.connection = connection;
        this.wholeGraph = wholeGraph;
    }

    @Override
    public void refreshNumberOfReferencesToAllIdentifications() {
        wholeGraph.getAllIdentifications().forEach(
                this::refreshNumberOfReferencesToIdentification
        );
    }

    @Override
    public WholeGraph getWholeGraph() {
        return wholeGraph;
    }

    private void refreshNumberOfReferencesToIdentification(IdentificationOperator identification) {
        Neo4jIdentification neo4jIdentification = (Neo4jIdentification) identification;
        String query = String.format(
                "%s MATCH n<-[r]-() " +
                        "WITH n, count(r) as nbReferences " +
                        "SET n.%s=nbReferences",
                neo4jIdentification.queryPrefix(),
                Neo4jIdentification.props.nb_references
        );
        NoExRun.wrap(() -> connection.createStatement().execute(query)).get();
    }
}
