/*
 * Copyright Vincent Blouin under the GPL License version 3
 */

package guru.bubl.module.neo4j_graph_manipulator.graph.graph;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import guru.bubl.module.model.Image;
import guru.bubl.module.model.UserUris;
import guru.bubl.module.model.graph.*;
import guru.bubl.module.model.graph.identification.Identifier;
import guru.bubl.module.model.graph.identification.IdentifierPojo;
import guru.bubl.module.model.json.ImageJson;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.FriendlyResourceNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.OperatorNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.image.ImagesNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.meta.IdentificationFactoryNeo4j;
import guru.bubl.module.neo4j_graph_manipulator.graph.search.GraphIndexerNeo4j;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.graphdb.Node;

import java.net.URI;
import java.util.*;

import static guru.bubl.module.neo4j_graph_manipulator.graph.RestApiUtilsNeo4j.map;
import static org.neo4j.driver.v1.Values.parameters;

public class GraphElementOperatorNeo4j implements GraphElementOperator, OperatorNeo4j {

    public enum props {
        identifications,
        sort_date,
        move_date
    }

    protected Node node;
    protected FriendlyResourceNeo4j friendlyResource;
    protected FriendlyResourceFactoryNeo4j friendlyResourceFactory;
    protected Session session;

    protected IdentificationFactoryNeo4j identificationFactory;

    protected GraphElementOperatorFactory graphElementOperatorFactory;

    public static String incrementNbFriendsOrPublicQueryPart(ShareLevel shareLevel, String variableName, String prefix) {
        return incrementOrDecrementNbFriendsOrPublicQueryPart(
                shareLevel,
                variableName,
                prefix,
                false
        );
    }

    public static String decrementNbFriendsOrPublicQueryPart(ShareLevel shareLevel, String variableName, String prefix) {
        return incrementOrDecrementNbFriendsOrPublicQueryPart(
                shareLevel,
                variableName,
                prefix,
                true
        );
    }

    private static String incrementOrDecrementNbFriendsOrPublicQueryPart(ShareLevel shareLevel, String variableName, String prefix, Boolean decrement) {
        String queryPart = "";
        if (shareLevel == ShareLevel.FRIENDS) {
            queryPart = prefix + "%s.nb_friend_neighbors = %s.nb_friend_neighbors " + (decrement ? "-" : "+") + "1 ";
        } else if (shareLevel.isPublic()) {
            queryPart = prefix + "%s.nb_public_neighbors = %s.nb_public_neighbors " + (decrement ? "-" : "+") + "1 ";
        } else {
            return queryPart;
        }
        return String.format(
                queryPart,
                variableName,
                variableName
        );
    }


    @AssistedInject
    protected GraphElementOperatorNeo4j(
            FriendlyResourceFactoryNeo4j friendlyResourceFactory,
            Session session,
            IdentificationFactoryNeo4j identificationFactory,
            GraphElementOperatorFactory graphElementOperatorFactory,
            @Assisted URI uri
    ) {
        this.friendlyResource = friendlyResourceFactory.withUri(
                uri
        );
        this.identificationFactory = identificationFactory;
        this.session = session;
        this.friendlyResourceFactory = friendlyResourceFactory;
        this.graphElementOperatorFactory = graphElementOperatorFactory;
    }

    @Override
    public Date creationDate() {
        return friendlyResource.creationDate();
    }

    @Override
    public Date lastModificationDate() {
        return friendlyResource.lastModificationDate();
    }

    public void updateLastModificationDate() {
        friendlyResource.updateLastModificationDate();
    }

    @Override
    public URI uri() {
        return friendlyResource.uri();
    }

    @Override
    public String label() {
        return friendlyResource.label();
    }

    @Override
    public void label(String label) {
        friendlyResource.label(
                label
        );
    }

    @Override
    public Set<Image> images() {
        return friendlyResource.images();
    }

    @Override
    public Boolean gotImages() {
        return friendlyResource.gotImages();
    }

