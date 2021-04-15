package com.coinbase.model.annotation.processor;

import com.coinbase.model.annotation.Api;
import com.coinbase.model.annotation.Request;
import com.coinbase.model.request.BaseRequest;
import com.coinbase.util.Format;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class representation of an API implementation with a base URI and related operations.
 * For example, provided an implementation for an accounts API with base URI <code>/accounts</code>,
 * the class have the annotation:
 * <p>
 * <pre>
 *     &#064Api("/accounts")
 *     public class AccountsApiImpl { ... }
 * </pre>
 * <p>
 * Furthermore, its implementations of HTTP requests will be denoted with {@link Request} annotations,
 * and accept 1 parameter that extends type {@link BaseRequest}. For example, for a <code>GET</code>
 * operation at absolute URI <code>/accounts/{id}/ledger</code>, where <code>{id}</code> denotes an input
 * account ID, the method annotation would be:
 * <p>
 * <pre>
 *     &#064Request(path = "/{id}/ledger", type = Http.GET)
 *     public &lt;T extends BaseRequest&gt; Response getAccountHistory(final T request) { ... }
 * </pre>
 */
@Getter
public class ApiAnnotatedClass {

    private final TypeElement annotatedClassElement;

    private final Map<Request, ExecutableElement> annotatedMethods;
    private final String baseUri;

    /**
     * Performs basic validation on the API base URI, and populates mapping of {@link Request} to
     * operation implementations in the form of {@link ExecutableElement}. Expects that all annotated
     * executable elements have unique {@link Request} annotations.
     *
     * @param classElement {@link TypeElement} for an API implementation.
     */
    public ApiAnnotatedClass(final TypeElement classElement) {
        this.annotatedClassElement = classElement;
        this.annotatedMethods = new LinkedHashMap<>();

        final Api classAnnotation = classElement.getAnnotation(Api.class);
        if (classAnnotation == null) {
            throw new IllegalArgumentException("Input class element " + classElement + " is not an annotated API.");
        }
        this.baseUri = classAnnotation.value();

        // invalid base URI
        if (!isValidUri(baseUri)) {
            throw new IllegalArgumentException(Format.format("Got invalid base URI from {}, uri: [{}]",
                    annotatedClassElement.getQualifiedName().toString(), baseUri));
        }

        // process methods
        for (final Element enclosedElement : classElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                continue;
            }
            final Request[] wrappedRequest = enclosedElement.getAnnotationsByType(Request.class);
            final Request request;

            // should only be annotated once with @Request
            if (wrappedRequest.length != 1) {
                continue;
            }

            // should not annotate multiple methods with the same @Request annotation
            request = wrappedRequest[0];
            if (annotatedMethods.containsKey(request)) {
                throw new IllegalStateException(Format.format("Found duplicate method with the same request "
                        + "annotation: [{}], method stored: [{}], method found: [{}]", request.toString(),
                        annotatedMethods.get(request), enclosedElement));
            }

            // store
            annotatedMethods.put(request, (ExecutableElement) enclosedElement);
        }
    }

    private boolean isValidUri(final String uriFormat) {
        return StringUtils.isNotEmpty(uriFormat)
                && uriFormat.charAt(0) == '/'
                && uriFormat.charAt(uriFormat.length()-1) != '/';
    }
}
