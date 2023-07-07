package gloomyfolken.hooklib.helper.annotation;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

class AnnotationInvocationHandler implements Annotation, InvocationHandler {

    private final Class<? extends Annotation> annotationType;
    private final Map<String, Object> values;
    private final int hashCode;

    AnnotationInvocationHandler(Class<? extends Annotation> annotationType, Map<String, Object> values) {
        this.annotationType = annotationType;
        this.values = Collections.unmodifiableMap(normalize(annotationType, values));

        hashCode = HashCodeBuilder.reflectionHashCode(this, "hashCode");
    }

    static Map<String, Object> normalize(Class<? extends Annotation> annotationType, Map<String, Object> values) {
        Map<String, Object> valid = new HashMap<>();
        for (Method element : annotationType.getDeclaredMethods()) {
            String elementName = element.getName();
            if (values.containsKey(elementName)) {
                valid.put(elementName, values.get(elementName));
            } else if (element.getDefaultValue() != null) {
                valid.put(elementName, element.getDefaultValue());
            }
        }
        return valid;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (values.containsKey(method.getName()))
            return values.get(method.getName());
        return method.invoke(this, args);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    /**
     * Performs an equality check as described in {@link Annotation#equals(Object)}.
     *
     * @param other The object to compare
     * @return Whether the given object is equal to this annotation or not
     * @see Annotation#equals(Object)
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!annotationType.isInstance(other)) {
            return false;
        }

        Annotation that = annotationType.cast(other);

        //compare annotation member values
        for (Entry<String, Object> element : values.entrySet()) {
            Object value = element.getValue();
            Object otherValue;
            try {
                otherValue = that.annotationType().getMethod(element.getKey()).invoke(that);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }

            if (!Objects.deepEquals(value, otherValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the hash code of this annotation as described in {@link Annotation#hashCode()}.
     *
     * @return The hash code of this annotation.
     * @see Annotation#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append('@').append(annotationType.getName()).append('(');
        Set<String> sorted = new TreeSet<>(values.keySet());
        for (String elementName : sorted) {
            String value;
            if (values.get(elementName).getClass().isArray()) {
                value = Arrays.deepToString(new Object[]{values.get(elementName)})
                        .replaceAll("^\\[\\[", "[")
                        .replaceAll("]]$", "]");
            } else {
                value = values.get(elementName).toString();
            }
            result.append(elementName).append('=').append(value).append(", ");
        }
        // remove the trailing separator
        if (values.size() > 0) {
            result.delete(result.length() - 2, result.length());
        }
        result.append(")");

        return result.toString();
    }


}