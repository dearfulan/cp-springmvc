package com.chenpp.spring.web.service.impl;

import com.chenpp.spring.web.annotation.CPService;
import com.chenpp.spring.web.service.IndexService;

@CPService("index")
public class IndexServiceImpl implements IndexService {

	@Override
	public String getName() {
		System.out.println("进入service方法");
		return "getName";
	}

}
