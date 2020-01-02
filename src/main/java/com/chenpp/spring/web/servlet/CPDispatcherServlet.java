package com.chenpp.spring.web.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.chenpp.spring.web.annotation.*;
import com.chenpp.spring.web.model.MethodMapping;
import com.chenpp.spring.web.util.Constants;
import com.chenpp.spring.web.util.StringUtils;

public class CPDispatcherServlet extends HttpServlet{

	//存储对应的配置文件信息(config.properties)
	private static ConcurrentHashMap<String, String> initParams = new  ConcurrentHashMap<String, String>();

	//ioc容器:实例化class
	private static ConcurrentHashMap<String, Object>  iocMap = new  ConcurrentHashMap<String, Object>();

	//存储url和对应的方法的Mapping关系
	private static ConcurrentHashMap<Pattern, MethodMapping>  urlMethodMap = new  ConcurrentHashMap<Pattern, MethodMapping>();

	//存储扫描到的包路径下class类
    private static List<Class<?>> classes = new ArrayList<>();

    @Override
	public void init(ServletConfig servletConfig) throws ServletException {
		//1.根据web.xml配置的init-param获取配置文件路径,读取扫包路径
		parseConfig(servletConfig);

		//2.根据扫包路径,递归获取到所有需要扫描的class[]
        doScanner(initParams.get(Constants.PACKAGE_SCANNING));

        //3.初始化@CPService和@CPController的beans,放入到IOC容器中
		initBeans(classes);

		//4.对使用了@CPAutowire注解的属性值进行依赖注入(反射机制)
		doDI();

		//5.遍历所有的@CPController的类和其上的方法,对url和method进行映射
		handlerMapping();

		System.out.println("spring 启动加载完成...........");
	}



	private void parseConfig(ServletConfig servletConfig) {
		String location = servletConfig.getInitParameter(Constants.CONTEXT_CONFIG_LOCATION);
		InputStream in = this.getClass().getClassLoader().getResourceAsStream(location);
		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			try {
				if(in != null){in.close();}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//遍历properties,保存到initParamMap里
		for(Object key:properties.keySet()){
			Object value = properties.get(key);
			initParams.put(key.toString(), value.toString());
		}
	}

	private void doScanner(String packageName) {
		//根据path遍历所有class文件
		URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/"));
		File dir = new File(url.getFile());
		try{
            for(File file:dir.listFiles()){
                if(file.isDirectory()){
                    //file是目录,递归
                    doScanner(packageName + "." + file.getName());
                }else{
                    //获取class文件路径和文件名
                    String className = packageName + "." + file.getName().replace(".class", "").trim();
                    Class<?> classFile  = Class.forName(className);
                    classes.add(classFile);
                }
            }
        }catch (Exception e){

        }

	}

	private void initBeans(List<Class<?>> classes){
    	try{
			//遍历所有的class文件,判断其上是否有CPController和CPService注解
			for(Class<?> clazz :classes){
				//对于CPController只需要存储beanName和对应bean的关系即可(一般不用于注入)
				if(clazz.isAnnotationPresent(CPController.class)){
					CPController controller = clazz.getAnnotation(CPController.class);
					String beanName = controller.value();
					//如果有定义属性值,以其为beanName,否则默认类名首字母小写
					if(StringUtils.isEmpty(beanName)){
						beanName = StringUtils.toFirstLowChar(clazz.getSimpleName());
					}
					iocMap.put(beanName, clazz.newInstance());
					continue;
				}else if(clazz.isAnnotationPresent(CPService.class)){
					//CPService注入有根据beanName和类型两种(接口类型)
					CPService service = clazz.getAnnotation(CPService.class);
					String beanName = service.value();
					//如果有定义属性值,以其为beanName,否则默认类名首字母小写
					if(StringUtils.isEmpty(beanName)){
						beanName = StringUtils.toFirstLowChar(clazz.getSimpleName());
						iocMap.put(beanName, clazz.newInstance());
					}
					//按照类型存储一个实例关系(为了方便按照类型注入)
					iocMap.put(clazz.getName(), clazz.newInstance());
					//按照接口的类型再存储一个实例关系(注入的时候方便按照接口类型来注入)
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> i : interfaces) {
						iocMap.put(i.getName(), clazz.newInstance());
					}
				}
				//TODO ...其他关于Component的注解就先不考虑
			}

		}
    	catch (Exception e){

		}

	}

