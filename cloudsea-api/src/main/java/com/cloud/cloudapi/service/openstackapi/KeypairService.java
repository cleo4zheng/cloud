package com.cloud.cloudapi.service.openstackapi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.cloud.cloudapi.exception.BusinessException;
import com.cloud.cloudapi.pojo.common.TokenOs;
import com.cloud.cloudapi.pojo.openstackapi.forgui.Keypair;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface KeypairService {
	public List<Keypair> getKeypairList(Map<String,String> paramMap,TokenOs ostoken) throws BusinessException;
	public Keypair createKeypair(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Keypair uploadKeypair(String createBody,TokenOs ostoken) throws BusinessException, JsonProcessingException, IOException;
	public Keypair getKeypair(String keypairName,TokenOs ostoken) throws BusinessException;
	public String deleteKeypair(String keypairName,TokenOs ostoken) throws BusinessException;
	public Keypair downloadKeypair(String keypairName,TokenOs ostoken) throws BusinessException, IOException;

}
