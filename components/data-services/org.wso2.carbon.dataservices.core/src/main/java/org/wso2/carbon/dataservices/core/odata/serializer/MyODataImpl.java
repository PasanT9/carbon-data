package org.wso2.carbon.dataservices.core.odata.serializer;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHandler;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.debug.DebugResponseHelper;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.FixedFormatDeserializer;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.etag.ETagHelper;
import org.apache.olingo.server.api.etag.ServiceMetadataETagSupport;
import org.apache.olingo.server.api.prefer.Preferences;
import org.apache.olingo.server.api.serializer.EdmAssistedSerializer;
import org.apache.olingo.server.api.serializer.EdmDeltaSerializer;
import org.apache.olingo.server.api.serializer.FixedFormatSerializer;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerException.MessageKeys;
import org.apache.olingo.server.api.uri.UriHelper;
import org.apache.olingo.server.core.ODataHandlerImpl;
import org.apache.olingo.server.core.ODataHttpHandlerImpl;
import org.apache.olingo.server.core.ServiceMetadataImpl;
import org.apache.olingo.server.core.debug.DebugResponseHelperImpl;
import org.apache.olingo.server.core.debug.ServerCoreDebugger;
import org.apache.olingo.server.core.deserializer.FixedFormatDeserializerImpl;
import org.apache.olingo.server.core.deserializer.json.ODataJsonDeserializer;
import org.apache.olingo.server.core.deserializer.xml.ODataXmlDeserializer;
import org.apache.olingo.server.core.etag.ETagHelperImpl;
import org.apache.olingo.server.core.prefer.PreferencesImpl;
import org.apache.olingo.server.core.serializer.FixedFormatSerializerImpl;
import org.apache.olingo.server.core.serializer.json.EdmAssistedJsonSerializer;
import org.apache.olingo.server.core.serializer.json.JsonDeltaSerializer;
import org.apache.olingo.server.core.serializer.json.JsonDeltaSerializerWithNavigations;
import org.apache.olingo.server.core.serializer.json.ODataJsonSerializer;
//import org.apache.olingo.server.core.serializer.xml.ODataXmlSerializer;
import org.apache.olingo.server.core.uri.UriHelperImpl;

public class MyODataImpl extends MyOData {
    public MyODataImpl() {
    }

    public MyODataSerializer createMySerializer(ContentType contentType) throws SerializerException {
        ODataSerializer serializer = null;
        if (contentType.isCompatible(ContentType.APPLICATION_JSON)) {
            String metadata = contentType.getParameter("odata.metadata");
            if (metadata == null || "minimal".equalsIgnoreCase(metadata) || "none".equalsIgnoreCase(metadata) || "full".equalsIgnoreCase(metadata)) {
                serializer = new MyODataJsonSerializer(contentType);
            }
        } else if (contentType.isCompatible(ContentType.APPLICATION_XML) || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML)) {
            serializer = new MyODataXmlSerializer();
        }

