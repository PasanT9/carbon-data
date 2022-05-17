package org.wso2.carbon.dataservices.core.odata.serializer;

import java.util.Collection;
import java.util.List;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
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
import org.apache.olingo.server.api.serializer.*;
//import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.uri.UriHelper;

public abstract class MyOData extends OData {
    private static final String IMPLEMENTATION = "org.apache.olingo.server.core.ODataImpl";

    public MyOData() {
    }

    public static OData newInstance() {
        try {
            Class<?> clazz = Class.forName("org.wso2.carbon.dataservices.core.odata.serializer.ODataImpl");
            Object object = clazz.newInstance();
            return (OData) object;
        } catch (Exception var2) {
            throw new ODataRuntimeException(var2);
        }
    }

    public static MyOData newMyInstance() {
        try {
            Class<?> clazz = Class.forName("org.wso2.carbon.dataservices.core.odata.serializer.MyODataImpl");
            Object object = clazz.newInstance();
            return (MyOData) object;
        } catch (Exception var2) {
            throw new ODataRuntimeException(var2);
        }
    }

    public abstract ODataSerializer createSerializer(ContentType var1) throws SerializerException;

    public abstract MyODataSerializer createMySerializer(ContentType var1) throws SerializerException;

    public abstract FixedFormatSerializer createFixedFormatSerializer();

    public abstract FixedFormatDeserializer createFixedFormatDeserializer();

    public abstract ODataHttpHandler createHandler(ServiceMetadata var1);

    public abstract ODataHandler createRawHandler(ServiceMetadata var1);

    public abstract ServiceMetadata createServiceMetadata(CsdlEdmProvider var1, List<EdmxReference> var2);

    public abstract ServiceMetadata createServiceMetadata(CsdlEdmProvider var1, List<EdmxReference> var2, ServiceMetadataETagSupport var3);

    public abstract UriHelper createUriHelper();

    public abstract ODataDeserializer createDeserializer(ContentType var1) throws DeserializerException;

    public abstract ODataDeserializer createDeserializer(ContentType var1, ServiceMetadata var2) throws DeserializerException;

    public abstract EdmPrimitiveType createPrimitiveTypeInstance(EdmPrimitiveTypeKind var1);

    public abstract ETagHelper createETagHelper();

    public abstract Preferences createPreferences(Collection<String> var1);

    public abstract DebugResponseHelper createDebugResponseHelper(String var1);

    public abstract EdmAssistedSerializer createEdmAssistedSerializer(ContentType var1) throws SerializerException;

    public abstract EdmDeltaSerializer createEdmDeltaSerializer(ContentType var1, List<String> var2) throws SerializerException;
}
