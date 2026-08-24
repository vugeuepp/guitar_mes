package com.example.guitarmes.service;

import static com.example.guitarmes.common.GuitarProcessConstants.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.common.ProcessConstants;
import com.example.guitarmes.dto.GuitarProgressResponse;
import com.example.guitarmes.dto.ProcessCountResponse;
import com.example.guitarmes.entity.Guitar;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.entity.Product;
import com.example.guitarmes.entity.ProductionOrder;
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
	
	private String generateSerialNo() {
		String year = String.valueOf(LocalDate.now().getYear()).substring(2);
		String prefix = "DY" + year;
		
		Guitar lastGuitar = guitarRepository.findTopBySerialNoStartingWithOrderBySerialNoDesc(prefix).orElse(null);
		
		int nextNumber = 1;
		
		if(lastGuitar != null) {
			String lastSerial = lastGuitar.getSerialNo();
			
			nextNumber = Integer.parseInt(lastSerial.substring(4)) + 1; 
		}
		
		return prefix + String.format("%04d", nextNumber);
	}
	
	public Guitar createGuitar(Product product) {
		Guitar guitar = new Guitar();
		guitar.setSerialNo(generateSerialNo());
		guitar.setCurrentProcess(ProcessConstants.NOT_STARTED);
		guitar.setProduct(product);
		return guitarRepository.save(guitar);
	}
	
	@Transactional
	public Guitar createGuitar(
	        ProductionOrder productionOrder) {

	    if (productionOrder == null) {
	        throw new BusinessException(
	                "生産計画が指定されていません。");
	    }

	    if (productionOrder.getProduct() == null) {
	        throw new BusinessException(
	                "生産計画に製品が設定されていません。");
	    }

	    Guitar guitar =
	            new Guitar();

	    guitar.setSerialNo(
	            generateSerialNo());

	    guitar.setProduct(
	            productionOrder.getProduct());

	    guitar.setProductionOrder(
	            productionOrder);

	    /*
	     * ネック取付が完了した時点でGuitarが成立する。
	     * 次に開始するGuitar工程を初期値として設定する。
	     */
	    guitar.setCurrentProcess(
	            PARTS_INSTALLATION);

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
			
			boolean needAssembly = nextProcess != null && ProcessConstants.NECK_ASSEMBLY.equals(nextProcess.getProcessName()) && assemblyService.getAssemblyByGuitarId(guitarId) == null;
			
			String productName = guitar.getProduct().getProductName();
			
			responses.add(
					new GuitarProgressResponse(
						guitar.getId(),
						guitar.getSerialNo(), 
						productName,
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
			if(ProcessConstants.COMPLETED.equals(guitar.getCurrentProcess())) {
				count++;
			}
		}
		return count;
	}
	
	public long getInProgressGuitarCount() {
		return getTotalGuitarCount() - getCompletedGuitarCount();
	}
	
	public List<ProcessCountResponse> getProcessCounts() {
		Map<String, Long> countMap = new LinkedHashMap<>();
		
		for (Guitar guitar : guitarRepository.findAll()) {
			String currentProcess = guitar.getCurrentProcess();
			
			if (currentProcess == null) {
				currentProcess = "未設定";
			}
			
			countMap.put(currentProcess, countMap.getOrDefault(currentProcess, 0L) + 1);
		}
		
		List<ProcessCountResponse> responses = new ArrayList<>();
		
		for (Map.Entry<String, Long> entry : countMap. entrySet()) {
			responses.add(new ProcessCountResponse(entry.getKey(), entry.getValue()));
		}
		
		return responses;
	}
	
	public int getCompletionRate() {
		long total = getTotalGuitarCount();
		if (total == 0) {
			return 0;
		}
		return (int)((getCompletedGuitarCount() * 100) / total);
	}
	
	public List<Guitar> getGuitarsByProductId(Long productId) {
		return guitarRepository.findByProductId(productId);
	}
	
	public List<Guitar> getGuitarsByProductionOrderId(
	        Long productionOrderId) {

	    return guitarRepository
	            .findByProductionOrderIdOrderByIdAsc(
	                    productionOrderId);
	}

}