	//执行依赖注入
	private void doDI()  {
		try{
			//遍历所有iocMap里的实例集合,判断其属性字段上是否有@CPAutowire注解
			for(Map.Entry<String,Object> entry:iocMap.entrySet()){
				Class<?> clazz = entry.getValue().getClass();
				Field[] fields = clazz.getDeclaredFields();
				for(Field field:fields){
					//CPAutowire默认使用byType的方式装配
					CPAutowire autoAnnotation = field.getAnnotation(CPAutowire.class);
					field.setAccessible(true);
					if(autoAnnotation != null ){
						CPQualifier qualifier = field.getAnnotation(CPQualifier.class);
						if(qualifier != null && !StringUtils.isEmpty(qualifier.value())){
							//按照名字注入
							field.set(entry.getValue(), iocMap.get(qualifier.value()));
							continue;
						}
						//否则按照类型注入
						field.set(entry.getValue(), iocMap.get(field.getType().getName()));
					}
				}
			}
		}catch (Exception e){

		}
	}

	private void handlerMapping() {
		for(Map.Entry<String,Object> entry:iocMap.entrySet()){
			Class<?> clazz = entry.getValue().getClass();
			//判断Controller类上是否有CPRequestMapping注解
			if(!clazz.isAnnotationPresent(CPRequestMapping.class)) continue;
			String baseUrl = clazz.getAnnotation(CPRequestMapping.class).value();
			Method[] methods = clazz.getDeclaredMethods();
			//遍历CPController上的Method 获取url与MethodMapping的映射关系
			for(Method method:methods){
				String methodUrl ="";
				if(method.isAnnotationPresent(CPRequestMapping.class)){
					methodUrl = method.getAnnotation(CPRequestMapping.class).value();
				}
				String regex = ("/"+baseUrl+methodUrl).replaceAll("/+", "/");
				Pattern pattern = Pattern.compile(regex);
				MethodMapping model = new MethodMapping(entry.getValue(),method);
				urlMethodMap.put(pattern, model);
			}
		}

	}
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try{
			this.doDispatcher(req, resp);
		}catch (Exception e){
			resp.getWriter().write("500 Exception,Details:\r\n" + Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]", "").replaceAll(",\\s", "\r\n"));

		}

	}

	private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		//获取实际的url请求
		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replace(contextPath, "").replaceAll("/+", "/");

		//根据url请求去获取响应的MethodBean对象
		MethodMapping methodMapping = null;
		for(Pattern pattern: urlMethodMap.keySet()){
			if(pattern.matcher(url).matches()){
				methodMapping = urlMethodMap.get(pattern);
			}
		}

		//如果找不到匹配的url,则直接返回404
		if(methodMapping == null){
			resp.getWriter().println("404 not found");
			resp.flushBuffer();
			return;
		}

		//获取方法的参数类型 列表
		Class<?> [] paramTypes = methodMapping.getMethod().getParameterTypes();
        //用于存储实际的参数列表
		Object [] paramValues = new Object[paramTypes.length];
        //获取请求的参数列表(Request请求里的参数都是字符串类型的，如果一个参数出现多次，那么它的value就是String数组)
		Map<String,String[]> params = req.getParameterMap();
		for (Map.Entry<String, String[]> param : params.entrySet()) {
			//将数组参数转化为string
			String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
			//如果找到匹配的参数名，则开始填充参数数组paramValues
			if(!methodMapping.getParamIndexMapping().containsKey(param.getKey())){continue;}
			int index = methodMapping.getParamIndexMapping().get(param.getKey());
			paramValues[index] = convert(paramTypes[index],value);
		}
		//设置方法中的request和response对象
		if(methodMapping.getParamIndexMapping().containsKey(HttpServletRequest.class.getName())){
			int reqIndex = methodMapping.getParamIndexMapping().get(HttpServletRequest.class.getName());
			paramValues[reqIndex] = req;
		}
		if(methodMapping.getParamIndexMapping().containsKey(HttpServletResponse.class.getName())) {
			int respIndex = methodMapping.getParamIndexMapping().get(HttpServletResponse.class.getName());
			paramValues[respIndex] = resp;
		}

		//执行方法获得返回值
		Object  returnValue = "";
		try {
			returnValue = methodMapping.getMethod().invoke(methodMapping.getController(),paramValues);
			//如果方法有加CPResponseBody注解,则直接返回结果 TODO Controller上也可加，这里就没考虑这种情形
			if(methodMapping.getMethod().isAnnotationPresent(CPResponseBody.class)){
				resp.getWriter().println(returnValue);
				return;
			}

			//否则根据配置文件里配置的视图进行转发
			req.getRequestDispatcher(initParams.get(Constants.PAGE_PREFIX)+returnValue+initParams.get(Constants.PAGE_SUFFIX)).forward(req, resp);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

    /**
	 * 转化参数类型，将String转化为实际的参数类型
	 * */
	private Object convert(Class<?> paramType, String value) {

		if( int.class == paramType || Integer.class == paramType){
			return Integer.valueOf(value);
		}
		if( double.class == paramType || Double.class == paramType){
			return Double.valueOf(value);
		}
		//TODO 这里只是列举了几种常用的,可以继续完善...
		return value;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		this.doPost(req,resp);
	}
}
