package com.example.guitarmes.service;

import static com.example.guitarmes.common.StatusConstants.*;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.guitarmes.common.ProcessConstants;
import com.example.guitarmes.common.StatusConstants;
import com.example.guitarmes.entity.Neck;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.NeckRepository;

@Service
public class NeckService {
	private final NeckRepository neckRepository;
	
	public NeckService(NeckRepository neckRepository) {
		this.neckRepository = neckRepository;
	}
	
	public List<Neck> getNecks() {
		return neckRepository.findAll();
	}
	
	public Neck getNeckById(Long id) {
		return findNeckOrThrow(id);
	}
	
	public Neck createNeck(
			String serialNo,
			String modelName) {
		Neck neck = new Neck (
				serialNo,
				modelName,
				ProcessConstants.NOT_STARTED,
				StatusConstants.WAITING);
		return neckRepository.save(neck);
	}
	
	public List<Neck> getAvailableNecks() {
		return neckRepository.findByStatusNot(ASSEMBLED);
	}
	
	private Neck findNeckOrThrow(Long id) {
	    return neckRepository.findById(id).orElseThrow(
	    		() -> new NotFoundException("指定されたネックが存在しません。"));
	}
	
	public long getAvailableNeckCount() {
		return getAvailableNecks().size();
	}
}
