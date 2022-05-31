package org.wso2.carbon.dataservices.core.odata.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.server.api.ODataContent;
import org.apache.olingo.server.api.ODataContentWriteErrorCallback;
import org.apache.olingo.server.api.ODataContentWriteErrorContext;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
//import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerStreamResult;
import org.apache.olingo.server.core.serializer.SerializerStreamResultImpl;

public class ODataWritableContent implements ODataContent {
    private StreamContent streamContent;

    private ODataWritableContent(StreamContent streamContent) {
        this.streamContent = streamContent;
    }

    public static ODataWritableContentBuilder with(EntityIterator iterator, EdmEntityType entityType, ODataSerializer serializer, ServiceMetadata metadata, EntityCollectionSerializerOptions options) {
        return new ODataWritableContentBuilder(iterator, entityType, serializer, metadata, options);
    }

    public void write(WritableByteChannel writeChannel) {
        this.streamContent.write(Channels.newOutputStream(writeChannel));
    }

    public void write(OutputStream stream) {
        this.write(Channels.newChannel(stream));
    }

    public static class ODataWritableContentBuilder {
        private ODataSerializer serializer;
        private EntityIterator entities;
        private ServiceMetadata metadata;
        private EdmEntityType entityType;
        private EntityCollectionSerializerOptions options;

        public ODataWritableContentBuilder(EntityIterator entities, EdmEntityType entityType, ODataSerializer serializer, ServiceMetadata metadata, EntityCollectionSerializerOptions options) {
            this.entities = entities;
            this.entityType = entityType;
            this.serializer = serializer;
            this.metadata = metadata;
            this.options = options;
        }

        public ODataContent buildContent() {
            if (this.serializer instanceof ODataJsonSerializer) {
                StreamContent input = new StreamContentForJson(this.entities, this.entityType, (ODataJsonSerializer) this.serializer, this.metadata, this.options);
                return new ODataWritableContent(input);
            } else if (this.serializer instanceof ODataXmlSerializer) {
                StreamContentForXml input = new StreamContentForXml(this.entities, this.entityType, (ODataXmlSerializer) this.serializer, this.metadata, this.options);
                return new ODataWritableContent(input);
            } else {
                throw new ODataRuntimeException("No suitable serializer found");
            }
        }

        public SerializerStreamResult build() {
            return SerializerStreamResultImpl.with().content(this.buildContent()).build();
        }
    }

    public static class WriteErrorContext implements ODataContentWriteErrorContext {
        private ODataLibraryException exception;

        public WriteErrorContext(ODataLibraryException exception) {
            this.exception = exception;
        }

        public Exception getException() {
            return this.exception;
        }

        public ODataLibraryException getODataLibraryException() {
            return this.exception;
        }
    }

    private static class StreamContentForXml extends StreamContent {
        private ODataXmlSerializer xmlSerializer;

        public StreamContentForXml(EntityIterator iterator, EdmEntityType entityType, ODataXmlSerializer xmlSerializer, ServiceMetadata metadata, EntityCollectionSerializerOptions options) {
            super(iterator, entityType, metadata, options);
            this.xmlSerializer = xmlSerializer;
        }

        protected void writeEntity(EntityIterator entity, OutputStream outputStream) throws SerializerException {
            try {
                this.xmlSerializer.entityCollectionIntoStream(this.metadata, this.entityType, entity, this.options, outputStream);
                outputStream.flush();
            } catch (IOException var4) {
                throw new ODataRuntimeException("Failed entity serialization", var4);
            }
        }
    }

    private static class StreamContentForJson extends StreamContent {
        private ODataJsonSerializer jsonSerializer;

        public StreamContentForJson(EntityIterator iterator, EdmEntityType entityType, ODataJsonSerializer jsonSerializer, ServiceMetadata metadata, EntityCollectionSerializerOptions options) {
            super(iterator, entityType, metadata, options);
            this.jsonSerializer = jsonSerializer;
        }

        protected void writeEntity(EntityIterator entity, OutputStream outputStream) throws SerializerException {
            try {
                this.jsonSerializer.entityCollectionIntoStream(this.metadata, this.entityType, entity, this.options, outputStream);
                outputStream.flush();
            } catch (IOException var4) {
                throw new ODataRuntimeException("Failed entity serialization", var4);
            }
        }
    }

    private abstract static class StreamContent {
        protected EntityIterator iterator;
        protected ServiceMetadata metadata;
        protected EdmEntityType entityType;
        protected EntityCollectionSerializerOptions options;

        public StreamContent(EntityIterator iterator, EdmEntityType entityType, ServiceMetadata metadata, EntityCollectionSerializerOptions options) {
            this.iterator = iterator;
            this.entityType = entityType;
            this.metadata = metadata;
            this.options = options;
        }

        protected abstract void writeEntity(EntityIterator var1, OutputStream var2) throws SerializerException;

        public void write(OutputStream out) {
            try {
                this.writeEntity(this.iterator, out);
            } catch (SerializerException var5) {
                ODataContentWriteErrorCallback errorCallback = this.options.getODataContentWriteErrorCallback();
                if (errorCallback != null) {
                    WriteErrorContext errorContext = new WriteErrorContext(var5);
                    errorCallback.handleError(errorContext, Channels.newChannel(out));
                }
            }

        }
    }
}
