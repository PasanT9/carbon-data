package org.wso2.carbon.dataservices.core.odata.serializer;

import java.io.IOException;
import java.io.OutputStream;
//import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerException.MessageKeys;

public abstract class AbstractODataSerializer extends org.apache.olingo.server.core.serializer.AbstractODataSerializer implements ODataSerializer {
    protected static final String IO_EXCEPTION_TEXT = "An I/O exception occurred.";

    public AbstractODataSerializer() {
    }

    protected void closeCircleStreamBufferOutput(OutputStream outputStream, SerializerException cachedException) throws SerializerException {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException var4) {
                if (cachedException != null) {
                    throw cachedException;
                }

                throw new SerializerException("An I/O exception occurred.", var4, MessageKeys.IO_EXCEPTION, new String[0]);
            }
        }

    }
}
