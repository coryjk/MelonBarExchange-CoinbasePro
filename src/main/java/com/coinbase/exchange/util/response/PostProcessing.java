package com.coinbase.exchange.util.response;

import com.coinbase.exchange.model.response.PostProcessor;
import com.coinbase.exchange.util.Format;
import com.coinbase.exchange.util.Guard;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Static library of common implementations of {@link PostProcessor} and other utility methods.
 */
@Slf4j
public final class PostProcessing {

    /**
     * Convert response body content to an {@link Optional} containing {@link JsonNode}, where the value is
     * present if the content can be successfully parsed as a json object.
     *
     * @return {@link PostProcessor} that provides <code>Optional{@literal <}JsonNode{@literal >}</code>
     */
    public static PostProcessor<Optional<JsonNode>> asJson() {
        return response -> Optional.ofNullable(marshalJson(response.content()));
    }

    /**
     * Converts the response content into a {@link JsonNode} using <code>asJson()</code> first, then attempts
     * to extract the value as a string using parameter <code>key</code> as a key.
     *
     * <p> TODO: add support for depth > 1 json parsing with inputs like <code>getJsonValueAsString("key1.key2")</code>
     *
     * @param key Json key
     * @return String value corresponding to json key
     */
    public static PostProcessor<String> getJsonValueAsString(final String key) {
        return response -> asJson()
                .andThen(jsonNode -> jsonNode
                        .map(_jsonNode -> getJsonValue(_jsonNode, key))
                        .map(JsonNode::toString)
                        .filter(StringUtils::isNotEmpty)
                        // key does not exist in json node, or failed to extract due to other issues (failed parse)
                        .orElseThrow(() -> new RuntimeException(Format.format(
                                "Could not extract key [{}] from response content [{}]", key, jsonNode.get()))))
                .apply(response);
    }

    /**
     * Converts the response content from {@link JsonNode} to a pretty string for debugging/logging purposes.
     *
     * @return Pretty printed json string
     */
    public static PostProcessor<String> asPrettyJsonString() {
        return response -> asJson()
                .andThen(jsonNode -> jsonNode
                        .map(JsonNode::toPrettyString)
                        // empty result
                        .orElse("{}"))
                .apply(response);
    }

    /**
     * Marshals the input json string into a {@link JsonNode}. Performs "deep" json parsing.
     *
     * @param jsonString String in json format
     * @return {@link JsonNode}
     */
    private static JsonNode marshalJson(final String jsonString) {
        Guard.nonNull(jsonString);
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readTree(
                    objectMapper.getFactory().createParser(jsonString));
        } catch (IOException ioException) {
            log.error("Exception [{}] thrown while attempting to parse content as json: [{}]",
                    ioException.getClass().getName(), jsonString, ioException);
        }
        return null;
    }

    /**
     * Extracts value from {@link JsonNode} provided a path. If the input path refers to just a depth=1 key, then
     * the call defaults to {@link JsonNode#get(String)}.
     *
     * @param jsonNode {@link JsonNode}
     * @param path Path to value
     * @return {@link JsonNode} at the specified path, or {@link com.fasterxml.jackson.databind.node.MissingNode} if
     *  input path is invalid
     */
    private static JsonNode getJsonValue(final JsonNode jsonNode, final String path) {
        return StringUtils.contains(path, JsonPointer.SEPARATOR)
                ? jsonNode.at(path)
                : jsonNode.get(path);
    }
}
