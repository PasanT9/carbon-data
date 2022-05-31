package org.wso2.carbon.dataservices.core.odata.serializer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.olingo.commons.api.data.AbstractEntityCollection;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Linked;
import org.apache.olingo.commons.api.data.Operation;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.constants.EdmTypeKind;
import org.apache.olingo.commons.api.edm.geo.ComposedGeospatial;
import org.apache.olingo.commons.api.edm.geo.Geospatial;
import org.apache.olingo.commons.api.edm.geo.GeospatialCollection;
import org.apache.olingo.commons.api.edm.geo.LineString;
import org.apache.olingo.commons.api.edm.geo.MultiLineString;
import org.apache.olingo.commons.api.edm.geo.MultiPoint;
import org.apache.olingo.commons.api.edm.geo.MultiPolygon;
import org.apache.olingo.commons.api.edm.geo.Point;
import org.apache.olingo.commons.api.edm.geo.Polygon;
import org.apache.olingo.commons.api.edm.geo.Geospatial.Type;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.ComplexSerializerOptions;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.ReferenceCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ReferenceSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.serializer.SerializerStreamResult;
import org.apache.olingo.server.api.serializer.SerializerException.MessageKeys;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.LevelsExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.core.serializer.SerializerResultImpl;
import org.apache.olingo.server.core.serializer.json.ODataErrorSerializer;
import org.apache.olingo.server.core.serializer.json.ServiceDocumentJsonSerializer;
import org.apache.olingo.server.core.serializer.utils.CircleStreamBuffer;
import org.apache.olingo.server.core.serializer.utils.ContentTypeHelper;
import org.apache.olingo.server.core.serializer.utils.ContextURLBuilder;
import org.apache.olingo.server.core.serializer.utils.ExpandSelectHelper;
import org.apache.olingo.server.core.uri.UriHelperImpl;
import org.apache.olingo.server.core.uri.queryoption.ExpandOptionImpl;

public class ODataJsonSerializer extends AbstractODataSerializer {
    private static final Map<Geospatial.Type, String> geoValueTypeToJsonName;

    static {
        Map<Geospatial.Type, String> temp = new EnumMap(Geospatial.Type.class);
        temp.put(Type.POINT, "Point");
        temp.put(Type.MULTIPOINT, "MultiPoint");
        temp.put(Type.LINESTRING, "LineString");
        temp.put(Type.MULTILINESTRING, "MultiLineString");
        temp.put(Type.POLYGON, "Polygon");
        temp.put(Type.MULTIPOLYGON, "MultiPolygon");
        temp.put(Type.GEOSPATIALCOLLECTION, "GeometryCollection");
        geoValueTypeToJsonName = Collections.unmodifiableMap(temp);
    }

    private final boolean isIEEE754Compatible;
    private final boolean isODataMetadataNone;
    private final boolean isODataMetadataFull;

    public ODataJsonSerializer(ContentType contentType) {
        this.isIEEE754Compatible = ContentTypeHelper.isODataIEEE754Compatible(contentType);
        this.isODataMetadataNone = ContentTypeHelper.isODataMetadataNone(contentType);
        this.isODataMetadataFull = ContentTypeHelper.isODataMetadataFull(contentType);
    }

