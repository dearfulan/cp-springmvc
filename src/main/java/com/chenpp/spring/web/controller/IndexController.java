package com.chenpp.spring.web.controller;

import com.chenpp.spring.web.annotation.*;
import com.chenpp.spring.web.service.IndexService;
import com.chenpp.spring.web.service.impl.IndexServiceImpl;
import com.sun.deploy.net.HttpRequest;

import javax.servlet.http.HttpServletRequest;

@CPController
@CPRequestMapping("/cp")
public class IndexController {

	@CPAutowire
    private IndexService indexService;

	@CPRequestMapping("/page")
	public String index(){
		indexService.getName();
		return "index";
	}


	@CPRequestMapping("/add")
	@CPResponseBody
	public String add(HttpServletRequest request, @CPRequestParam("a") int a,@CPRequestParam("b") int b){
		return a + "+" + b + "=" +(a+b);
	}

	@CPRequestMapping("/getName")
	@CPResponseBody
	public String getName(String name){
		return "My Name is "+ name;
	}

	//这里使用的是贪婪匹配 表示匹配以/cp/query开头的url
	@CPRequestMapping("/query.*")
	@CPResponseBody
	public String query(String param){
		return "The query param  is "+ param;
	}
	
	
}
