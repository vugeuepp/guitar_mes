package com.example.guitarmes.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.dto.GuitarProgressResponse;
import com.example.guitarmes.entity.Guitar;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.GuitarRepository;



@Service
public class GuitarService {
	private final GuitarRepository guitarRepository;
	
	public GuitarService(GuitarRepository guitarRepository) {
		this.guitarRepository = guitarRepository;
	}
	
	public List<Guitar> getGuitars() {
		return guitarRepository.findAll();
	}
	
	public Guitar createGuitar(String serialNo, String modelName, String currentProcess) {
		Guitar guitar = new Guitar(serialNo, modelName, currentProcess);
		return guitarRepository.save(guitar);
	}

	public Guitar getGuitarById(Long id) {
		return findGuitarOrThrow(id);
	}
	
	@Transactional
	public Guitar updateGuitar(Long id, String currentProcess) {
		Guitar guitar = findGuitarOrThrow(id);
		
		guitar.setCurrentProcess(currentProcess);
		
		return guitarRepository.save(guitar);
	}
	
	private Guitar findGuitarOrThrow(Long id) {
		return guitarRepository.findById(id).orElseThrow(
				() -> new NotFoundException("指定されたギターが存在しません。"));
	}
	
	public List<GuitarProgressResponse> getGuitarProgressList(ProcessService processService, AssemblyService assemblyService) {
		List<GuitarProgressResponse> responses = new ArrayList<>();
		for (Guitar guitar : guitarRepository.findAll()) {
			Long guitarId = guitar.getId();
			int progressRate = processService.getProgressRate(guitarId);
			boolean hasRunningProcess = processService.hasRunningProcess(guitarId);
			boolean hasNextProcess = processService.hasNextProcess(guitarId);
			
			ManufacturingProcess nextProcess = null;
			
			try {
				nextProcess = processService.getNextAvailableProcess(guitarId);
			} catch (BusinessException e) {
				nextProcess = null;
			}
			
			boolean needAssembly = nextProcess != null && "ネック取付".equals(nextProcess.getProcessName()) && assemblyService.getAssemblyByGuitarId(guitarId) == null;
			
			responses.add(
					new GuitarProgressResponse(
						guitar.getId(),
						guitar.getSerialNo(), 
						guitar.getModelName(),
						guitar.getCurrentProcess(),
						progressRate,
						hasRunningProcess,
						hasNextProcess,
						needAssembly));
		}
		return responses;
	}
	
	public long getTotalGuitarCount() {
	    return guitarRepository.count();
	}
	
	public long getCompletedGuitarCount() {
		long count = 0;
		for (Guitar guitar : guitarRepository.findAll()) {
			if("完成".equals(guitar.getCurrentProcess())) {
				count++;
			}
		}
		return count;
	}
	
	public long getInProgressGuitarCount() {
		return getTotalGuitarCount() - getCompletedGuitarCount();
	}
}