    public SerializerResult serviceDocument(ServiceMetadata metadata, String serviceRoot) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var7;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            (new ServiceDocumentJsonSerializer(metadata, serviceRoot, this.isODataMetadataNone)).writeServiceDocument(json);
            json.close();
            outputStream.close();
            var7 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var11) {
            cachedException = new SerializerException("An I/O exception occurred.", var11, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var7;
    }

    public SerializerResult metadataDocument(ServiceMetadata serviceMetadata) throws SerializerException {
        throw new SerializerException("Metadata in JSON format not supported!", MessageKeys.JSON_METADATA, new String[0]);
    }

    public SerializerResult error(ODataServerError error) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var6;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            (new ODataErrorSerializer()).writeErrorDocument(json, error);
            json.close();
            outputStream.close();
            var6 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var10) {
            cachedException = new SerializerException("An I/O exception occurred.", var10, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var6;
    }

    public SerializerResult entityCollection(ServiceMetadata metadata, EdmEntityType entityType, AbstractEntityCollection entitySet, EntityCollectionSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;
        boolean pagination = false;

        SerializerResult var12;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            json.writeStartObject();
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            String name = contextURL == null ? null : contextURL.getEntitySetOrSingletonOrType();
            this.writeContextURL(contextURL, json);
            this.writeMetadataETag(metadata, json);
            if (options != null && options.getCount() != null && options.getCount().getValue()) {
                this.writeInlineCount("", entitySet.getCount(), json);
            }

            this.writeOperations(entitySet.getOperations(), json);
            json.writeFieldName("value");
            if (options == null) {
                this.writeEntitySet(metadata, entityType, entitySet, (ExpandOption) null, (Integer) null, (SelectOption) null, false, (Set) null, name, json);
            } else {
                this.writeEntitySet(metadata, entityType, entitySet, options.getExpand(), (Integer) null, options.getSelect(), options.getWriteOnlyReferences(), (Set) null, name, json);
            }

            this.writeNextLink(entitySet, json, pagination);
            this.writeDeltaLink(entitySet, json, pagination);
            json.close();
            outputStream.close();
            var12 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var16) {
            cachedException = new SerializerException("An I/O exception occurred.", var16, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var12;
    }

    public SerializerStreamResult entityCollectionStreamed(ServiceMetadata metadata, EdmEntityType entityType, EntityIterator entities, EntityCollectionSerializerOptions options) throws SerializerException {
        return ODataWritableContent.with(entities, entityType, this, metadata, options).build();
    }

    public void entityCollectionIntoStream(ServiceMetadata metadata, EdmEntityType entityType, EntityIterator entitySet, EntityCollectionSerializerOptions options, OutputStream outputStream) throws SerializerException {
        boolean pagination = false;

        try {
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            json.writeStartObject();
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            this.writeContextURL(contextURL, json);
            this.writeMetadataETag(metadata, json);

            json.writeFieldName("value");
            String name = contextURL == null ? null : contextURL.getEntitySetOrSingletonOrType();
            if (options == null) {
                this.writeEntitySet(metadata, entityType, entitySet, (ExpandOption) null, (Integer) null, (SelectOption) null, false, (Set) null, name, json);
            } else {
                this.writeEntitySet(metadata, entityType, entitySet, options.getExpand(), (Integer) null, options.getSelect(), options.getWriteOnlyReferences(), (Set) null, name, json);
            }

            if (options != null && options.getCount() != null && options.getCount().getValue()) {
                this.writeInlineCount("", entitySet.getCount(), json);
            }

            this.writeNextLink(entitySet, json, pagination);
            json.close();
        } catch (IOException var11) {
            SerializerException cachedException = new SerializerException("An I/O exception occurred.", var11, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        }
    }

    public SerializerResult entity(ServiceMetadata metadata, EdmEntityType entityType, Entity entity, EntitySerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var11;
        try {
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            String name = contextURL == null ? null : contextURL.getEntitySetOrSingletonOrType();
            this.writeEntity(metadata, entityType, entity, contextURL, options == null ? null : options.getExpand(), (Integer) null, options == null ? null : options.getSelect(), options == null ? false : options.getWriteOnlyReferences(), (Set) null, name, json);
            json.close();
            outputStream.close();
            var11 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var15) {
            cachedException = new SerializerException("An I/O exception occurred.", var15, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var11;
    }

    ContextURL checkContextURL(ContextURL contextURL) throws SerializerException {
        if (this.isODataMetadataNone) {
            return null;
        } else if (contextURL == null) {
            throw new SerializerException("ContextURL null!", MessageKeys.NO_CONTEXT_URL, new String[0]);
        } else {
            return contextURL;
        }
    }

    protected void writeEntitySet(ServiceMetadata metadata, EdmEntityType entityType, AbstractEntityCollection entitySet, ExpandOption expand, Integer toDepth, SelectOption select, boolean onlyReference, Set<String> ancestors, String name, JsonGenerator json) throws IOException, SerializerException {
        json.writeStartArray();
        Iterator var11 = entitySet.iterator();

        while (var11.hasNext()) {
            Entity entity = (Entity) var11.next();
            if (onlyReference) {
                json.writeStartObject();
                json.writeStringField("@odata.id", this.getEntityId(entity, entityType, name));
                json.writeEndObject();
            } else {
                this.writeEntity(metadata, entityType, entity, (ContextURL) null, expand, toDepth, select, false, ancestors, name, json);
            }
        }

        json.writeEndArray();
    }

    private String getEntityId(Entity entity, EdmEntityType entityType, String name) throws SerializerException {
        if (entity.getId() == null) {
            if (entity == null || entityType == null || entityType.getKeyPredicateNames() == null || name == null) {
                throw new SerializerException("Entity id is null.", MessageKeys.MISSING_ID, new String[0]);
            }

            UriHelper uriHelper = new UriHelperImpl();
            entity.setId(URI.create(name + '(' + uriHelper.buildKeyPredicate(entityType, entity) + ')'));
        }

        return entity.getId().toASCIIString();
    }

    private boolean areKeyPredicateNamesSelected(SelectOption select, EdmEntityType type) {
        if (select != null && !ExpandSelectHelper.isAll(select)) {
            Set<String> selected = ExpandSelectHelper.getSelectedPropertyNames(select.getSelectItems());
            Iterator var4 = type.getKeyPredicateNames().iterator();

            String key;
            do {
                if (!var4.hasNext()) {
                    return true;
                }

                key = (String) var4.next();
            } while (selected.contains(key));

            return false;
        } else {
            return true;
        }
    }

    protected void writeEntity(ServiceMetadata metadata, EdmEntityType entityType, Entity entity, ContextURL contextURL, ExpandOption expand, Integer toDepth, SelectOption select, boolean onlyReference, Set<String> ancestors, String name, JsonGenerator json) throws IOException, SerializerException {
        boolean cycle = false;
        if (expand != null) {
            if (ancestors == null) {
                ancestors = new HashSet();
            }

            cycle = !((Set) ancestors).add(this.getEntityId(entity, entityType, name));
        }

        try {
            json.writeStartObject();
            if (!this.isODataMetadataNone) {
                if (contextURL != null) {
                    this.writeContextURL(contextURL, json);
                    this.writeMetadataETag(metadata, json);
                }

                if (entity.getETag() != null) {
                    json.writeStringField("@odata.etag", entity.getETag());
                }

                if (entityType.hasStream()) {
                    if (entity.getMediaETag() != null) {
                        json.writeStringField("@odata.mediaEtag", entity.getMediaETag());
                    }

                    if (entity.getMediaContentType() != null) {
                        json.writeStringField("@odata.mediaContentType", entity.getMediaContentType());
                    }

                    if (entity.getMediaContentSource() != null) {
                        json.writeStringField("@odata.mediaReadLink", entity.getMediaContentSource().toString());
                    }

                    if (entity.getMediaEditLinks() != null && !entity.getMediaEditLinks().isEmpty()) {
                        json.writeStringField("@odata.mediaEditLink", ((Link) entity.getMediaEditLinks().get(0)).getHref());
                    }
                }
            }

            if (!cycle && !onlyReference) {
                EdmEntityType resolvedType = this.resolveEntityType(metadata, entityType, entity.getType());
                if (!this.isODataMetadataNone && !resolvedType.equals(entityType) || this.isODataMetadataFull) {
                    json.writeStringField("@odata.type", "#" + entity.getType());
                }

                if (!this.isODataMetadataNone && !this.areKeyPredicateNamesSelected(select, resolvedType) || this.isODataMetadataFull) {
                    json.writeStringField("@odata.id", this.getEntityId(entity, resolvedType, name));
                }

                if (this.isODataMetadataFull) {
                    if (entity.getSelfLink() != null) {
                        json.writeStringField("@odata.readLink", entity.getSelfLink().getHref());
                    }

                    if (entity.getEditLink() != null) {
                        json.writeStringField("@odata.editLink", entity.getEditLink().getHref());
                    }
                }

                this.writeProperties(metadata, resolvedType, entity.getProperties(), select, json);
                this.writeNavigationProperties(metadata, resolvedType, entity, expand, toDepth, (Set) ancestors, name, json);
                this.writeOperations(entity.getOperations(), json);
            } else {
                json.writeStringField("@odata.id", this.getEntityId(entity, entityType, name));
            }

            json.writeEndObject();
        } finally {
            if (expand != null && !cycle && ancestors != null) {
                ((Set) ancestors).remove(this.getEntityId(entity, entityType, name));
            }

        }

    }

    private void writeOperations(List<Operation> operations, JsonGenerator json) throws IOException {
        if (this.isODataMetadataFull) {
            Iterator var3 = operations.iterator();

            while (var3.hasNext()) {
                Operation operation = (Operation) var3.next();
                json.writeObjectFieldStart(operation.getMetadataAnchor());
                json.writeStringField("title", operation.getTitle());
                json.writeStringField("target", operation.getTarget().toASCIIString());
                json.writeEndObject();
            }
        }

    }

    protected EdmEntityType resolveEntityType(ServiceMetadata metadata, EdmEntityType baseType, String derivedTypeName) throws SerializerException {
        if (derivedTypeName != null && !baseType.getFullQualifiedName().getFullQualifiedNameAsString().equals(derivedTypeName)) {
            EdmEntityType derivedType = metadata.getEdm().getEntityType(new FullQualifiedName(derivedTypeName));
            if (derivedType == null) {
                throw new SerializerException("EntityType not found", MessageKeys.UNKNOWN_TYPE, new String[]{derivedTypeName});
            } else {
                for (EdmEntityType type = derivedType.getBaseType(); type != null; type = type.getBaseType()) {
                    if (type.getFullQualifiedName().equals(baseType.getFullQualifiedName())) {
                        return derivedType;
                    }
                }

                throw new SerializerException("Wrong base type", MessageKeys.WRONG_BASE_TYPE, new String[]{derivedTypeName, baseType.getFullQualifiedName().getFullQualifiedNameAsString()});
            }
        } else {
            return baseType;
        }
    }

    protected EdmComplexType resolveComplexType(ServiceMetadata metadata, EdmComplexType baseType, String derivedTypeName) throws SerializerException {
        String fullQualifiedName = baseType.getFullQualifiedName().getFullQualifiedNameAsString();
        if (derivedTypeName != null && !fullQualifiedName.equals(derivedTypeName)) {
            EdmComplexType derivedType = metadata.getEdm().getComplexType(new FullQualifiedName(derivedTypeName));
            if (derivedType == null) {
                throw new SerializerException("Complex Type not found", MessageKeys.UNKNOWN_TYPE, new String[]{derivedTypeName});
            } else {
                for (EdmComplexType type = derivedType.getBaseType(); type != null; type = type.getBaseType()) {
                    if (type.getFullQualifiedName().equals(baseType.getFullQualifiedName())) {
                        return derivedType;
                    }
                }

                throw new SerializerException("Wrong base type", MessageKeys.WRONG_BASE_TYPE, new String[]{derivedTypeName, baseType.getFullQualifiedName().getFullQualifiedNameAsString()});
            }
        } else {
            return baseType;
        }
    }

    protected void writeProperties(ServiceMetadata metadata, EdmStructuredType type, List<Property> properties, SelectOption select, JsonGenerator json) throws IOException, SerializerException {
        boolean all = ExpandSelectHelper.isAll(select);
        Set<String> selected = all ? new HashSet() : ExpandSelectHelper.getSelectedPropertyNames(select.getSelectItems());
        Iterator var8 = type.getPropertyNames().iterator();

        while (true) {
            String propertyName;
            do {
                if (!var8.hasNext()) {
                    return;
                }

                propertyName = (String) var8.next();
            } while (!all && !((Set) selected).contains(propertyName));

            EdmProperty edmProperty = type.getStructuralProperty(propertyName);
            Property property = this.findProperty(propertyName, properties);
            Set<List<String>> selectedPaths = !all && !edmProperty.isPrimitive() ? ExpandSelectHelper.getSelectedPaths(select.getSelectItems(), propertyName) : null;
            this.writeProperty(metadata, edmProperty, property, selectedPaths, json);
        }
    }

    protected void writeNavigationProperties(ServiceMetadata metadata, EdmStructuredType type, Linked linked, ExpandOption expand, Integer toDepth, Set<String> ancestors, String name, JsonGenerator json) throws SerializerException, IOException {
        if (this.isODataMetadataFull) {
            Iterator var9 = type.getNavigationPropertyNames().iterator();

            while (var9.hasNext()) {
                String propertyName = (String) var9.next();
                Link navigationLink = linked.getNavigationLink(propertyName);
                if (navigationLink != null) {
                    json.writeStringField(propertyName + "@odata.navigationLink", navigationLink.getHref());
                }

                Link associationLink = linked.getAssociationLink(propertyName);
                if (associationLink != null) {
                    json.writeStringField(propertyName + "@odata.associationLink", associationLink.getHref());
                }
            }
        }

        if (toDepth != null && toDepth > 1 || toDepth == null && ExpandSelectHelper.hasExpand(expand)) {
            ExpandItem expandAll = ExpandSelectHelper.getExpandAll(expand);
            Iterator var19 = type.getNavigationPropertyNames().iterator();

            while (true) {
                String propertyName;
                ExpandItem innerOptions;
                do {
                    if (!var19.hasNext()) {
                        return;
                    }

                    propertyName = (String) var19.next();
                    innerOptions = ExpandSelectHelper.getExpandItem(expand.getExpandItems(), propertyName);
                } while (innerOptions == null && expandAll == null && toDepth == null);

                Integer levels = null;
                EdmNavigationProperty property = type.getNavigationProperty(propertyName);
                Link navigationLink = linked.getNavigationLink(property.getName());
                ExpandOption childExpand = null;
                LevelsExpandOption levelsOption = null;
                if (innerOptions != null) {
                    levelsOption = innerOptions.getLevelsOption();
                    childExpand = levelsOption == null ? innerOptions.getExpandOption() : (new ExpandOptionImpl()).addExpandItem(innerOptions);
                } else if (expandAll != null) {
                    levels = 1;
                    levelsOption = expandAll.getLevelsOption();
                    childExpand = (new ExpandOptionImpl()).addExpandItem(expandAll);
                }

                if (levelsOption != null) {
                    levels = levelsOption.isMax() ? Integer.MAX_VALUE : levelsOption.getValue();
                }

                if (toDepth != null) {
                    levels = toDepth - 1;
                    childExpand = expand;
                }

                this.writeExpandedNavigationProperty(metadata, property, navigationLink, (ExpandOption) childExpand, levels, innerOptions == null ? null : innerOptions.getSelectOption(), innerOptions == null ? null : innerOptions.getCountOption(), innerOptions == null ? false : innerOptions.hasCountPath(), innerOptions == null ? false : innerOptions.isRef(), ancestors, name, json);
            }
        }
    }

    protected void writeExpandedNavigationProperty(ServiceMetadata metadata, EdmNavigationProperty property, Link navigationLink, ExpandOption innerExpand, Integer toDepth, SelectOption innerSelect, CountOption innerCount, boolean writeOnlyCount, boolean writeOnlyRef, Set<String> ancestors, String name, JsonGenerator json) throws IOException, SerializerException {
        if (property.isCollection()) {
            if (writeOnlyCount) {
                if (navigationLink != null && navigationLink.getInlineEntitySet() != null) {
                    this.writeInlineCount(property.getName(), navigationLink.getInlineEntitySet().getCount(), json);
                } else {
                    this.writeInlineCount(property.getName(), 0, json);
                }
            } else if (navigationLink != null && navigationLink.getInlineEntitySet() != null) {
                if (innerCount != null && innerCount.getValue()) {
                    this.writeInlineCount(property.getName(), navigationLink.getInlineEntitySet().getCount(), json);
                }

                json.writeFieldName(property.getName());
                this.writeEntitySet(metadata, property.getType(), navigationLink.getInlineEntitySet(), innerExpand, toDepth, innerSelect, writeOnlyRef, ancestors, name, json);
            } else {
                if (innerCount != null && innerCount.getValue()) {
                    this.writeInlineCount(property.getName(), 0, json);
                }

                json.writeFieldName(property.getName());
                json.writeStartArray();
                json.writeEndArray();
            }
        } else {
            json.writeFieldName(property.getName());
            if (navigationLink != null && navigationLink.getInlineEntity() != null) {
                this.writeEntity(metadata, property.getType(), navigationLink.getInlineEntity(), (ContextURL) null, innerExpand, toDepth, innerSelect, writeOnlyRef, ancestors, name, json);
            } else {
                json.writeNull();
            }
        }

    }

    private boolean isStreamProperty(EdmProperty edmProperty) {
        EdmType type = edmProperty.getType();
        return edmProperty.isPrimitive() && type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Stream);
    }

    protected void writeProperty(ServiceMetadata metadata, EdmProperty edmProperty, Property property, Set<List<String>> selectedPaths, JsonGenerator json) throws IOException, SerializerException {
        boolean isStreamProperty = this.isStreamProperty(edmProperty);
        this.writePropertyType(edmProperty, json);
        if (!isStreamProperty) {
            json.writeFieldName(edmProperty.getName());
        }

        if (property != null && !property.isNull()) {
            this.writePropertyValue(metadata, edmProperty, property, selectedPaths, json);
        } else {
            if (edmProperty.isNullable() == Boolean.FALSE) {
                throw new SerializerException("Non-nullable property not present!", MessageKeys.MISSING_PROPERTY, new String[]{edmProperty.getName()});
            }

            if (!isStreamProperty) {
                if (edmProperty.isCollection()) {
                    json.writeStartArray();
                    json.writeEndArray();
                } else {
                    json.writeNull();
                }
            }
        }

    }

    private void writePropertyType(EdmProperty edmProperty, JsonGenerator json) throws SerializerException, IOException {
        if (this.isODataMetadataFull) {
            String typeName = edmProperty.getName() + "@odata.type";
            EdmType type = edmProperty.getType();
            if (type.getKind() != EdmTypeKind.ENUM && type.getKind() != EdmTypeKind.DEFINITION) {
                if (edmProperty.isPrimitive()) {
                    if (edmProperty.isCollection()) {
                        json.writeStringField(typeName, "#Collection(" + type.getFullQualifiedName().getName() + ")");
                    } else if (type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Boolean) && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Double) && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.String)) {
                        json.writeStringField(typeName, "#" + type.getFullQualifiedName().getName());
                    }
                } else {
                    if (type.getKind() != EdmTypeKind.COMPLEX) {
                        throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{edmProperty.getName()});
                    }

                    if (edmProperty.isCollection()) {
                        json.writeStringField(typeName, "#Collection(" + type.getFullQualifiedName().getFullQualifiedNameAsString() + ")");
                    }
                }
            } else if (edmProperty.isCollection()) {
                json.writeStringField(typeName, "#Collection(" + type.getFullQualifiedName().getFullQualifiedNameAsString() + ")");
            } else {
                json.writeStringField(typeName, "#" + type.getFullQualifiedName().getFullQualifiedNameAsString());
            }

        }
    }

    private void writePropertyValue(ServiceMetadata metadata, EdmProperty edmProperty, Property property, Set<List<String>> selectedPaths, JsonGenerator json) throws IOException, SerializerException {
        EdmType type = edmProperty.getType();

        try {
            if (!edmProperty.isPrimitive() && type.getKind() != EdmTypeKind.ENUM && type.getKind() != EdmTypeKind.DEFINITION) {
                if (!property.isComplex()) {
                    throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{edmProperty.getName()});
                }

                if (edmProperty.isCollection()) {
                    this.writeComplexCollection(metadata, (EdmComplexType) type, property, selectedPaths, json);
                } else {
                    this.writeComplex(metadata, (EdmComplexType) type, property, selectedPaths, json);
                }
            } else if (edmProperty.isCollection()) {
                this.writePrimitiveCollection((EdmPrimitiveType) type, property, edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(), edmProperty.isUnicode(), json);
            } else {
                this.writePrimitive((EdmPrimitiveType) type, property, edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(), edmProperty.isUnicode(), json);
            }

        } catch (EdmPrimitiveTypeException var8) {
            throw new SerializerException("Wrong value for property!", var8, MessageKeys.WRONG_PROPERTY_VALUE, new String[]{edmProperty.getName(), property.getValue().toString()});
        }
    }

    private void writeComplex(ServiceMetadata metadata, EdmComplexType type, Property property, Set<List<String>> selectedPaths, JsonGenerator json) throws IOException, SerializerException {
        json.writeStartObject();
        String derivedName = property.getType();
        EdmComplexType resolvedType = null;
        if (!type.getFullQualifiedName().getFullQualifiedNameAsString().equals(derivedName)) {
            if (type.getBaseType() != null && type.getBaseType().getFullQualifiedName().getFullQualifiedNameAsString().equals(derivedName)) {
                resolvedType = this.resolveComplexType(metadata, type.getBaseType(), type.getFullQualifiedName().getFullQualifiedNameAsString());
            } else {
                resolvedType = this.resolveComplexType(metadata, type, derivedName);
            }
        } else {
            resolvedType = this.resolveComplexType(metadata, type, derivedName);
        }

        if (!this.isODataMetadataNone && !resolvedType.equals(type) || this.isODataMetadataFull) {
            json.writeStringField("@odata.type", "#" + resolvedType.getFullQualifiedName().getFullQualifiedNameAsString());
        }

        this.writeComplexValue(metadata, resolvedType, property.asComplex().getValue(), selectedPaths, json);
        json.writeEndObject();
    }

    private void writePrimitiveCollection(EdmPrimitiveType type, Property property, Boolean isNullable, Integer maxLength, Integer precision, Integer scale, Boolean isUnicode, JsonGenerator json) throws IOException, SerializerException {
        json.writeStartArray();
        Iterator var9 = property.asCollection().iterator();

        while (var9.hasNext()) {
            Object value = var9.next();
            switch (property.getValueType()) {
                case COLLECTION_PRIMITIVE:
                case COLLECTION_ENUM:
                case COLLECTION_GEOSPATIAL:
                    try {
                        this.writePrimitiveValue(property.getName(), type, value, isNullable, maxLength, precision, scale, isUnicode, json);
                        break;
                    } catch (EdmPrimitiveTypeException var12) {
                        throw new SerializerException("Wrong value for property!", var12, MessageKeys.WRONG_PROPERTY_VALUE, new String[]{property.getName(), property.getValue().toString()});
                    }
                default:
                    throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{property.getName()});
            }
        }

        json.writeEndArray();
    }

    private void writeComplexCollection(ServiceMetadata metadata, EdmComplexType type, Property property, Set<List<String>> selectedPaths, JsonGenerator json) throws IOException, SerializerException {
        json.writeStartArray();
        Iterator var7 = property.asCollection().iterator();

        while (var7.hasNext()) {
            Object value = var7.next();
            EdmComplexType derivedType = ((ComplexValue) value).getTypeName() != null ? metadata.getEdm().getComplexType(new FullQualifiedName(((ComplexValue) value).getTypeName())) : type;
            switch (property.getValueType()) {
                case COLLECTION_COMPLEX:
                    json.writeStartObject();
                    if (this.isODataMetadataFull || !this.isODataMetadataNone && !derivedType.equals(type)) {
                        json.writeStringField("@odata.type", "#" + derivedType.getFullQualifiedName().getFullQualifiedNameAsString());
                    }

                    this.writeComplexValue(metadata, derivedType, ((ComplexValue) value).getValue(), selectedPaths, json);
                    json.writeEndObject();
                    break;
                default:
                    throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{property.getName()});
            }
        }

        json.writeEndArray();
    }

    private void writePrimitive(EdmPrimitiveType type, Property property, Boolean isNullable, Integer maxLength, Integer precision, Integer scale, Boolean isUnicode, JsonGenerator json) throws EdmPrimitiveTypeException, IOException, SerializerException {
        if (property.isPrimitive()) {
            this.writePrimitiveValue(property.getName(), type, property.asPrimitive(), isNullable, maxLength, precision, scale, isUnicode, json);
        } else if (property.isGeospatial()) {
            this.writeGeoValue(property.getName(), type, property.asGeospatial(), isNullable, json);
        } else {
            if (!property.isEnum()) {
                throw new SerializerException("Inconsistent property type!", MessageKeys.INCONSISTENT_PROPERTY_TYPE, new String[]{property.getName()});
            }

            this.writePrimitiveValue(property.getName(), type, property.asEnum(), isNullable, maxLength, precision, scale, isUnicode, json);
        }

    }

    protected void writePrimitiveValue(String name, EdmPrimitiveType type, Object primitiveValue, Boolean isNullable, Integer maxLength, Integer precision, Integer scale, Boolean isUnicode, JsonGenerator json) throws EdmPrimitiveTypeException, IOException {
        String value = type.valueToString(primitiveValue, isNullable, maxLength, precision, scale, isUnicode);
        if (value == null) {
            json.writeNull();
        } else if (type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Boolean)) {
            json.writeBoolean(Boolean.parseBoolean(value));
        } else if (type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Byte) || type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Double) || type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int16) || type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int32) || type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.SByte) || type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Single) || (type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Decimal) || type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int64)) && !this.isIEEE754Compatible) {
            json.writeNumber(value);
        } else if (type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Stream)) {
            if (primitiveValue instanceof Link) {
                Link stream = (Link) primitiveValue;
                if (!this.isODataMetadataNone) {
                    if (stream.getMediaETag() != null) {
                        json.writeStringField(name + "@odata.mediaEtag", stream.getMediaETag());
                    }

                    if (stream.getType() != null) {
                        json.writeStringField(name + "@odata.mediaContentType", stream.getType());
                    }
                }

                if (this.isODataMetadataFull) {
                    if (stream.getRel() != null && stream.getRel().equals("http://docs.oasis-open.org/odata/ns/mediaresource/")) {
                        json.writeStringField(name + "@odata.mediaReadLink", stream.getHref());
                    }

                    if (stream.getRel() == null || stream.getRel().equals("http://docs.oasis-open.org/odata/ns/edit-media/")) {
                        json.writeStringField(name + "@odata.mediaEditLink", stream.getHref());
                    }
                }
            }
        } else {
            json.writeString(value);
        }

    }

    protected void writeGeoValue(String name, EdmPrimitiveType type, Geospatial geoValue, Boolean isNullable, JsonGenerator json) throws EdmPrimitiveTypeException, IOException, SerializerException {
        if (geoValue == null) {
            if (isNullable != null && !isNullable) {
                throw new EdmPrimitiveTypeException("The literal 'null' is not allowed.");
            }

            json.writeNull();
        } else {
            if (!type.getDefaultType().isAssignableFrom(geoValue.getClass())) {
                throw new EdmPrimitiveTypeException("The value type " + geoValue.getClass() + " is not supported.");
            }

            if (geoValue.getSrid() != null && geoValue.getSrid().isNotDefault()) {
                throw new SerializerException("Non-standard SRID not supported!", MessageKeys.WRONG_PROPERTY_VALUE, new String[]{name, geoValue.toString()});
            }

            json.writeStartObject();
            json.writeStringField("type", (String) geoValueTypeToJsonName.get(geoValue.getGeoType()));
            json.writeFieldName(geoValue.getGeoType() == Type.GEOSPATIALCOLLECTION ? "geometries" : "coordinates");
            json.writeStartArray();
            Iterator var6;
            label54:
            switch (geoValue.getGeoType()) {
                case POINT:
                    this.writeGeoPoint(json, (Point) geoValue);
                    break;
                case MULTIPOINT:
                    this.writeGeoPoints(json, (MultiPoint) geoValue);
                    break;
                case LINESTRING:
                    this.writeGeoPoints(json, (LineString) geoValue);
                    break;
                case MULTILINESTRING:
                    var6 = ((MultiLineString) geoValue).iterator();

                    while (true) {
                        if (!var6.hasNext()) {
                            break label54;
                        }

                        LineString lineString = (LineString) var6.next();
                        json.writeStartArray();
                        this.writeGeoPoints(json, lineString);
                        json.writeEndArray();
                    }
                case POLYGON:
                    this.writeGeoPolygon(json, (Polygon) geoValue);
                    break;
                case MULTIPOLYGON:
                    var6 = ((MultiPolygon) geoValue).iterator();

                    while (true) {
                        if (!var6.hasNext()) {
                            break label54;
                        }

                        Polygon polygon = (Polygon) var6.next();
                        json.writeStartArray();
                        this.writeGeoPolygon(json, polygon);
                        json.writeEndArray();
                    }
                case GEOSPATIALCOLLECTION:
                    var6 = ((GeospatialCollection) geoValue).iterator();

                    while (var6.hasNext()) {
                        Geospatial element = (Geospatial) var6.next();
                        this.writeGeoValue(name, EdmPrimitiveTypeFactory.getInstance(element.getEdmPrimitiveTypeKind()), element, isNullable, json);
                    }
            }

            json.writeEndArray();
            json.writeEndObject();
        }

    }

    private void writeGeoPoint(JsonGenerator json, Point point) throws IOException {
        json.writeNumber(point.getX());
        json.writeNumber(point.getY());
        if (point.getZ() != 0.0) {
            json.writeNumber(point.getZ());
        }

    }

    private void writeGeoPoints(JsonGenerator json, ComposedGeospatial<Point> points) throws IOException {
        Iterator var3 = points.iterator();

        while (var3.hasNext()) {
            Point point = (Point) var3.next();
            json.writeStartArray();
            this.writeGeoPoint(json, point);
            json.writeEndArray();
        }

    }

    private void writeGeoPolygon(JsonGenerator json, Polygon polygon) throws IOException {
        json.writeStartArray();
        this.writeGeoPoints(json, polygon.getExterior());
        json.writeEndArray();
        if (!polygon.getInterior().isEmpty()) {
            json.writeStartArray();
            this.writeGeoPoints(json, polygon.getInterior());
            json.writeEndArray();
        }

    }

    protected void writeComplexValue(ServiceMetadata metadata, EdmComplexType type, List<Property> properties, Set<List<String>> selectedPaths, JsonGenerator json) throws IOException, SerializerException {
        Iterator var6 = type.getPropertyNames().iterator();

        while (true) {
            String propertyName;
            Property property;
            do {
                if (!var6.hasNext()) {
                    return;
                }

                propertyName = (String) var6.next();
                property = this.findProperty(propertyName, properties);
            } while (selectedPaths != null && !ExpandSelectHelper.isSelected(selectedPaths, propertyName));

            this.writeProperty(metadata, (EdmProperty) type.getProperty(propertyName), property, selectedPaths == null ? null : ExpandSelectHelper.getReducedSelectedPaths(selectedPaths, propertyName), json);
        }
    }

    private Property findProperty(String propertyName, List<Property> properties) {
        Iterator var3 = properties.iterator();

        Property property;
        do {
            if (!var3.hasNext()) {
                return null;
            }

            property = (Property) var3.next();
        } while (!propertyName.equals(property.getName()));

        return property;
    }

    public SerializerResult primitive(ServiceMetadata metadata, EdmPrimitiveType type, Property property, PrimitiveSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var10;
        try {
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            json.writeStartObject();
            this.writeContextURL(contextURL, json);
            this.writeMetadataETag(metadata, json);
            this.writeOperations(property.getOperations(), json);
            if (property.isNull()) {
                throw new SerializerException("Property value can not be null.", MessageKeys.NULL_INPUT, new String[0]);
            }

            json.writeFieldName("value");
            this.writePrimitive(type, property, options == null ? null : options.isNullable(), options == null ? null : options.getMaxLength(), options == null ? null : options.getPrecision(), options == null ? null : options.getScale(), options == null ? null : options.isUnicode(), json);
            json.writeEndObject();
            json.close();
            outputStream.close();
            var10 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var15) {
            cachedException = new SerializerException("An I/O exception occurred.", var15, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (EdmPrimitiveTypeException var16) {
            cachedException = new SerializerException("Wrong value for property!", var16, MessageKeys.WRONG_PROPERTY_VALUE, new String[]{property.getName(), property.getValue().toString()});
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var10;
    }

    public SerializerResult complex(ServiceMetadata metadata, EdmComplexType type, Property property, ComplexSerializerOptions options) throws SerializerException {
//        OutputStream outputStream = null;
//        SerializerException cachedException = null;
//
//        SerializerResult var13;
//        try {
//            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
//            String name = contextURL == null ? null : contextURL.getEntitySetOrSingletonOrType();
//            CircleStreamBuffer buffer = new CircleStreamBuffer();
//            outputStream = buffer.getOutputStream();
//            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
//            json.writeStartObject();
//            this.writeContextURL(contextURL, json);
//            this.writeMetadataETag(metadata, json);
//            EdmComplexType resolvedType = null;
//            if (!type.getFullQualifiedName().getFullQualifiedNameAsString().equals(property.getType())) {
//                if (type.getBaseType() != null && type.getBaseType().getFullQualifiedName().getFullQualifiedNameAsString().equals(property.getType())) {
//                    resolvedType = this.resolveComplexType(metadata, type.getBaseType(), type.getFullQualifiedName().getFullQualifiedNameAsString());
//                } else {
//                    resolvedType = this.resolveComplexType(metadata, type, property.getType());
//                }
//            } else {
//                resolvedType = this.resolveComplexType(metadata, type, property.getType());
//            }
//
//            if (!this.isODataMetadataNone && !resolvedType.equals(type) || this.isODataMetadataFull) {
//                json.writeStringField("@odata.type", "#" + resolvedType.getFullQualifiedName().getFullQualifiedNameAsString());
//            }
//
//            this.writeOperations(property.getOperations(), json);
//            List<Property> values = property.isNull() ? Collections.emptyList() : property.asComplex().getValue();
//            this.writeProperties(metadata, type, values, options == null ? null : options.getSelect(), json);
//            if (!property.isNull() && property.isComplex()) {
//                this.writeNavigationProperties(metadata, type, property.asComplex(), options == null ? null : options.getExpand(), (Integer)null, (Set)null, name, json);
//            }
//
//            json.writeEndObject();
//            json.close();
//            outputStream.close();
//            var13 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
//        } catch (IOException var17) {
//            cachedException = new SerializerException("An I/O exception occurred.", var17, MessageKeys.IO_EXCEPTION, new String[0]);
//            throw cachedException;
//        } finally {
//            this.closeCircleStreamBufferOutput(outputStream, cachedException);
//        }

        return null;
    }

    public SerializerResult primitiveCollection(ServiceMetadata metadata, EdmPrimitiveType type, Property property, PrimitiveSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var10;
        try {
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            json.writeStartObject();
            this.writeContextURL(contextURL, json);
            this.writeMetadataETag(metadata, json);
            if (this.isODataMetadataFull) {
                json.writeStringField("@odata.type", "#Collection(" + type.getFullQualifiedName().getName() + ")");
            }

            this.writeOperations(property.getOperations(), json);
            json.writeFieldName("value");
            this.writePrimitiveCollection(type, property, options == null ? null : options.isNullable(), options == null ? null : options.getMaxLength(), options == null ? null : options.getPrecision(), options == null ? null : options.getScale(), options == null ? null : options.isUnicode(), json);
            json.writeEndObject();
            json.close();
            outputStream.close();
            var10 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var14) {
            cachedException = new SerializerException("An I/O exception occurred.", var14, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var10;
    }

    public SerializerResult complexCollection(ServiceMetadata metadata, EdmComplexType type, Property property, ComplexSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var10;
        try {
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            json.writeStartObject();
            this.writeContextURL(contextURL, json);
            this.writeMetadataETag(metadata, json);
            if (this.isODataMetadataFull) {
                json.writeStringField("@odata.type", "#Collection(" + type.getFullQualifiedName().getFullQualifiedNameAsString() + ")");
            }

            this.writeOperations(property.getOperations(), json);
            json.writeFieldName("value");
            this.writeComplexCollection(metadata, type, property, (Set) null, json);
            json.writeEndObject();
            json.close();
            outputStream.close();
            var10 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var14) {
            cachedException = new SerializerException("An I/O exception occurred.", var14, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var10;
    }

    public SerializerResult reference(ServiceMetadata metadata, EdmEntitySet edmEntitySet, Entity entity, ReferenceSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var11;
        try {
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            UriHelper uriHelper = new UriHelperImpl();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            json.writeStartObject();
            this.writeContextURL(contextURL, json);
            json.writeStringField("@odata.id", uriHelper.buildCanonicalURL(edmEntitySet, entity));
            json.writeEndObject();
            json.close();
            outputStream.close();
            var11 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (IOException var15) {
            cachedException = new SerializerException("An I/O exception occurred.", var15, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var11;
    }

    public SerializerResult referenceCollection(ServiceMetadata metadata, EdmEntitySet edmEntitySet, AbstractEntityCollection entityCollection, ReferenceCollectionSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;
        boolean pagination = false;

        try {
            ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            UriHelper uriHelper = new UriHelperImpl();
            outputStream = buffer.getOutputStream();
            JsonGenerator json = (new JsonFactory()).createGenerator(outputStream);
            json.writeStartObject();
            this.writeContextURL(contextURL, json);
            if (options != null && options.getCount() != null && options.getCount().getValue()) {
                this.writeInlineCount("", entityCollection.getCount(), json);
            }

            json.writeArrayFieldStart("value");
            Iterator var12 = entityCollection.iterator();

            while (var12.hasNext()) {
                Entity entity = (Entity) var12.next();
                json.writeStartObject();
                json.writeStringField("@odata.id", uriHelper.buildCanonicalURL(edmEntitySet, entity));
                json.writeEndObject();
            }

            json.writeEndArray();
            this.writeNextLink(entityCollection, json, pagination);
            json.writeEndObject();
            json.close();
            outputStream.close();
            SerializerResult var19 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
            return var19;
        } catch (IOException var17) {
            cachedException = new SerializerException("An I/O exception occurred.", var17, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }
    }

    void writeContextURL(ContextURL contextURL, JsonGenerator json) throws IOException {
        if (!this.isODataMetadataNone && contextURL != null) {
            json.writeStringField("@odata.context", ContextURLBuilder.create(contextURL).toASCIIString());
        }

    }

    void writeMetadataETag(ServiceMetadata metadata, JsonGenerator json) throws IOException {
        if (!this.isODataMetadataNone && metadata != null && metadata.getServiceMetadataETagSupport() != null && metadata.getServiceMetadataETagSupport().getMetadataETag() != null) {
            json.writeStringField("@odata.metadataEtag", metadata.getServiceMetadataETagSupport().getMetadataETag());
        }

    }

    void writeInlineCount(String propertyName, Integer count, JsonGenerator json) throws IOException {
        if (count != null) {
            if (this.isIEEE754Compatible) {
                json.writeStringField(propertyName + "@odata.count", String.valueOf(count));
            } else {
                json.writeNumberField(propertyName + "@odata.count", count);
            }
        }

    }

    void writeNextLink(AbstractEntityCollection entitySet, JsonGenerator json, boolean pagination) throws IOException {
        if (entitySet.getNext() != null) {
            pagination = true;
            json.writeStringField("@odata.nextLink", entitySet.getNext().toASCIIString());
        } else {
            pagination = false;
        }

    }

    void writeDeltaLink(AbstractEntityCollection entitySet, JsonGenerator json, boolean pagination) throws IOException {
        if (entitySet.getDeltaLink() != null && !pagination) {
            json.writeStringField("@odata.deltaLink", entitySet.getDeltaLink().toASCIIString());
        }

    }
}
