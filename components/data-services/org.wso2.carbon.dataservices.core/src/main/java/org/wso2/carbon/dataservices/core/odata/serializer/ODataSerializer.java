package org.wso2.carbon.dataservices.core.odata.serializer;

import org.apache.olingo.commons.api.data.AbstractEntityCollection;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.server.api.ODataServerError;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.*;

public interface ODataSerializer extends org.apache.olingo.server.api.serializer.ODataSerializer {
    String DEFAULT_CHARSET = "UTF-8";

    SerializerResult serviceDocument(ServiceMetadata var1, String var2) throws SerializerException;

    SerializerResult metadataDocument(ServiceMetadata var1) throws SerializerException;

    SerializerResult error(ODataServerError var1) throws SerializerException;

    SerializerResult entityCollection(ServiceMetadata var1, EdmEntityType var2, AbstractEntityCollection var3, EntityCollectionSerializerOptions var4) throws SerializerException;

    SerializerStreamResult entityCollectionStreamed(ServiceMetadata var1, EdmEntityType var2, EntityIterator var3, EntityCollectionSerializerOptions var4) throws SerializerException;

    SerializerResult entity(ServiceMetadata var1, EdmEntityType var2, Entity var3, EntitySerializerOptions var4) throws SerializerException;

    SerializerResult primitive(ServiceMetadata var1, EdmPrimitiveType var2, Property var3, PrimitiveSerializerOptions var4) throws SerializerException;

    SerializerResult complex(ServiceMetadata var1, EdmComplexType var2, Property var3, ComplexSerializerOptions var4) throws SerializerException;

    SerializerResult primitiveCollection(ServiceMetadata var1, EdmPrimitiveType var2, Property var3, PrimitiveSerializerOptions var4) throws SerializerException;

    SerializerResult complexCollection(ServiceMetadata var1, EdmComplexType var2, Property var3, ComplexSerializerOptions var4) throws SerializerException;

    SerializerResult reference(ServiceMetadata var1, EdmEntitySet var2, Entity var3, ReferenceSerializerOptions var4) throws SerializerException;

    SerializerResult referenceCollection(ServiceMetadata var1, EdmEntitySet var2, AbstractEntityCollection var3, ReferenceCollectionSerializerOptions var4) throws SerializerException;
}
