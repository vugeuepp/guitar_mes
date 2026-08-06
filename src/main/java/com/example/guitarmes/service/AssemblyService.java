package com.example.guitarmes.service;

import static com.example.guitarmes.common.StatusConstants.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.common.DateTimeFormatterUtil;
import com.example.guitarmes.dto.AssemblyResponse;
import com.example.guitarmes.entity.Assembly;
import com.example.guitarmes.entity.Body;
import com.example.guitarmes.entity.Guitar;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.entity.Neck;
import com.example.guitarmes.entity.ProcessHistory;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.AssemblyRepository;
import com.example.guitarmes.repository.BodyRepository;
import com.example.guitarmes.repository.GuitarRepository;
import com.example.guitarmes.repository.ManufacturingProcessRepository;
import com.example.guitarmes.repository.NeckRepository;
import com.example.guitarmes.repository.ProcessHistoryRepository;

@Service
public class AssemblyService {
	private final AssemblyRepository assemblyRepository;
	private final GuitarRepository guitarRepository;
	private final NeckRepository neckRepository;
	private final BodyRepository bodyRepository;
	private final ProcessHistoryRepository processHistoryRepository;
	private final ManufacturingProcessRepository processRepository;
	
	private static final String NECK_ATTACH_PROCESS_NAME = "ネック取付";
	
	public AssemblyService(
			AssemblyRepository assemblyRepository,
			NeckRepository neckRepository,
			BodyRepository bodyRepository,
			GuitarRepository guitarRepository,
			ProcessHistoryRepository processHistoryRepository,
			ManufacturingProcessRepository processRepository) {
		this.assemblyRepository = assemblyRepository;
		this.neckRepository = neckRepository;
		this.bodyRepository = bodyRepository;
		this.guitarRepository = guitarRepository;
		this.processHistoryRepository = processHistoryRepository;
		this.processRepository = processRepository;
	}
	
	/*
	 * ①Assemblyを全件取得
	 * ②順番に処理する
	 * ③各AssemblyをconvertToResponse()でAssemblyResponseへ変換する
	 * ④Listにまとめる
	 */
	public List<AssemblyResponse> getAssemblies() {
		return assemblyRepository.findAll().stream().map(this::convertToResponse).toList();
	}
	
	private AssemblyResponse convertToResponse(Assembly assembly) {
		AssemblyResponse response = new AssemblyResponse();
		
		response.setAssemblyId(assembly.getId());
		response.setGuitarSerial(assembly.getGuitar().getSerialNo());
		response.setNeckSerial(assembly.getNeck().getSerialNo());
		response.setBodySerial(assembly.getBody().getSerialNo());
		response.setWorkerName(assembly.getWorkerName());
		response.setAssemblyDateText(DateTimeFormatterUtil.format(assembly.getAssemblyDate()));
		
		return response;
	}
	
	public AssemblyResponse getAssemblyById(Long assemblyId) {
		Assembly assembly = assemblyRepository.findById(assemblyId).orElseThrow(
				() -> new NotFoundException("指定された組み立て情報が存在しません。"));
		return convertToResponse(assembly);
	}
	
	@Transactional
	public Assembly createAssembly(
			Long guitarId,
			Long neckId,
			Long bodyId,
			String workerName) {
		
		/*
		 * neckIdに該当するNeckをDBから探す。
		 * 見つかったらneck変数に入れる。
		 * 見つからなかったら「指定されたネックが存在しません。」というNotFoundExceptionを投げる。
		 */
		Guitar targetGuitar = guitarRepository.findById(guitarId).orElseThrow(
				() -> new NotFoundException("指定されたギターが存在しません。"));
		
		Neck targetNeck = neckRepository.findById(neckId).orElseThrow(
				() -> new NotFoundException("指定されたネックが存在しません。"));
		
		Body targetBody = bodyRepository.findById(bodyId).orElseThrow(
				() -> new NotFoundException("指定されたボディが存在しません。"));
		
		if(ASSEMBLED.equals(targetNeck.getStatus())) {
			throw new BusinessException("このネックはすでに組み立て済みです。");
		}
		
		if(ASSEMBLED.equals(targetBody.getStatus())) {
			throw new BusinessException("このボディはすでに組み立て済みです。");
		}
		
		Assembly assembly = new Assembly(targetGuitar, targetNeck, targetBody, LocalDateTime.now(), workerName);
		
		Assembly savedAssembly = assemblyRepository.save(assembly);
		
		targetNeck.setStatus(ASSEMBLED);
		targetBody.setStatus(ASSEMBLED);
		
		targetGuitar.setCurrentProcess(NECK_ATTACH_PROCESS_NAME);
		
		neckRepository.save(targetNeck);
		bodyRepository.save(targetBody);
		guitarRepository.save(targetGuitar);
		
		ManufacturingProcess neckAttachProcess = findProcessByName(NECK_ATTACH_PROCESS_NAME);
		
		LocalDateTime now = LocalDateTime.now();
		
		ProcessHistory history = new ProcessHistory(targetGuitar.getId(), neckAttachProcess.getId(), workerName, now);
		
		history.setEndTime(now);
		processHistoryRepository.save(history);
		
		return savedAssembly;
	}
	
	private ManufacturingProcess findProcessByName(String processName) {
		List<ManufacturingProcess> processes = processRepository.findAll();
		for(ManufacturingProcess process : processes) {
			if(process.getProcessName().equals(processName)) {
				return process;
			}
		}
		throw new NotFoundException("指定された工程が存在しません。");
	}
	
	public AssemblyResponse getAssemblyByGuitarId(Long guitarId) {
		Assembly assembly = assemblyRepository.findByGuitar_Id(guitarId).orElse(null);
		if (assembly == null) {
			return null;
		}
		return convertToResponse(assembly);
	}
}