    @Override
    public String comment() {
        return friendlyResource.comment();
    }

    @Override
    public void comment(String comment) {
        friendlyResource.comment(
                comment
        );
    }

    @Override
    public Boolean gotComments() {
        return friendlyResource.gotComments();
    }

    @Override
    public void addImages(Set<Image> images) {
        friendlyResource.addImages(
                images
        );
    }

    @Override
    public boolean hasLabel() {
        return friendlyResource.hasLabel();
    }

    @Override
    public String getColors() {
        Record record = session.run(
                queryPrefix() + "RETURN n.colors as colors",
                parameters(
                        "uri",
                        this.uri().toString()
                )
        ).single();
        return record.get(
                "colors"
        ).asObject() == null ? "" : record.get("colors").asString();
    }

    @Override
    public String getFont() {
        Record record = session.run(
                queryPrefix() + "RETURN n.font as font",
                parameters(
                        "uri",
                        this.uri().toString()
                )
        ).single();
        return record.get(
                "font"
        ).asObject() == null ? "" : record.get("font").asString();
    }

    @Override
    public void setColors(String colors) {
        session.run(
                queryPrefix() + "SET n.colors=$colors",
                parameters(
                        "uri",
                        uri().toString(),
                        "colors",
                        colors
                )
        );
    }

    @Override
    public void setFont(String font) {
        session.run(
                queryPrefix() + "SET n.font=$font",
                parameters(
                        "uri",
                        uri().toString(),
                        "font",
                        font
                )
        );
    }

    @Override
    public void setChildrenIndex(String childrenIndex) {
        session.run(
                queryPrefix() + "SET n.childrenIndexes=$childrenIndexes",
                parameters(
                        "uri",
                        uri().toString(),
                        "childrenIndexes",
                        childrenIndex
                )
        );
    }

    @Override
    public String getChildrenIndex() {
        Record record = session.run(
                queryPrefix() + "RETURN n.childrenIndexes as childrenIndexes",
                parameters(
                        "uri",
                        this.uri().toString()
                )
        ).single();
        return record.get(
                "childrenIndexes"
        ).asObject() == null ? "" : record.get("childrenIndexes").asString();
    }

    @Override
    public void create() {
        createUsingInitialValues(
                map()
        );
    }

    @Override
    public void createUsingInitialValues(Map<String, Object> values) {
        friendlyResource.createUsingInitialValues(values);
    }

    @Override
    public Map<URI, IdentifierPojo> addMeta(
            Identifier identification
    ) {
        return addTagAndOriginalReferenceOnesOrNot(
                identification,
                true
        );
    }

    public ShareLevel getShareLevel() {
        Record record = session.run(
                queryPrefix() + "RETURN n.shareLevel as shareLevel",
                parameters(
                        "uri",
                        this.uri().toString()
                )
        ).single();
        Integer shareLevel = record.get("shareLevel").asInt();
        return ShareLevel.get(shareLevel);

    }

    public Boolean isPublic() {
        return this.getShareLevel().isPublic();
    }

