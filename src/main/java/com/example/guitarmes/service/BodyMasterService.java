package com.example.guitarmes.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.entity.BodyMaster;
import com.example.guitarmes.repository.BodyMasterRepository;

@Service
public class BodyMasterService {
	private final BodyMasterRepository bodyMasterRepository;

	public BodyMasterService(BodyMasterRepository bodyMasterRepository) {
		this.bodyMasterRepository = bodyMasterRepository;
	}
	
	public List<BodyMaster> getBodyMasters() {
		return bodyMasterRepository.findAll();
	}
	
	public BodyMaster getBodyMasterById(Long id) {
		return bodyMasterRepository.findById(id).orElseThrow(
				() -> new RuntimeException("ボディマスタが存在しません。"));
	}
	
	public BodyMaster createBodyMaster(BodyMaster bodyMaster) {
		return bodyMasterRepository.save(bodyMaster);
	}
}
