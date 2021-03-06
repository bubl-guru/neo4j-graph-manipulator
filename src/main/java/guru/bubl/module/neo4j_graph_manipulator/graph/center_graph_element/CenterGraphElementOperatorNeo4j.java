/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.center_graph_element;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.FriendlyResource;
import guru.bubl.module.model.center_graph_element.CenterGraphElementOperator;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import java.util.Date;

import static org.neo4j.driver.Values.parameters;

public class CenterGraphElementOperatorNeo4j implements CenterGraphElementOperator {

    public enum props {
        nb_visits,
        last_center_date
    }

    private Driver driver;
    private FriendlyResourceNeo4j neo4jFriendlyResource;

    @AssistedInject
    protected CenterGraphElementOperatorNeo4j(
            Driver driver,
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            @Assisted FriendlyResource friendlyResource
    ) {
        this.driver = driver;
        this.neo4jFriendlyResource = friendlyResourceFactory.withUri(
                friendlyResource.uri()
        );
    }

    @Override
    public void incrementNumberOfVisits() {
        try (Session session = driver.session()) {
            session.run(
                    neo4jFriendlyResource.queryPrefix() + "SET n.nb_visits= CASE WHEN n.nb_visits is null THEN 1 ELSE n.nb_visits + 1 END",
                    parameters(
                            "uri",
                            neo4jFriendlyResource.uri().toString()
                    )
            );
        }
    }

    @Override
    public Integer getNumberOfVisits() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    neo4jFriendlyResource.queryPrefix() + "RETURN n.nb_visits as number;",
                    parameters(
                            "uri", neo4jFriendlyResource.uri().toString()
                    )
            ).single();
            Object numberAsObject = record.get("number").asObject();
            return numberAsObject == null ? 0 : (Integer) numberAsObject;
        }
    }

    @Override
    public void updateLastCenterDate() {
        this.setLastCenterDate(new Date());
    }

    @Override
    public void setLastCenterDate(Date lastCenterDate) {
        try (Session session = driver.session()) {
            session.run(
                    neo4jFriendlyResource.queryPrefix() + "SET n.last_center_date=$lastCenterDate",
                    parameters(
                            "uri",
                            neo4jFriendlyResource.uri().toString(),
                            "lastCenterDate", lastCenterDate.getTime()
                    )
            );
        }
    }

    @Override
    public Date getLastCenterDate() {
        try (Session session = driver.session()) {
            Record record = session.run(
                    neo4jFriendlyResource.queryPrefix() + "RETURN n.last_center_date as date;",
                    parameters(
                            "uri", neo4jFriendlyResource.uri().toString()
                    )
            ).single();
            Object dateAsObject = record.get("date").asObject();
            return dateAsObject == null ? null : new Date(
                    (Long) (dateAsObject)
            );
        }
    }

    @Override
    public void remove() {
        try (Session session = driver.session()) {
            session.run(
                    "MATCH(n:GraphElement{uri:$uri}) REMOVE n.last_center_date, n.nb_visits",
                    parameters(
                            "uri", neo4jFriendlyResource.uri().toString()
                    )
            );
        }
    }
}