    private Map<URI, IdentifierPojo> addTagAndOriginalReferenceOnesOrNot(Identifier tag, Boolean addOriginalReferenceTags) {
        IdentifierPojo identificationPojo;
        Boolean isIdentifyingToAnIdentification = UserUris.isUriOfAnIdentifier(
                tag.getExternalResourceUri()
        );
        if (isIdentifyingToAnIdentification) {
            identificationPojo = new IdentifierPojo(
                    identificationFactory.withUri(
                            tag.getExternalResourceUri()
                    ).getExternalResourceUri(),
                    new FriendlyResourcePojo(
                            tag.getExternalResourceUri()
                    )
            );
        } else {
            identificationPojo = new IdentifierPojo(
                    new UserUris(getOwnerUsername()).generateIdentificationUri(),
                    tag
            );
        }

        identificationPojo.setCreationDate(new Date().getTime());
        final FriendlyResourceNeo4j neo4jFriendlyResource = friendlyResourceFactory.withUri(
                new UserUris(getOwnerUsername()).generateIdentificationUri()
        );
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        Date tagCreationDate = new Date();
        String searchContext = GraphIndexerNeo4j.descriptionToContext(
                tag.comment()
        );
        StatementResult result = session.run(
                AddIdentificationQueryBuilder.usingIdentificationForGraphElement(
                        identificationPojo, this
                ).build(),
                parameters(
                        "uri",
                        uri().toString(),
                        "metaUri",
                        neo4jFriendlyResource.uri().toString(),
                        "label",
                        tag.label(),
                        "comment",
                        tag.comment(),
                        "privateContext",
                        searchContext,
                        "publicContext",
                        searchContext,
                        ImagesNeo4j.props.images.name(),
                        ImageJson.toJsonArray(tag.images()),
                        "creationDate",
                        tagCreationDate.getTime(),
                        "external_uri",
                        identificationPojo.getExternalResourceUri().toString(),
                        "relationExternalUri",
                        identificationPojo.getRelationExternalResourceUri().toString(),
                        FriendlyResourceNeo4j.props.last_modification_date.name(),
                        new Date().getTime(),
                        FriendlyResourceNeo4j.props.owner.name(),
                        UserUris.ownerUserNameFromUri(uri())
                )
        );
        while (result.hasNext()) {
            Record record = result.next();
            URI externalUri = URI.create(
                    record.get("external_uri").asString()
            );
            IdentifierPojo tagPojo = new IdentifierPojo(
                    externalUri,
                    record.get("nbReferences").asInt(),
                    new FriendlyResourcePojo(
                            URI.create(
                                    record.get("uri").asString()
                            ),
                            record.get("label").asObject() == null ?
                                    "" : record.get("label").asString(),
                            record.get("images").asObject() == null ?
                                    new HashSet<>() : ImageJson.fromJson(record.get("images").asString()),
                            record.get("comment").asObject() == null ?
                                    "" : record.get("comment").asString(),
                            record.get("creation_date").asLong(),
                            record.get("last_modification_date").asLong()
                    )
            );
            Boolean isReference = tag.getExternalResourceUri().equals(
                    this.uri()
            );

            Boolean isOwnerOfExternalUri = UserUris.ownerUserNameFromUri(
                    tag.getExternalResourceUri()
            ).equals(getOwnerUsername());

            if (isOwnerOfExternalUri && !isReference && addOriginalReferenceTags) {
                Map<URI, IdentifierPojo> existingTags = getIdentifications();
                Map<URI, IdentifierPojo> referenceTags = graphElementOperatorFactory.withUri(
                        tag.getExternalResourceUri()
                ).getIdentifications();
                for (IdentifierPojo otherIdentifier : referenceTags.values()) {
                    if (!existingTags.containsKey(otherIdentifier.getExternalResourceUri())) {
                        otherIdentifier = addTagAndOriginalReferenceOnesOrNot(
                                otherIdentifier,
                                false
                        ).get(otherIdentifier.getExternalResourceUri());
                    }
                    identifications.put(otherIdentifier.getExternalResourceUri(), otherIdentifier);
                }
            }
            Boolean justCreatedTag = tagCreationDate.equals(tagPojo.creationDate());
            if (!isReference && isOwnerOfExternalUri && justCreatedTag) {
                Map<URI, IdentifierPojo> tags = graphElementOperatorFactory.withUri(
                        tag.getExternalResourceUri()
                ).addMeta(
                        tagPojo
                );
                tagPojo = tags.get(tag.getExternalResourceUri());
            }
            identifications.put(
                    tag.getExternalResourceUri(),
                    tagPojo
            );
        }
        return identifications;
    }

