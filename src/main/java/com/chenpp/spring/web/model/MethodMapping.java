package com.chenpp.spring.web.model;

import com.chenpp.spring.web.annotation.CPRequestParam;
import com.chenpp.spring.web.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class MethodMapping {
	private  Method method;
	private Object controller;
	protected Map<String,Integer> paramIndexMapping;	//参数顺序

	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public Object getController() {
		return controller;
	}
	public void setController(Object controller) {
		this.controller = controller;
	}

	public Map<String, Integer> getParamIndexMapping() {
		return paramIndexMapping;
	}

	public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
		this.paramIndexMapping = paramIndexMapping;
	}
	public MethodMapping(Object controller, Method method){
		this.controller = controller;
		this.method = method;

		paramIndexMapping = new HashMap<String,Integer>();
		putParamIndexMapping(method);
	}

    /**
	 * 根据方法获取对应参数和下标的Mapping
	 *
	 * */
	private void putParamIndexMapping(Method method) {

		//遍历Method中的所有参数,获取其对应的参数名和下标
		Parameter[] params = method.getParameters();
		for(int i = 0 ; i < params.length ; i++){
			Class<?> type = params[i].getType();
			if(type == HttpServletRequest.class || type == HttpServletResponse.class){
				paramIndexMapping.put(type.getName(),i);
				continue;
			}
			Annotation[] annotations = params[i].getAnnotations();
			String paramName  = getAnnotationParamName(annotations);
			if(StringUtils.isEmpty(paramName)){
				paramName = params[i].getName();
			}
			paramIndexMapping.put(paramName,i);

		}
	}

	private String getAnnotationParamName(Annotation[] annotations){
		for(Annotation a : annotations) {
			if (a instanceof CPRequestParam) {
				return ((CPRequestParam) a).value();
			}
		}
		return "";
	}

}
