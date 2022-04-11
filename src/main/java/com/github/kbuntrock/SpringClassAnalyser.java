package com.github.kbuntrock;

import com.github.kbuntrock.model.*;
import com.github.kbuntrock.utils.OpenApiDataType;
import com.github.kbuntrock.utils.ParameterLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringClassAnalyser {

    public Optional<Tag> getTagFromClass(Class clazz) throws MojoFailureException {
        Tag tag = new Tag(clazz.getSimpleName());
        String basePath = "";
        RequestMapping classRequestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
        if (classRequestMapping.value().length > 0) {
            basePath = classRequestMapping.value()[0];
        }

        parseEndpoints(tag, basePath, clazz);

        if (tag.getEndpoints().isEmpty()) {
            // There was not valid endpoint to attach to this tag. Therefore, we don't keep track of it.
            return Optional.empty();
        } else {
            return Optional.of(tag);
        }

    }

    private void parseEndpoints(Tag tag, String basePath, Class clazz) throws MojoFailureException {
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                RequestMapping requestMapping = null;
                Annotation realAnnotation = annotation;
                if (annotation.annotationType() == RequestMapping.class) {
                    requestMapping = (RequestMapping) annotation;
                } else if (annotation.annotationType().getAnnotation(RequestMapping.class) != null) {
                    requestMapping = annotation.annotationType().getAnnotation(RequestMapping.class);
                }
                if (requestMapping != null) {
                    Optional<Endpoint> optEndpoint = readRequestMapping(basePath, requestMapping, realAnnotation);
                    if (optEndpoint.isPresent()) {
                        Endpoint endpoint = optEndpoint.get();
                        endpoint.setName(method.getName());
                        endpoint.setParameters(readParameters(method));
                        endpoint.setResponseObject(readResponseObject(method));
                        tag.addEndpoint(endpoint);
                    }
                }

            }
        }
    }

    private static List<ParameterObject> readParameters(Method method) {
        List<ParameterObject> parameters = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
            if(parameter.getType().isAssignableFrom(HttpServletRequest.class)){
                continue;
            }

            ParameterObject paramObj = new ParameterObject();
            paramObj.setName(parameter.getName());

            ParameterizedType parameterizedType = null;
            Type genericReturnType = parameter.getParameterizedType();
            if(genericReturnType instanceof ParameterizedType){
                parameterizedType = (ParameterizedType) genericReturnType;
            }

            paramObj.setJavaType(parameter.getType(), parameterizedType);

            // Detect if is a path variable
            PathVariable pathAnnotation = parameter.getAnnotation(PathVariable.class);
            if (pathAnnotation != null) {
                paramObj.setLocation(ParameterLocation.PATH);
                paramObj.setRequired(pathAnnotation.required());
                if (!StringUtils.isEmpty(pathAnnotation.value())) {
                    paramObj.setName(pathAnnotation.name());
                }
            }

            // Detect if is a query variable
            RequestParam queryAnnotation = parameter.getAnnotation(RequestParam.class);
            if (queryAnnotation != null) {
                paramObj.setLocation(ParameterLocation.QUERY);
                paramObj.setRequired(queryAnnotation.required());
                if (!StringUtils.isEmpty(queryAnnotation.value())) {
                    paramObj.setName(queryAnnotation.name());
                }
            }

            // Detect if is a request body parameter
            RequestBody requestBodyAnnotation = parameter.getAnnotation(RequestBody.class);
            if (requestBodyAnnotation != null) {
                paramObj.setLocation(ParameterLocation.BODY);
                paramObj.setRequired(requestBodyAnnotation.required());
            }

            parameters.add(paramObj);
        }
        return parameters;
    }

    private static DataObject readResponseObject(Method method) {
        Class<?> returnType = method.getReturnType();
        if (Void.class == returnType || Void.TYPE == returnType) {
            return null;
        }
        DataObject dataObject = new DataObject();
        ParameterizedType parameterizedType = null;
        Type genericReturnType = method.getGenericReturnType();
        if(genericReturnType instanceof ParameterizedType){
            parameterizedType = (ParameterizedType) genericReturnType;
        }
        dataObject.setJavaType(returnType, parameterizedType);
        return dataObject;
    }

    private static Optional<Endpoint> readRequestMapping(String basePath, RequestMapping requestMapping, Annotation realAnnotation) throws MojoFailureException {
        Optional<Operation> operation = requestMappingToOperation(requestMapping);
        if (operation.isEmpty()) {
            return Optional.empty();
        }
        Endpoint endpoint = new Endpoint();
        endpoint.setOperation(operation.get());
        endpoint.setPath(readEndpointPath(basePath, realAnnotation));
        return Optional.of(endpoint);
    }

    private static String readEndpointPath(String basePath, Annotation realAnnotation) throws MojoFailureException {
        Method methodValue = null;
        Method methodPath = null;
        try {
            methodPath = realAnnotation.annotationType().getMethod("path");
            methodValue = realAnnotation.annotationType().getMethod("value");
        } catch (NoSuchMethodException e) {
            throw new MojoFailureException("Method 'value' not found for " + realAnnotation.getClass().getSimpleName());
        }
        if (methodValue == null && methodPath == null) {
            return basePath;
        }
        try {
            String[] paths = (String[]) methodPath.invoke(realAnnotation);
            if (paths != null && paths.length > 0) {
                return basePath + paths[0];
            }
            String[] values = (String[]) methodValue.invoke(realAnnotation);
            if (values != null && values.length > 0) {
                return basePath + values[0];
            }
            return basePath;

        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new MojoFailureException("Method value cannot be invoked for " + realAnnotation.annotationType().getSimpleName());
        }


    }

    private static Optional<Operation> requestMappingToOperation(RequestMapping requestMapping) {
        if (requestMapping.method().length < 1) {
            return Optional.empty();
        }

        switch (requestMapping.method()[0]) {
            case GET:
                return Optional.of(Operation.GET);
            case PUT:
                return Optional.of(Operation.PUT);
            case HEAD:
                return Optional.of(Operation.HEAD);
            case POST:
                return Optional.of(Operation.POST);
            case PATCH:
                return Optional.of(Operation.PATCH);
            case DELETE:
                return Optional.of(Operation.DELETE);
            case TRACE:
                return Optional.of(Operation.TRACE);
            case OPTIONS:
                return Optional.of(Operation.OPTIONS);
            default:
                throw new RuntimeException("RequestMethod unknow : " + requestMapping.method()[0].name());
        }
    }
}