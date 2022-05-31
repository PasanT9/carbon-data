package org.wso2.carbon.dataservices.core.odata.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.AbstractEntityCollection;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Linked;
import org.apache.olingo.commons.api.data.Operation;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.Operation.Type;
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
import org.apache.olingo.commons.api.ex.ODataErrorDetail;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
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
import org.apache.olingo.server.core.serializer.utils.CircleStreamBuffer;
import org.apache.olingo.server.core.serializer.utils.ContextURLBuilder;
import org.apache.olingo.server.core.serializer.utils.ExpandSelectHelper;
import org.apache.olingo.server.core.serializer.xml.MetadataDocumentXmlSerializer;
import org.apache.olingo.server.core.serializer.xml.ServiceDocumentXmlSerializer;
import org.apache.olingo.server.core.uri.UriHelperImpl;
import org.apache.olingo.server.core.uri.queryoption.ExpandOptionImpl;

public class ODataXmlSerializer extends AbstractODataSerializer {
    private static final String ATOM = "a";
    private static final String NS_ATOM = "http://www.w3.org/2005/Atom";
    private static final String METADATA = "m";
    private static final String NS_METADATA = "http://docs.oasis-open.org/odata/ns/metadata";
    private static final String DATA = "d";
    private static final String NS_DATA = "http://docs.oasis-open.org/odata/ns/data";

    public ODataXmlSerializer() {
    }

    static String replaceInvalidCharacters(EdmPrimitiveType expectedType, String value, Boolean isUniCode, String invalidCharacterReplacement) {
        if (expectedType instanceof EdmString && invalidCharacterReplacement != null && isUniCode != null && isUniCode) {
            String s = value;
            StringBuilder result = null;

            for (int i = 0; i < s.length(); ++i) {
                char c = s.charAt(i);
                if (c <= ' ' && c != ' ' && c != '\n' && c != '\t' && c != '\r') {
                    if (result == null) {
                        result = new StringBuilder();
                        result.append(s.substring(0, i));
                    }

                    result.append(invalidCharacterReplacement);
                } else if (result != null) {
                    result.append(c);
                }
            }

            if (result == null) {
                return value;
            } else {
                return result.toString();
            }
        } else {
            return value;
        }
    }

