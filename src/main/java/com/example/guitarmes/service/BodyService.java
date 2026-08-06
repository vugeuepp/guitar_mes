package com.example.guitarmes.service;

import static com.example.guitarmes.common.StatusConstants.*;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.common.ProcessConstants;
import com.example.guitarmes.common.StatusConstants;
import com.example.guitarmes.entity.Body;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.BodyRepository;

@Service
public class BodyService {
	private final BodyRepository bodyRepository;
	
	public BodyService(BodyRepository bodyRepository) {
		this.bodyRepository = bodyRepository;
	}
	
	public List<Body> getBodies() {
		return bodyRepository.findAll();
	}
	
	public Body getBodyById(Long id) {
		return findBodyOrThrow(id);
	}
	
	public Body createBody(
			String serialNo,
			String modelName,
			String color) {
		Body body = new Body(serialNo, modelName, color, ProcessConstants.NOT_STARTED, StatusConstants.WAITING);
		return bodyRepository.save(body);
	}
	
	public List<Body> getAvailableBodies() {
		return bodyRepository.findByStatusNot(ASSEMBLED);
	}
	
	private Body findBodyOrThrow(Long id) {
	    return bodyRepository.findById(id).orElseThrow(
	    		() -> new NotFoundException("指定されたボディが存在しません。"));
	}
	
	public long getAvailableBodyCount() {
		return getAvailableBodies().size();
	}
}