    @Override
    public void removeIdentification(Identifier identification) {
        session.run(
                String.format(
                        "%s MATCH (n)-[r:IDENTIFIED_TO]->(i{uri:$metaUri}) " +
                                "DELETE r " +
                                "SET i.nb_references=i.nb_references -1, " +
                                FriendlyResourceNeo4j.LAST_MODIFICATION_QUERY_PART +
                                "RETURN i.uri as uri",
                        queryPrefix()
                ),
                parameters(
                        "uri",
                        uri().toString(),
                        "metaUri",
                        identification.uri().toString(),
                        "last_modification_date",
                        new Date().getTime()
                )
        );
        if (identification.getExternalResourceUri() != null && identification.getExternalResourceUri().equals(this.uri())) {
            identificationFactory.withUri(identification.uri()).setExternalResourceUri(
                    identification.uri()
            );
        }
    }

    @Override
    public void remove() {
        removeAllIdentifications();
        friendlyResource.remove();
    }

    @Override
    public boolean equals(Object graphElementToCompare) {
        return friendlyResource.equals(graphElementToCompare);
    }

    @Override
    public int hashCode() {
        return friendlyResource.hashCode();
    }

    @Override
    public String queryPrefix() {
        return friendlyResource.queryPrefix();
    }

    @Override
    public Map<String, Object> addCreationProperties(Map<String, Object> map) {
        return friendlyResource.addCreationProperties(
                map
        );
    }

    public GraphElementPojo pojoFromCreationProperties(Map<String, Object> creationProperties) {
        return new GraphElementPojo(
                friendlyResource.pojoFromCreationProperties(
                        creationProperties
                )
        );
    }

    @Override
    public Map<URI, IdentifierPojo> getIdentifications() {
        Map<URI, IdentifierPojo> identifications = new HashMap<>();
        StatementResult rs = session.run(
                String.format(
                        "%sMATCH (n)-[r:IDENTIFIED_TO]->(identification) " +
                                "RETURN identification.uri as uri, " +
                                "identification.external_uri as external_uri, " +
                                "identification.nb_references as nbReferences, " +
                                "r.relation_external_uri as r_x_u",
                        queryPrefix()
                ),
                parameters(
                        "uri", uri().toString()
                )
        );
        while (rs.hasNext()) {
            Record record = rs.next();
            URI uri = URI.create(
                    record.get("uri").asString()
            );
            URI externalUri = URI.create(
                    record.get("external_uri").asString()
            );
            IdentifierPojo identification = new IdentifierPojo(
                    externalUri,
                    new Integer(record.get("nbReferences").asInt()),
                    new FriendlyResourcePojo(
                            uri
                    )
            );
            String relationExternalUriString = record.get("r_x_u").asString();
            identification.setRelationExternalResourceUri(
                    relationExternalUriString == null ? Identifier.DEFAULT_IDENTIFIER_RELATION_EXTERNAL_URI :
                            URI.create(
                                    relationExternalUriString
                            )
            );
            identifications.put(
                    externalUri,
                    identification
            );
        }
        return identifications;
    }

    public void removeAllIdentifications() {
        session.run(
                String.format(
                        "%s MATCH (n)-[r:IDENTIFIED_TO]->(i) " +
                                "DELETE r " +
                                "SET i.nb_references=i.nb_references -1 ",
                        queryPrefix()
                ),
                parameters(
                        "uri", this.uri().toString()
                )
        );
    }

    public GraphElementOperator forkUsingCreationPropertiesAndCache(GraphElementOperator clone, Map<String, Object> additionalCreateValues, GraphElement cache) {
        FriendlyResourcePojo original = new FriendlyResourcePojo(
                uri(),
                cache.label()
        );
        original.setComment(
                cache.comment()
        );
        Map<String, Object> createValues = map(
                FriendlyResourceNeo4j.props.label.name(), original.label(),
                FriendlyResourceNeo4j.props.comment.name(), original.comment()
        );
        createValues.putAll(
                additionalCreateValues
        );
        clone.createUsingInitialValues(
                createValues
        );
        clone.addMeta(
                new IdentifierPojo(
                        this.uri(),
                        original
                )
        );
        cache.getIdentifications().values().forEach(
                clone::addMeta
        );
        return clone;
    }
}