        if (serializer == null) {
            throw new SerializerException("Unsupported format: " + contentType.toContentTypeString(), MessageKeys.UNSUPPORTED_FORMAT, new String[]{contentType.toContentTypeString()});
        } else {
            return (MyODataSerializer)serializer;
        }
    }

    public ODataSerializer createSerializer(ContentType contentType) throws SerializerException {
        ODataSerializer serializer = null;
        if (contentType.isCompatible(ContentType.APPLICATION_JSON)) {
            String metadata = contentType.getParameter("odata.metadata");
            if (metadata == null || "minimal".equalsIgnoreCase(metadata) || "none".equalsIgnoreCase(metadata) || "full".equalsIgnoreCase(metadata)) {
                serializer = new ODataJsonSerializer(contentType);
            }
        } else if (contentType.isCompatible(ContentType.APPLICATION_XML) || contentType.isCompatible(ContentType.APPLICATION_ATOM_XML)) {
            serializer = new MyODataXmlSerializer();
        }

        if (serializer == null) {
            throw new SerializerException("Unsupported format: " + contentType.toContentTypeString(), MessageKeys.UNSUPPORTED_FORMAT, new String[]{contentType.toContentTypeString()});
        } else {
            return (ODataSerializer)serializer;
        }
    }

    public FixedFormatSerializer createFixedFormatSerializer() {
        return new FixedFormatSerializerImpl();
    }

    public EdmAssistedSerializer createEdmAssistedSerializer(ContentType contentType) throws SerializerException {
        if (contentType.isCompatible(ContentType.APPLICATION_JSON)) {
            return new EdmAssistedJsonSerializer(contentType);
        } else {
            throw new SerializerException("Unsupported format: " + contentType.toContentTypeString(), MessageKeys.UNSUPPORTED_FORMAT, new String[]{contentType.toContentTypeString()});
        }
    }

    public EdmDeltaSerializer createEdmDeltaSerializer(ContentType contentType, List<String> versions) throws SerializerException {
        if (contentType.isCompatible(ContentType.APPLICATION_JSON)) {
            if (versions != null && versions.size() > 0) {
                return (EdmDeltaSerializer)(this.getMaxVersion(versions) > 4.0F ? new JsonDeltaSerializerWithNavigations(contentType) : new JsonDeltaSerializer(contentType));
            } else {
                return new JsonDeltaSerializerWithNavigations(contentType);
            }
        } else {
            throw new SerializerException("Unsupported format: " + contentType.toContentTypeString(), MessageKeys.UNSUPPORTED_FORMAT, new String[]{contentType.toContentTypeString()});
        }
    }

    private float getMaxVersion(List<String> versions) {
        Float[] versionValue = new Float[versions.size()];
        int i = 0;
        Float max = new Float(0.0F);

        Float ver;
        for(Iterator var5 = versions.iterator(); var5.hasNext(); max = max > ver ? max : ver) {
            String version = (String)var5.next();
            ver = Float.valueOf(version);
            versionValue[i++] = ver;
        }

        return max;
    }

    public ODataHttpHandler createHandler(ServiceMetadata serviceMetadata) {
        return new ODataHttpHandlerImpl(this, serviceMetadata);
    }

    public ODataHandler createRawHandler(ServiceMetadata serviceMetadata) {
        return new ODataHandlerImpl(this, serviceMetadata, new ServerCoreDebugger(this));
    }

    public ServiceMetadata createServiceMetadata(CsdlEdmProvider edmProvider, List<EdmxReference> references) {
        return this.createServiceMetadata(edmProvider, references, (ServiceMetadataETagSupport)null);
    }

    public ServiceMetadata createServiceMetadata(CsdlEdmProvider edmProvider, List<EdmxReference> references, ServiceMetadataETagSupport serviceMetadataETagSupport) {
        return new ServiceMetadataImpl(edmProvider, references, serviceMetadataETagSupport);
    }

    public FixedFormatDeserializer createFixedFormatDeserializer() {
        return new FixedFormatDeserializerImpl();
    }

    public UriHelper createUriHelper() {
        return new UriHelperImpl();
    }

    public ODataDeserializer createDeserializer(ContentType contentType) throws DeserializerException {
        if (contentType.isCompatible(ContentType.JSON)) {
            return new ODataJsonDeserializer(contentType);
        } else if (!contentType.isCompatible(ContentType.APPLICATION_XML) && !contentType.isCompatible(ContentType.APPLICATION_ATOM_XML)) {
            throw new DeserializerException("Unsupported format: " + contentType.toContentTypeString(), org.apache.olingo.server.api.deserializer.DeserializerException.MessageKeys.UNSUPPORTED_FORMAT, new String[]{contentType.toContentTypeString()});
        } else {
            return new ODataXmlDeserializer();
        }
    }

    public ODataDeserializer createDeserializer(ContentType contentType, ServiceMetadata metadata) throws DeserializerException {
        if (contentType.isCompatible(ContentType.JSON)) {
            return new ODataJsonDeserializer(contentType, metadata);
        } else if (!contentType.isCompatible(ContentType.APPLICATION_XML) && !contentType.isCompatible(ContentType.APPLICATION_ATOM_XML)) {
            throw new DeserializerException("Unsupported format: " + contentType.toContentTypeString(), org.apache.olingo.server.api.deserializer.DeserializerException.MessageKeys.UNSUPPORTED_FORMAT, new String[]{contentType.toContentTypeString()});
        } else {
            return new ODataXmlDeserializer(metadata);
        }
    }

    public EdmPrimitiveType createPrimitiveTypeInstance(EdmPrimitiveTypeKind kind) {
        return EdmPrimitiveTypeFactory.getInstance(kind);
    }

    public ETagHelper createETagHelper() {
        return new ETagHelperImpl();
    }

    public Preferences createPreferences(Collection<String> preferHeaders) {
        return new PreferencesImpl(preferHeaders);
    }

    public DebugResponseHelper createDebugResponseHelper(String debugFormat) {
        return new DebugResponseHelperImpl(debugFormat);
    }
}
