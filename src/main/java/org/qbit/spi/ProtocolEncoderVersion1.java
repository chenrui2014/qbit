package org.qbit.spi;

import org.boon.collections.MultiMap;
import org.boon.json.JsonSerializer;
import org.boon.json.JsonSerializerFactory;
import org.boon.primitive.CharBuf;
import org.qbit.message.MethodCall;
import org.qbit.service.Protocol;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.qbit.service.Protocol.*;

public class ProtocolEncoderVersion1 implements ProtocolEncoder {

    private static ThreadLocal<JsonSerializer> jsonSerializer = new ThreadLocal<JsonSerializer>(){
        @Override
        protected JsonSerializer initialValue() {
            return new JsonSerializerFactory().create();
        }
    };



    @Override
    public String encodeAsString(MethodCall<Object> methodCall) {
        CharBuf buf = CharBuf.createCharBuf();
        encodeAsString(buf, methodCall);
        return buf.toString();
    }

    @Override
    public String encodeAsString(List<MethodCall<Object>> methodCalls) {
        CharBuf buf = CharBuf.createCharBuf(1000);


        buf.addChar(PROTOCOL_MARKER);
        buf.addChar(PROTOCOL_VERSION_1_GROUP);



        for (MethodCall<Object> metthodCall : methodCalls) {
            encodeAsString(buf, metthodCall);
            buf.addChar(PROTOCOL_METHOD_SEPERATOR);
        }

        return buf.toString();

    }

    private void encodeAsString(CharBuf buf, MethodCall<Object> methodCall) {


        buf.addChar(PROTOCOL_MARKER);
        buf.addChar(PROTOCOL_VERSION_1);
        buf.addChar(PROTOCOL_SEPARATOR);


        buf.add(methodCall.id());
        buf.addChar(PROTOCOL_SEPARATOR);

        buf.add(methodCall.address());
        buf.addChar(PROTOCOL_SEPARATOR);


        buf.add(methodCall.returnAddress());
        buf.addChar(PROTOCOL_SEPARATOR);


        encodeHeadersAndParams(buf, methodCall.headers());
        buf.addChar(PROTOCOL_SEPARATOR);


        encodeHeadersAndParams(buf, methodCall.params());
        buf.addChar(PROTOCOL_SEPARATOR);


        buf.add(methodCall.objectName());
        buf.addChar(PROTOCOL_SEPARATOR);

        buf.add(methodCall.name());
        buf.addChar(PROTOCOL_SEPARATOR);


        buf.add(methodCall.timestamp());
        buf.addChar(PROTOCOL_SEPARATOR);


        final Object body = methodCall.body();



        final JsonSerializer serializer = jsonSerializer.get();
        if (body instanceof Iterable) {



            Iterable iter = (Iterable) body;


            for (Object bodyPart : iter) {

                serializer.serialize(buf, bodyPart);
                buf.addChar(PROTOCOL_ARG_SEPARATOR);
            }
        } else if (body!=null) {


            serializer.serialize(buf, body);
        }


    }

    private void encodeHeadersAndParams(CharBuf buf, MultiMap<String, String> headerOrParams) {

        if (headerOrParams == null) {
            return;
        }

        final Map<? extends String, ? extends Collection<String>> map = headerOrParams.baseMap();
        final Set<? extends Map.Entry<? extends String, ? extends Collection<String>>> entries = map.entrySet();
        for (Map.Entry<? extends String, ? extends Collection<String>> entry : entries) {

            final Collection<String> values = entry.getValue();

            if (values.size()==0) {
                continue;
            }


            buf.add(entry.getKey());
            buf.addChar(Protocol.PROTOCOL_KEY_HEADER_DELIM);

            for (String value : values) {
                buf.add(value);
                buf.addChar(Protocol.PROTOCOL_VALUE_HEADER_DELIM);
            }


            buf.addChar(Protocol.PROTOCOL_ENTRY_HEADER_DELIM);

        }


    }
}