    public SerializerResult serviceDocument(ServiceMetadata metadata, String serviceRoot) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var8;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            ServiceDocumentXmlSerializer serializer = new ServiceDocumentXmlSerializer(metadata, serviceRoot);
            serializer.writeServiceDocument(writer);
            writer.flush();
            writer.close();
            outputStream.close();
            var8 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (XMLStreamException var13) {
            cachedException = new SerializerException("An I/O exception occurred.", var13, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (IOException var14) {
            cachedException = new SerializerException("An I/O exception occurred.", var14, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var8;
    }

    public SerializerResult metadataDocument(ServiceMetadata serviceMetadata) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var7;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            MetadataDocumentXmlSerializer serializer = new MetadataDocumentXmlSerializer(serviceMetadata);
            serializer.writeMetadataDocument(writer);
            writer.flush();
            writer.close();
            outputStream.close();
            var7 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (XMLStreamException var12) {
            cachedException = new SerializerException("An I/O exception occurred.", var12, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (IOException var13) {
            cachedException = new SerializerException("An I/O exception occurred.", var13, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var7;
    }

    public SerializerResult error(ODataServerError error) throws SerializerException {
        if (error == null) {
            throw new SerializerException("ODataError object MUST NOT be null!", MessageKeys.NULL_INPUT, new String[0]);
        } else {
            OutputStream outputStream = null;
            SerializerException cachedException = null;

            SerializerResult var15;
            try {
                CircleStreamBuffer buffer = new CircleStreamBuffer();
                outputStream = buffer.getOutputStream();
                XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement("error");
                writer.writeDefaultNamespace("http://docs.oasis-open.org/odata/ns/metadata");
                this.writeErrorDetails(String.valueOf(error.getCode()), error.getMessage(), error.getTarget(), writer);
                if (error.getDetails() != null && !error.getDetails().isEmpty()) {
                    writer.writeStartElement("details");
                    Iterator var6 = error.getDetails().iterator();

                    while (var6.hasNext()) {
                        ODataErrorDetail inner = (ODataErrorDetail) var6.next();
                        this.writeErrorDetails(inner.getCode(), inner.getMessage(), inner.getTarget(), writer);
                    }

                    writer.writeEndElement();
                }

                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
                writer.close();
                outputStream.close();
                var15 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
            } catch (XMLStreamException var12) {
                cachedException = new SerializerException("An I/O exception occurred.", var12, MessageKeys.IO_EXCEPTION, new String[0]);
                throw cachedException;
            } catch (IOException var13) {
                cachedException = new SerializerException("An I/O exception occurred.", var13, MessageKeys.IO_EXCEPTION, new String[0]);
                throw cachedException;
            } finally {
                this.closeCircleStreamBufferOutput(outputStream, cachedException);
            }

            return var15;
        }
    }

    private void writeErrorDetails(String code, String message, String target, XMLStreamWriter writer) throws XMLStreamException {
        if (code != null) {
            writer.writeStartElement("code");
            writer.writeCharacters(code);
            writer.writeEndElement();
        }

        writer.writeStartElement("message");
        writer.writeCharacters(message);
        writer.writeEndElement();
        if (target != null) {
            writer.writeStartElement("target");
            writer.writeCharacters(target);
            writer.writeEndElement();
        }

    }

    public SerializerResult entityCollection(ServiceMetadata metadata, EdmEntityType entityType, AbstractEntityCollection entitySet, EntityCollectionSerializerOptions options) throws SerializerException {
        ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
        String name = contextURL == null ? null : contextURL.getEntitySetOrSingletonOrType();
        if (options != null && options.getWriteOnlyReferences()) {
            ReferenceCollectionSerializerOptions rso = ReferenceCollectionSerializerOptions.with().contextURL(contextURL).build();
            return this.entityReferenceCollection(entitySet, rso);
        } else {
            OutputStream outputStream = null;
            SerializerException cachedException = null;

            SerializerResult var12;
            try {
                CircleStreamBuffer buffer = new CircleStreamBuffer();
                outputStream = buffer.getOutputStream();
                XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeStartElement("a", "feed", "http://www.w3.org/2005/Atom");
                writer.writeNamespace("a", "http://www.w3.org/2005/Atom");
                writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
                writer.writeNamespace("d", "http://docs.oasis-open.org/odata/ns/data");
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
                this.writeMetadataETag(metadata, writer);
                this.writeOperations(entitySet.getOperations(), writer);
                if (options != null && options.getId() != null) {
                    writer.writeStartElement("a", "id", "http://www.w3.org/2005/Atom");
                    writer.writeCharacters(options.getId());
                    writer.writeEndElement();
                }

                if (options != null && options.getCount() != null && options.getCount().getValue() && entitySet.getCount() != null) {
                    this.writeCount(entitySet, writer);
                }

                if (entitySet.getNext() != null) {
                    this.writeNextLink(entitySet, writer);
                }

                boolean writeOnlyRef = options != null && options.getWriteOnlyReferences();
                if (options == null) {
                    this.writeEntitySet(metadata, entityType, entitySet, (ExpandOption) null, (Integer) null, (SelectOption) null, (String) null, writer, writeOnlyRef, name, (Set) null);
                } else {
                    this.writeEntitySet(metadata, entityType, entitySet, options.getExpand(), (Integer) null, options.getSelect(), options.xml10InvalidCharReplacement(), writer, writeOnlyRef, name, (Set) null);
                }

                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
                writer.close();
                outputStream.close();
                var12 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
            } catch (XMLStreamException var17) {
                cachedException = new SerializerException("An I/O exception occurred.", var17, MessageKeys.IO_EXCEPTION, new String[0]);
                throw cachedException;
            } catch (IOException var18) {
                cachedException = new SerializerException("An I/O exception occurred.", var18, MessageKeys.IO_EXCEPTION, new String[0]);
                throw cachedException;
            } finally {
                this.closeCircleStreamBufferOutput(outputStream, cachedException);
            }

            return var12;
        }
    }

    public void entityCollectionIntoStream(ServiceMetadata metadata, EdmEntityType entityType, EntityIterator entitySet, EntityCollectionSerializerOptions options, OutputStream outputStream) throws SerializerException {
        ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
        String name = contextURL == null ? null : contextURL.getEntitySetOrSingletonOrType();

        try {
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("a", "feed", "http://www.w3.org/2005/Atom");
            writer.writeNamespace("a", "http://www.w3.org/2005/Atom");
            writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeNamespace("d", "http://docs.oasis-open.org/odata/ns/data");
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
            this.writeMetadataETag(metadata, writer);
            if (options != null && options.getId() != null) {
                writer.writeStartElement("a", "id", "http://www.w3.org/2005/Atom");
                writer.writeCharacters(options.getId());
                writer.writeEndElement();
            }

            boolean writeOnlyRef = options != null && options.getWriteOnlyReferences();
            if (options == null) {
                this.writeEntitySet(metadata, entityType, entitySet, (ExpandOption) null, (Integer) null, (SelectOption) null, (String) null, writer, writeOnlyRef, name, (Set) null);
            } else {
                this.writeEntitySet(metadata, entityType, entitySet, options.getExpand(), (Integer) null, options.getSelect(), options.xml10InvalidCharReplacement(), writer, writeOnlyRef, name, (Set) null);
            }

            if (options != null && options.getCount() != null && options.getCount().getValue() && entitySet.getCount() != null) {
                this.writeCount(entitySet, writer);
            }

            if (entitySet != null && entitySet.getNext() != null) {
                this.writeNextLink(entitySet, writer);
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
        } catch (XMLStreamException var11) {
            SerializerException cachedException = new SerializerException("An I/O exception occurred.", var11, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        }
    }

    public SerializerStreamResult entityCollectionStreamed(ServiceMetadata metadata, EdmEntityType entityType, EntityIterator entities, EntityCollectionSerializerOptions options) throws SerializerException {
        return ODataWritableContent.with(entities, entityType, this, metadata, options).build();
    }

    public SerializerResult entity(ServiceMetadata metadata, EdmEntityType entityType, Entity entity, EntitySerializerOptions options) throws SerializerException {
        ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
        String name = contextURL == null ? null : contextURL.getEntitySetOrSingletonOrType();
        if (options != null && options.getWriteOnlyReferences()) {
            return this.entityReference(entity, ReferenceSerializerOptions.with().contextURL(contextURL).build());
        } else {
            OutputStream outputStream = null;
            SerializerException cachedException = null;

            SerializerResult var11;
            try {
                CircleStreamBuffer buffer = new CircleStreamBuffer();
                outputStream = buffer.getOutputStream();
                XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
                writer.writeStartDocument("UTF-8", "1.0");
                this.writeEntity(metadata, entityType, entity, contextURL, options == null ? null : options.getExpand(), (Integer) null, options == null ? null : options.getSelect(), options == null ? null : options.xml10InvalidCharReplacement(), writer, true, false, name, (Set) null);
                writer.writeEndDocument();
                writer.flush();
                writer.close();
                outputStream.close();
                var11 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
            } catch (XMLStreamException var16) {
                cachedException = new SerializerException("An I/O exception occurred.", var16, MessageKeys.IO_EXCEPTION, new String[0]);
                throw cachedException;
            } catch (IOException var17) {
                cachedException = new SerializerException("An I/O exception occurred.", var17, MessageKeys.IO_EXCEPTION, new String[0]);
                throw cachedException;
            } finally {
                this.closeCircleStreamBufferOutput(outputStream, cachedException);
            }

            return var11;
        }
    }

    private ContextURL checkContextURL(ContextURL contextURL) throws SerializerException {
        if (contextURL == null) {
            throw new SerializerException("ContextURL null!", MessageKeys.NO_CONTEXT_URL, new String[0]);
        } else {
            return contextURL;
        }
    }

    private void writeMetadataETag(ServiceMetadata metadata, XMLStreamWriter writer) throws XMLStreamException {
        if (metadata != null && metadata.getServiceMetadataETagSupport() != null && metadata.getServiceMetadataETagSupport().getMetadataETag() != null) {
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "metadata-etag", metadata.getServiceMetadataETagSupport().getMetadataETag());
        }

    }

    protected void writeEntitySet(ServiceMetadata metadata, EdmEntityType entityType, AbstractEntityCollection entitySet, ExpandOption expand, Integer toDepth, SelectOption select, String xml10InvalidCharReplacement, XMLStreamWriter writer, boolean writeOnlyRef, String name, Set<String> ancestors) throws XMLStreamException, SerializerException {
        Iterator var12 = entitySet.iterator();

        while (var12.hasNext()) {
            Entity entity = (Entity) var12.next();
            this.writeEntity(metadata, entityType, entity, (ContextURL) null, expand, toDepth, select, xml10InvalidCharReplacement, writer, false, writeOnlyRef, name, ancestors);
        }

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

    protected void writeEntity(ServiceMetadata metadata, EdmEntityType entityType, Entity entity, ContextURL contextURL, ExpandOption expand, Integer toDepth, SelectOption select, String xml10InvalidCharReplacement, XMLStreamWriter writer, boolean top, boolean writeOnlyRef, String name, Set<String> ancestors) throws XMLStreamException, SerializerException {
        boolean cycle = false;
        if (expand != null) {
            if (ancestors == null) {
                ancestors = new HashSet();
            }

            cycle = !((Set) ancestors).add(this.getEntityId(entity, entityType, name));
        }

        if (!cycle && !writeOnlyRef) {
            try {
                writer.writeStartElement("a", "entry", "http://www.w3.org/2005/Atom");
                if (top) {
                    writer.writeNamespace("a", "http://www.w3.org/2005/Atom");
                    writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
                    writer.writeNamespace("d", "http://docs.oasis-open.org/odata/ns/data");
                    if (contextURL != null) {
                        writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
                        this.writeMetadataETag(metadata, writer);
                    }
                }

                if (entity.getETag() != null) {
                    writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "etag", entity.getETag());
                }

                if (entity.getId() != null) {
                    writer.writeStartElement("http://www.w3.org/2005/Atom", "id");
                    writer.writeCharacters(entity.getId().toASCIIString());
                    writer.writeEndElement();
                }

                this.writerAuthorInfo(entity.getTitle(), writer);
                if (entity.getId() != null) {
                    writer.writeStartElement("http://www.w3.org/2005/Atom", "link");
                    writer.writeAttribute("rel", "edit");
                    writer.writeAttribute("href", entity.getId().toASCIIString());
                    writer.writeEndElement();
                }

                if (entityType.hasStream()) {
                    writer.writeStartElement("http://www.w3.org/2005/Atom", "content");
                    writer.writeAttribute("type", entity.getMediaContentType());
                    if (entity.getMediaContentSource() != null) {
                        writer.writeAttribute("src", entity.getMediaContentSource().toString());
                    } else {
                        String id = entity.getId().toASCIIString();
                        writer.writeAttribute("src", id + (id.endsWith("/") ? "" : "/") + "$value");
                    }

                    writer.writeEndElement();
                }

                Iterator var20 = entity.getMediaEditLinks().iterator();

                while (var20.hasNext()) {
                    Link link = (Link) var20.next();
                    this.writeLink(writer, link);
                }

                EdmEntityType resolvedType = this.resolveEntityType(metadata, entityType, entity.getType());
                this.writeNavigationProperties(metadata, resolvedType, entity, expand, toDepth, xml10InvalidCharReplacement, (Set) ancestors, name, writer);
                writer.writeStartElement("a", "category", "http://www.w3.org/2005/Atom");
                writer.writeAttribute("scheme", "http://docs.oasis-open.org/odata/ns/scheme");
                writer.writeAttribute("term", "#" + resolvedType.getFullQualifiedName().getFullQualifiedNameAsString());
                writer.writeEndElement();
                if (!entityType.hasStream()) {
                    writer.writeStartElement("http://www.w3.org/2005/Atom", "content");
                    writer.writeAttribute("type", "application/xml");
                }

                writer.writeStartElement("m", "properties", "http://docs.oasis-open.org/odata/ns/metadata");
                this.writeProperties(metadata, resolvedType, entity.getProperties(), select, xml10InvalidCharReplacement, writer);
                writer.writeEndElement();
                if (!entityType.hasStream()) {
                    writer.writeEndElement();
                }

                this.writeOperations(entity.getOperations(), writer);
                writer.writeEndElement();
            } finally {
                if (!cycle && ancestors != null) {
                    ((Set) ancestors).remove(this.getEntityId(entity, entityType, name));
                }

            }

        } else {
            this.writeReference(entity, contextURL, writer, top);
        }
    }

    private void writeOperations(List<Operation> operations, XMLStreamWriter writer) throws XMLStreamException {
        Iterator var3 = operations.iterator();

        while (var3.hasNext()) {
            Operation operation = (Operation) var3.next();
            boolean action = operation.getType() != null && operation.getType() == Type.ACTION;
            writer.writeStartElement("m", action ? "action" : "function", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeAttribute("metadata", operation.getMetadataAnchor());
            writer.writeAttribute("title", operation.getTitle());
            writer.writeAttribute("target", operation.getTarget().toASCIIString());
            writer.writeEndElement();
        }

    }

    private void writerAuthorInfo(String title, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("http://www.w3.org/2005/Atom", "title");
        if (title != null) {
            writer.writeCharacters(title);
        }

        writer.writeEndElement();
        writer.writeStartElement("http://www.w3.org/2005/Atom", "summary");
        writer.writeEndElement();
        writer.writeStartElement("http://www.w3.org/2005/Atom", "updated");
        writer.writeCharacters((new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(new Date(System.currentTimeMillis())));
        writer.writeEndElement();
        writer.writeStartElement("http://www.w3.org/2005/Atom", "author");
        writer.writeStartElement("http://www.w3.org/2005/Atom", "name");
        writer.writeEndElement();
        writer.writeEndElement();
    }

    protected EdmEntityType resolveEntityType(ServiceMetadata metadata, EdmEntityType baseType, String derivedTypeName) throws SerializerException {
        if (derivedTypeName != null && !baseType.getFullQualifiedName().getFullQualifiedNameAsString().equals(derivedTypeName)) {
            EdmEntityType derivedType = metadata.getEdm().getEntityType(new FullQualifiedName(derivedTypeName));
            if (derivedType == null) {
                throw new SerializerException("EntityType not found", MessageKeys.UNKNOWN_TYPE, new String[]{derivedTypeName});
            } else {
                for (EdmEntityType type = derivedType.getBaseType(); type != null; type = type.getBaseType()) {
                    if (type.getFullQualifiedName().getFullQualifiedNameAsString().equals(baseType.getFullQualifiedName().getFullQualifiedNameAsString())) {
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
        if (derivedTypeName != null && !baseType.getFullQualifiedName().getFullQualifiedNameAsString().equals(derivedTypeName)) {
            EdmComplexType derivedType = metadata.getEdm().getComplexType(new FullQualifiedName(derivedTypeName));
            if (derivedType == null) {
                throw new SerializerException("Complex Type not found", MessageKeys.UNKNOWN_TYPE, new String[]{derivedTypeName});
            } else {
                for (EdmComplexType type = derivedType.getBaseType(); type != null; type = type.getBaseType()) {
                    if (type.getFullQualifiedName().getFullQualifiedNameAsString().equals(baseType.getFullQualifiedName().getFullQualifiedNameAsString())) {
                        return derivedType;
                    }
                }

                throw new SerializerException("Wrong base type", MessageKeys.WRONG_BASE_TYPE, new String[]{derivedTypeName, baseType.getFullQualifiedName().getFullQualifiedNameAsString()});
            }
        } else {
            return baseType;
        }
    }

    protected void writeProperties(ServiceMetadata metadata, EdmStructuredType type, List<Property> properties, SelectOption select, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws XMLStreamException, SerializerException {
        boolean all = ExpandSelectHelper.isAll(select);
        Set<String> selected = all ? new HashSet() : ExpandSelectHelper.getSelectedPropertyNames(select.getSelectItems());
        Iterator var9 = type.getPropertyNames().iterator();

        while (true) {
            String propertyName;
            do {
                if (!var9.hasNext()) {
                    return;
                }

                propertyName = (String) var9.next();
            } while (!all && !((Set) selected).contains(propertyName));

            EdmProperty edmProperty = type.getStructuralProperty(propertyName);
            Property property = this.findProperty(propertyName, properties);
            Set<List<String>> selectedPaths = !all && !edmProperty.isPrimitive() ? ExpandSelectHelper.getSelectedPaths(select.getSelectItems(), propertyName) : null;
            this.writeProperty(metadata, edmProperty, property, selectedPaths, xml10InvalidCharReplacement, writer);
        }
    }

    protected void writeNavigationProperties(ServiceMetadata metadata, EdmStructuredType type, Linked linked, ExpandOption expand, Integer toDepth, String xml10InvalidCharReplacement, Set<String> ancestors, String name, XMLStreamWriter writer) throws SerializerException, XMLStreamException {
        Iterator var19;
        if ((toDepth == null || toDepth <= 1) && (toDepth != null || !ExpandSelectHelper.hasExpand(expand))) {
            var19 = type.getNavigationPropertyNames().iterator();

            while (var19.hasNext()) {
                String propertyName = (String) var19.next();
                this.writeLink(writer, this.getOrCreateLink(linked, propertyName));
            }
        } else {
            ExpandItem expandAll = ExpandSelectHelper.getExpandAll(expand);
            Iterator var11 = type.getNavigationPropertyNames().iterator();

            label90:
            while (true) {
                while (true) {
                    if (!var11.hasNext()) {
                        break label90;
                    }

                    String propertyName = (String) var11.next();
                    ExpandItem innerOptions = ExpandSelectHelper.getExpandItem(expand.getExpandItems(), propertyName);
                    if (expandAll == null && innerOptions == null && toDepth == null) {
                        this.writeLink(writer, this.getOrCreateLink(linked, propertyName));
                    } else {
                        Integer levels = null;
                        EdmNavigationProperty property = type.getNavigationProperty(propertyName);
                        Link navigationLink = this.getOrCreateLink(linked, propertyName);
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

                        this.writeLink(writer, navigationLink, false);
                        writer.writeStartElement("m", "inline", "http://docs.oasis-open.org/odata/ns/metadata");
                        this.writeExpandedNavigationProperty(metadata, property, navigationLink, (ExpandOption) childExpand, levels, innerOptions == null ? null : innerOptions.getSelectOption(), innerOptions == null ? null : innerOptions.getCountOption(), innerOptions == null ? false : innerOptions.hasCountPath(), innerOptions == null ? false : innerOptions.isRef(), xml10InvalidCharReplacement, ancestors, name, writer);
                        writer.writeEndElement();
                        writer.writeEndElement();
                    }
                }
            }
        }

        var19 = linked.getAssociationLinks().iterator();

        while (var19.hasNext()) {
            Link link = (Link) var19.next();
            this.writeLink(writer, link);
        }

    }

    protected Link getOrCreateLink(Linked linked, String navigationPropertyName) throws XMLStreamException {
        Link link = linked.getNavigationLink(navigationPropertyName);
        if (link == null) {
            link = new Link();
            link.setRel("http://docs.oasis-open.org/odata/ns/related/" + navigationPropertyName);
            link.setType(Constants.ENTITY_SET_NAVIGATION_LINK_TYPE);
            link.setTitle(navigationPropertyName);
            EntityCollection target = new EntityCollection();
            link.setInlineEntitySet(target);
            if (linked.getId() != null) {
                link.setHref(linked.getId().toASCIIString() + "/" + navigationPropertyName);
            }
        }

        return link;
    }

    private void writeLink(XMLStreamWriter writer, Link link) throws XMLStreamException {
        this.writeLink(writer, link, true);
    }

    private void writeLink(XMLStreamWriter writer, Link link, boolean close) throws XMLStreamException {
        writer.writeStartElement("a", "link", "http://www.w3.org/2005/Atom");
        writer.writeAttribute("rel", link.getRel());
        if (link.getType() != null) {
            writer.writeAttribute("type", link.getType());
        }

        if (link.getTitle() != null) {
            writer.writeAttribute("title", link.getTitle());
        }

        if (link.getHref() != null) {
            writer.writeAttribute("href", link.getHref());
        }

        if (close) {
            writer.writeEndElement();
        }

    }

    protected void writeExpandedNavigationProperty(ServiceMetadata metadata, EdmNavigationProperty property, Link navigationLink, ExpandOption innerExpand, Integer toDepth, SelectOption innerSelect, CountOption coutOption, boolean writeNavigationCount, boolean writeOnlyRef, String xml10InvalidCharReplacement, Set<String> ancestors, String name, XMLStreamWriter writer) throws XMLStreamException, SerializerException {
        if (property.isCollection()) {
            if (navigationLink != null && navigationLink.getInlineEntitySet() != null) {
                writer.writeStartElement("a", "feed", "http://www.w3.org/2005/Atom");
                if (writeNavigationCount) {
                    this.writeCount(navigationLink.getInlineEntitySet(), writer);
                } else {
                    if (coutOption != null && coutOption.getValue()) {
                        this.writeCount(navigationLink.getInlineEntitySet(), writer);
                    }

                    this.writeEntitySet(metadata, property.getType(), navigationLink.getInlineEntitySet(), innerExpand, toDepth, innerSelect, xml10InvalidCharReplacement, writer, writeOnlyRef, name, ancestors);
                }

                writer.writeEndElement();
            }
        } else if (navigationLink != null && navigationLink.getInlineEntity() != null) {
            this.writeEntity(metadata, property.getType(), navigationLink.getInlineEntity(), (ContextURL) null, innerExpand, toDepth, innerSelect, xml10InvalidCharReplacement, writer, false, writeOnlyRef, name, ancestors);
        }

    }

    protected void writeProperty(ServiceMetadata metadata, EdmProperty edmProperty, Property property, Set<List<String>> selectedPaths, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws XMLStreamException, SerializerException {
        writer.writeStartElement("d", edmProperty.getName(), "http://docs.oasis-open.org/odata/ns/data");
        if (property != null && !property.isNull()) {
            this.writePropertyValue(metadata, edmProperty, property, selectedPaths, xml10InvalidCharReplacement, writer);
        } else {
            if (!edmProperty.isNullable()) {
                throw new SerializerException("Non-nullable property not present!", MessageKeys.MISSING_PROPERTY, new String[]{edmProperty.getName()});
            }

            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "null", "true");
        }

        writer.writeEndElement();
    }

    private String collectionType(EdmType type) {
        return "#Collection(" + type.getFullQualifiedName().getFullQualifiedNameAsString() + ")";
    }

    private String complexType(ServiceMetadata metadata, EdmComplexType baseType, String definedType) throws SerializerException {
        EdmComplexType type = this.resolveComplexType(metadata, baseType, definedType);
        return type.getFullQualifiedName().getFullQualifiedNameAsString();
    }

    private String derivedComplexType(EdmComplexType baseType, String definedType) throws SerializerException {
        String base = baseType.getFullQualifiedName().getFullQualifiedNameAsString();
        return base.equals(definedType) ? null : definedType;
    }

    private void writePropertyValue(ServiceMetadata metadata, EdmProperty edmProperty, Property property, Set<List<String>> selectedPaths, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws XMLStreamException, SerializerException {
        try {
            if (!edmProperty.isPrimitive() && edmProperty.getType().getKind() != EdmTypeKind.ENUM && edmProperty.getType().getKind() != EdmTypeKind.DEFINITION) {
                if (!property.isComplex()) {
                    throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{edmProperty.getName()});
                }

                if (edmProperty.isCollection()) {
                    writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", this.collectionType(edmProperty.getType()));
                    this.writeComplexCollection(metadata, (EdmComplexType) edmProperty.getType(), property, selectedPaths, xml10InvalidCharReplacement, writer);
                } else {
                    this.writeComplex(metadata, edmProperty, property, selectedPaths, xml10InvalidCharReplacement, writer);
                }
            } else if (edmProperty.isCollection()) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", edmProperty.isPrimitive() ? "#Collection(" + edmProperty.getType().getName() + ")" : this.collectionType(edmProperty.getType()));
                this.writePrimitiveCollection((EdmPrimitiveType) edmProperty.getType(), property, edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(), edmProperty.isUnicode(), xml10InvalidCharReplacement, writer);
            } else {
                this.writePrimitive((EdmPrimitiveType) edmProperty.getType(), property, edmProperty.isNullable(), edmProperty.getMaxLength(), edmProperty.getPrecision(), edmProperty.getScale(), edmProperty.isUnicode(), xml10InvalidCharReplacement, writer);
            }

        } catch (EdmPrimitiveTypeException var8) {
            throw new SerializerException("Wrong value for property!", var8, MessageKeys.WRONG_PROPERTY_VALUE, new String[]{edmProperty.getName(), property.getValue().toString()});
        }
    }

    private void writeComplex(ServiceMetadata metadata, EdmProperty edmProperty, Property property, Set<List<String>> selectedPaths, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws XMLStreamException, SerializerException {
        writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", "#" + this.complexType(metadata, (EdmComplexType) edmProperty.getType(), property.getType()));
        String derivedName = property.getType();
        EdmComplexType resolvedType = this.resolveComplexType(metadata, (EdmComplexType) edmProperty.getType(), derivedName);
        this.writeComplexValue(metadata, resolvedType, property.asComplex().getValue(), selectedPaths, xml10InvalidCharReplacement, writer);
    }

    private void writePrimitiveCollection(EdmPrimitiveType type, Property property, Boolean isNullable, Integer maxLength, Integer precision, Integer scale, Boolean isUnicode, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws XMLStreamException, EdmPrimitiveTypeException, SerializerException {
        Iterator var10 = property.asCollection().iterator();

        while (var10.hasNext()) {
            Object value = var10.next();
            writer.writeStartElement("m", "element", "http://docs.oasis-open.org/odata/ns/metadata");
            switch (property.getValueType()) {
                case COLLECTION_PRIMITIVE:
                case COLLECTION_ENUM:
                    this.writePrimitiveValue(type, value, isNullable, maxLength, precision, scale, isUnicode, xml10InvalidCharReplacement, writer);
                    writer.writeEndElement();
                    break;
                case COLLECTION_GEOSPATIAL:
                    throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{property.getName()});
                default:
                    throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{property.getName()});
            }
        }

    }

    private void writeComplexCollection(ServiceMetadata metadata, EdmComplexType type, Property property, Set<List<String>> selectedPaths, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws XMLStreamException, SerializerException {
        Iterator var8 = property.asCollection().iterator();

        while (var8.hasNext()) {
            Object value = var8.next();
            writer.writeStartElement("m", "element", "http://docs.oasis-open.org/odata/ns/metadata");
            String typeName = ((ComplexValue) value).getTypeName();
            String propertyType = typeName != null ? typeName : property.getType();
            if (this.derivedComplexType(type, propertyType) != null) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", propertyType);
            }

            EdmComplexType complexType;
            if (typeName != null && !propertyType.equals(type.getFullQualifiedName().getFullQualifiedNameAsString())) {
                complexType = metadata.getEdm().getComplexType(new FullQualifiedName(propertyType));
            } else {
                complexType = type;
            }

            switch (property.getValueType()) {
                case COLLECTION_COMPLEX:
                    this.writeComplexValue(metadata, complexType, ((ComplexValue) value).getValue(), selectedPaths, xml10InvalidCharReplacement, writer);
                    writer.writeEndElement();
                    break;
                default:
                    throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{property.getName()});
            }
        }

    }

    private void writePrimitive(EdmPrimitiveType type, Property property, Boolean isNullable, Integer maxLength, Integer precision, Integer scale, Boolean isUnicode, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws EdmPrimitiveTypeException, XMLStreamException, SerializerException {
        if (property.isPrimitive()) {
            if (type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.String)) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", type.getKind() == EdmTypeKind.DEFINITION ? "#" + type.getFullQualifiedName().getFullQualifiedNameAsString() : type.getName());
            }

            this.writePrimitiveValue(type, property.asPrimitive(), isNullable, maxLength, precision, scale, isUnicode, xml10InvalidCharReplacement, writer);
        } else {
            if (property.isGeospatial()) {
                throw new SerializerException("Property type not yet supported!", MessageKeys.UNSUPPORTED_PROPERTY_TYPE, new String[]{property.getName()});
            }

            if (!property.isEnum()) {
                throw new SerializerException("Inconsistent property type!", MessageKeys.INCONSISTENT_PROPERTY_TYPE, new String[]{property.getName()});
            }

            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", "#" + type.getFullQualifiedName().getFullQualifiedNameAsString());
            this.writePrimitiveValue(type, property.asEnum(), isNullable, maxLength, precision, scale, isUnicode, xml10InvalidCharReplacement, writer);
        }

    }

    protected void writePrimitiveValue(EdmPrimitiveType type, Object primitiveValue, Boolean isNullable, Integer maxLength, Integer precision, Integer scale, Boolean isUnicode, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws EdmPrimitiveTypeException, XMLStreamException {
        String value = type.valueToString(primitiveValue, isNullable, maxLength, precision, scale, isUnicode);
        if (value == null) {
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "null", "true");
        } else {
            writer.writeCharacters(replaceInvalidCharacters(type, value, isUnicode, xml10InvalidCharReplacement));
        }

    }

    protected void writeComplexValue(ServiceMetadata metadata, EdmComplexType type, List<Property> properties, Set<List<String>> selectedPaths, String xml10InvalidCharReplacement, XMLStreamWriter writer) throws XMLStreamException, SerializerException {
        Iterator var7 = type.getPropertyNames().iterator();

        while (true) {
            String propertyName;
            Property property;
            do {
                if (!var7.hasNext()) {
                    return;
                }

                propertyName = (String) var7.next();
                property = this.findProperty(propertyName, properties);
            } while (selectedPaths != null && !ExpandSelectHelper.isSelected(selectedPaths, propertyName));

            this.writeProperty(metadata, (EdmProperty) type.getProperty(propertyName), property, selectedPaths == null ? null : ExpandSelectHelper.getReducedSelectedPaths(selectedPaths, propertyName), xml10InvalidCharReplacement, writer);
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
        ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var10;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("m", "value", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
            if (contextURL != null) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
            }

            this.writeMetadataETag(metadata, writer);
            if (property.isNull()) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "null", "true");
            } else {
                this.writePrimitive(type, property, options == null ? null : options.isNullable(), options == null ? null : options.getMaxLength(), options == null ? null : options.getPrecision(), options == null ? null : options.getScale(), options == null ? null : options.isUnicode(), options == null ? null : options.xml10InvalidCharReplacement(), writer);
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            outputStream.close();
            var10 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (XMLStreamException var16) {
            cachedException = new SerializerException("An I/O exception occurred.", var16, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (EdmPrimitiveTypeException var17) {
            cachedException = new SerializerException("Wrong value for property!", var17, MessageKeys.WRONG_PROPERTY_VALUE, new String[]{property.getName(), property.getValue().toString()});
            throw cachedException;
        } catch (IOException var18) {
            cachedException = new SerializerException("An I/O exception occurred.", var18, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var10;
    }

    public SerializerResult complex(ServiceMetadata metadata, EdmComplexType type, Property property, ComplexSerializerOptions options) throws SerializerException {
        ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var19;
        try {
            EdmComplexType resolvedType = null;
            if (!type.getFullQualifiedName().getFullQualifiedNameAsString().equals(property.getType())) {
                if (type.getBaseType() != null && type.getBaseType().getFullQualifiedName().getFullQualifiedNameAsString().equals(property.getType())) {
                    resolvedType = this.resolveComplexType(metadata, type.getBaseType(), type.getFullQualifiedName().getFullQualifiedNameAsString());
                } else {
                    resolvedType = this.resolveComplexType(metadata, type, property.getType());
                }
            } else {
                resolvedType = this.resolveComplexType(metadata, type, property.getType());
            }

            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("m", "value", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeNamespace("d", "http://docs.oasis-open.org/odata/ns/data");
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", "#" + resolvedType.getFullQualifiedName().getFullQualifiedNameAsString());
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
            this.writeMetadataETag(metadata, writer);
            if (property.isNull()) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "null", "true");
            } else {
                List<Property> values = property.asComplex().getValue();
                this.writeProperties(metadata, resolvedType, values, options == null ? null : options.getSelect(), options == null ? null : options.xml10InvalidCharReplacement(), writer);
            }

            writer.writeEndDocument();
            writer.flush();
            writer.close();
            outputStream.close();
            var19 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (XMLStreamException var16) {
            cachedException = new SerializerException("An I/O exception occurred.", var16, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (IOException var17) {
            cachedException = new SerializerException("An I/O exception occurred.", var17, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var19;
    }

    public SerializerResult primitiveCollection(ServiceMetadata metadata, EdmPrimitiveType type, Property property, PrimitiveSerializerOptions options) throws SerializerException {
        ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var10;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("m", "value", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
            if (contextURL != null) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
            }

            this.writeMetadataETag(metadata, writer);
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", "#Collection(" + type.getName() + ")");
            this.writePrimitiveCollection(type, property, options == null ? null : options.isNullable(), options == null ? null : options.getMaxLength(), options == null ? null : options.getPrecision(), options == null ? null : options.getScale(), options == null ? null : options.isUnicode(), options == null ? null : options.xml10InvalidCharReplacement(), writer);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            outputStream.close();
            var10 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (XMLStreamException var16) {
            cachedException = new SerializerException("An I/O exception occurred.", var16, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (EdmPrimitiveTypeException var17) {
            cachedException = new SerializerException("Wrong value for property!", var17, MessageKeys.WRONG_PROPERTY_VALUE, new String[]{property.getName(), property.getValue().toString()});
            throw cachedException;
        } catch (IOException var18) {
            cachedException = new SerializerException("An I/O exception occurred.", var18, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var10;
    }

    public SerializerResult complexCollection(ServiceMetadata metadata, EdmComplexType type, Property property, ComplexSerializerOptions options) throws SerializerException {
        ContextURL contextURL = this.checkContextURL(options == null ? null : options.getContextURL());
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var10;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("m", "value", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
            writer.writeNamespace("d", "http://docs.oasis-open.org/odata/ns/data");
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "type", this.collectionType(type));
            writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
            this.writeMetadataETag(metadata, writer);
            this.writeComplexCollection(metadata, type, property, (Set) null, options == null ? null : options.xml10InvalidCharReplacement(), writer);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            outputStream.close();
            var10 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (XMLStreamException var15) {
            cachedException = new SerializerException("An I/O exception occurred.", var15, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (IOException var16) {
            cachedException = new SerializerException("An I/O exception occurred.", var16, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var10;
    }

    public SerializerResult reference(ServiceMetadata metadata, EdmEntitySet edmEntitySet, Entity entity, ReferenceSerializerOptions options) throws SerializerException {
        return this.entityReference(entity, options);
    }

    protected SerializerResult entityReference(Entity entity, ReferenceSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        SerializerResult var7;
        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            this.writeReference(entity, options == null ? null : options.getContextURL(), writer, true);
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            outputStream.close();
            var7 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
        } catch (XMLStreamException var12) {
            cachedException = new SerializerException("An I/O exception occurred.", var12, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (IOException var13) {
            cachedException = new SerializerException("An I/O exception occurred.", var13, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }

        return var7;
    }

    private void writeReference(Entity entity, ContextURL contextURL, XMLStreamWriter writer, boolean top) throws XMLStreamException {
        writer.writeStartElement("m", "ref", "http://docs.oasis-open.org/odata/ns/metadata");
        if (top) {
            writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
            if (contextURL != null) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(contextURL).toASCIIString());
            }
        }

        writer.writeAttribute("id", entity.getId().toASCIIString());
        writer.writeEndElement();
    }

    public SerializerResult referenceCollection(ServiceMetadata metadata, EdmEntitySet edmEntitySet, AbstractEntityCollection entityCollection, ReferenceCollectionSerializerOptions options) throws SerializerException {
        return this.entityReferenceCollection(entityCollection, options);
    }

    protected SerializerResult entityReferenceCollection(AbstractEntityCollection entitySet, ReferenceCollectionSerializerOptions options) throws SerializerException {
        OutputStream outputStream = null;
        SerializerException cachedException = null;

        try {
            CircleStreamBuffer buffer = new CircleStreamBuffer();
            outputStream = buffer.getOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeStartElement("a", "feed", "http://www.w3.org/2005/Atom");
            writer.writeNamespace("a", "http://www.w3.org/2005/Atom");
            writer.writeNamespace("m", "http://docs.oasis-open.org/odata/ns/metadata");
            if (options != null && options.getContextURL() != null) {
                writer.writeAttribute("m", "http://docs.oasis-open.org/odata/ns/metadata", "context", ContextURLBuilder.create(options.getContextURL()).toASCIIString());
            }

            if (options != null && options.getCount() != null && options.getCount().getValue() && entitySet.getCount() != null) {
                this.writeCount(entitySet, writer);
            }

            if (entitySet.getNext() != null) {
                this.writeNextLink(entitySet, writer);
            }

            Iterator var7 = entitySet.iterator();

            while (var7.hasNext()) {
                Entity entity = (Entity) var7.next();
                this.writeReference(entity, options == null ? null : options.getContextURL(), writer, false);
            }

            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();
            outputStream.close();
            SerializerResult var16 = SerializerResultImpl.with().content(buffer.getInputStream()).build();
            return var16;
        } catch (XMLStreamException var13) {
            cachedException = new SerializerException("An I/O exception occurred.", var13, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } catch (IOException var14) {
            cachedException = new SerializerException("An I/O exception occurred.", var14, MessageKeys.IO_EXCEPTION, new String[0]);
            throw cachedException;
        } finally {
            this.closeCircleStreamBufferOutput(outputStream, cachedException);
        }
    }

    private void writeCount(AbstractEntityCollection entitySet, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("m", "count", "http://docs.oasis-open.org/odata/ns/metadata");
        writer.writeCharacters(String.valueOf(entitySet.getCount() == null ? 0 : entitySet.getCount()));
        writer.writeEndElement();
    }

    private void writeNextLink(AbstractEntityCollection entitySet, XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("a", "link", "http://www.w3.org/2005/Atom");
        writer.writeAttribute("rel", "next");
        writer.writeAttribute("href", entitySet.getNext().toASCIIString());
        writer.writeEndElement();
    }
}
