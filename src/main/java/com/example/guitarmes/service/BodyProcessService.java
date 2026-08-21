package com.example.guitarmes.service;

import static com.example.guitarmes.common.BodyProcessConstants.*;
import static com.example.guitarmes.common.StatusConstants.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.common.DateTimeFormatterUtil;
import com.example.guitarmes.common.ProcessResultConstants;
import com.example.guitarmes.common.ProcessTargetConstants;
import com.example.guitarmes.dto.BodyProcessHistoryResponse;
import com.example.guitarmes.dto.ComponentProcessAverageTimeResponse;
import com.example.guitarmes.entity.Body;
import com.example.guitarmes.entity.BodyProcessHistory;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.BodyProcessHistoryRepository;
import com.example.guitarmes.repository.BodyRepository;
import com.example.guitarmes.repository.ManufacturingProcessRepository;

@Service
public class BodyProcessService {

    private final BodyProcessHistoryRepository historyRepository;
    private final BodyRepository bodyRepository;
    private final ManufacturingProcessRepository processRepository;

    public BodyProcessService(
            BodyProcessHistoryRepository historyRepository,
            BodyRepository bodyRepository,
            ManufacturingProcessRepository processRepository) {

        this.historyRepository = historyRepository;
        this.bodyRepository = bodyRepository;
        this.processRepository = processRepository;
    }

    @Transactional
    public BodyProcessHistory startProcess(Long bodyId, Long processId, String workerName) {
        if (historyRepository.existsByBodyIdAndEndTimeIsNull(bodyId)) {
            throw new BusinessException("このボディには現在実施中の工程があります。");
        }

        Body body = bodyRepository.findById(bodyId)
                        .orElseThrow(() ->new NotFoundException("指定されたボディが存在しません。"));

        ManufacturingProcess process = processRepository.findById(processId)
        		        .orElseThrow(() -> new NotFoundException("指定された工程が存在しません。"));

        if (!ProcessTargetConstants.BODY.equals(process.getTargetType())) {

            throw new BusinessException("指定された工程はボディ工程ではありません。");
        }

        validateStartableProcess(body, process.getProcessName());

        BodyProcessHistory history = new BodyProcessHistory(bodyId, processId, workerName, LocalDateTime.now());

        body.setCurrentProcess(process.getProcessName());

        body.setStatus(WORKING);

        bodyRepository.save(body);

        return historyRepository.save(history);
    }

    @Transactional
    public BodyProcessHistory endProcess(Long historyId, String result, String note) {

        BodyProcessHistory history = historyRepository.findById(historyId)
                        .orElseThrow(() -> new NotFoundException("指定された履歴が存在しません。"));

        if (history.getEndTime() != null) {
            throw new BusinessException("この工程はすでに終了しています。");
        }

        Body body = bodyRepository.findById(history.getBodyId())
                        .orElseThrow(() -> new NotFoundException("指定されたボディが存在しません。"));

        ManufacturingProcess process = processRepository.findById(history.getProcessId())
                        .orElseThrow(() -> new NotFoundException("指定された工程が存在しません。"));

        if (!ProcessTargetConstants.BODY.equals(process.getTargetType())) {
            throw new BusinessException(
                    "指定された工程はボディ工程ではありません。");
        }

        validateResult(process.getProcessName(), result);

        history.setResult(result);
        history.setNote(note);
        history.setEndTime(LocalDateTime.now());

        updateBodyAfterProcess(body, process.getProcessName(), result);

        bodyRepository.save(body);
        return historyRepository.save(history);
    }

    public List<BodyProcessHistory> getHistory(Long bodyId) {
        return historyRepository.findByBodyIdOrderByStartTimeAsc(bodyId);
    }
    
    public List<BodyProcessHistoryResponse>
    getHistoryResponses(Long bodyId) {

	List<BodyProcessHistory> histories =
	        historyRepository
	                .findByBodyIdOrderByStartTimeAsc(
	                        bodyId);
	
	List<BodyProcessHistoryResponse> responses =
	        new ArrayList<>();
	
	for (BodyProcessHistory history : histories) {
	
	    ManufacturingProcess process =
	            processRepository
	                    .findById(
	                            history.getProcessId())
	                    .orElseThrow(() ->
	                            new NotFoundException(
	                                    "指定された工程が存在しません。"));
	
	    BodyProcessHistoryResponse response =
	            new BodyProcessHistoryResponse();
	
	    response.setHistoryId(
	            history.getId());
	
	    response.setProcessName(
	            process.getProcessName());
	
	    response.setResult(
	            history.getResult());
	
	    response.setWorkerName(
	            history.getWorkerName());
	
	    response.setStartTimeText(
	            DateTimeFormatterUtil.format(
	                    history.getStartTime()));
	
	    response.setEndTimeText(
	            DateTimeFormatterUtil.format(
	                    history.getEndTime()));
	
	    response.setNote(
	            history.getNote());
	
	    if (history.getStartTime() != null
	            && history.getEndTime() != null) {
	
	        long workMinutes =
	                Duration.between(
	                        history.getStartTime(),
	                        history.getEndTime())
	                        .toMinutes();
	
	        response.setWorkMinutesText(
	                workMinutes + "分");
	
	    } else {
	
	        response.setWorkMinutesText("-");
	    }
	
	    responses.add(response);
	}
	
	return responses;
	}
	
	    public List<ManufacturingProcess>getBodyProcesses() {
	        return processRepository.findByTargetTypeOrderByProcessOrderAsc(ProcessTargetConstants.BODY);
	    }
	
	    public BodyProcessHistory getRunningProcess(Long bodyId) {
	        return historyRepository.findFirstByBodyIdAndEndTimeIsNullOrderByStartTimeDesc(bodyId).orElse(null);
	    }
	    
	    public ManufacturingProcess getCurrentProcess(Long bodyId) {
	    	Body body = bodyRepository.findById(bodyId).orElseThrow(
	    			() -> new NotFoundException("指定されたボディが存在しません。"));
	    	String  currentProcessName = body.getCurrentProcess();
	    	
	    	if (currentProcessName == null || currentProcessName.isBlank()) {
			throw new BusinessException("現在工程が設定されていません。");
	    	}
	    	
	    	return processRepository.findByTargetTypeAndProcessName(ProcessTargetConstants.BODY, currentProcessName)
	    			.orElseThrow(() -> new BusinessException("現在工程に対応する工程マスタが存在しません。"));
	    }
	
	    private void validateStartableProcess(Body body, String selectedProcessName) {
	        String currentProcess = body.getCurrentProcess();
	
	        if (currentProcess == null || currentProcess.isBlank()) {
	            throw new BusinessException("このボディの現在工程が設定されていません。");
	        }
	
	        if (!currentProcess.equals(selectedProcessName)) {
	            throw new BusinessException("現在開始可能な工程は「" + currentProcess + "」です。");
	        }
	
	        if (AVAILABLE.equals(body.getStatus())) {
	            throw new BusinessException(
	                    "このボディはすでに組立使用可能です。");
	        }
	
	        if (ASSEMBLED.equals(body.getStatus())) {
	            throw new BusinessException(
	                    "このボディはすでに組立済みです。");
	        }
	
	        if (REJECTED.equals(body.getStatus())) {
	            throw new BusinessException(
	                    "不合格のボディでは工程を開始できません。");
	        }
	    }
	
	    private void validateResult(String processName, String result) {
	        if (result == null || result.isBlank()) {
	            throw new BusinessException("工程結果を選択してください。");
	        }
	        
	        if (POST_PAINT_INSPECTION.equals(processName)) {
	            boolean valid =
	            		ProcessResultConstants.PASSED.equals(result) 
	                 || ProcessResultConstants.REWORK.equals(result) 
	                 || ProcessResultConstants.REJECTED.equals(result);
	            if (!valid) {
	                throw new BusinessException("塗装後検品では、合格・手直し・不合格のいずれかを選択してください。");
	            }
	            return;
	        }
	        
	        if (BUFFING.equals(processName) || PARTS_INSTALLATION.equals(processName)) {
	            if (!ProcessResultConstants.COMPLETED.equals(result)) {
	                throw new BusinessException("この工程では完了を選択してください。");
	            }
	            return;
	        }
	        throw new BusinessException("未対応のボディ工程です。");
	    }
	
	    private void updateBodyAfterProcess(Body body, String processName, String result) {
	        if (POST_PAINT_INSPECTION.equals(processName)) {
	            updateAfterInspection(body, result);
	            return;
	        }
	
	        if (BUFFING.equals(processName)) {
	            body.setCurrentProcess(POST_PAINT_INSPECTION);
	            body.setStatus(WAITING_INSPECTION);
	            return;
	        }
	
	        if (PARTS_INSTALLATION.equals(processName)) {
	            body.setCurrentProcess(WAITING_FOR_ASSEMBLY);
	            body.setStatus(AVAILABLE);
	            return;
	        }
	        throw new BusinessException(
	                "未対応のボディ工程です。");
	    }
	
	    private void updateAfterInspection(Body body, String result) {
	        if (ProcessResultConstants.PASSED.equals(result)) {
	            body.setCurrentProcess(PARTS_INSTALLATION);
	            body.setStatus(WAITING);
	            return;
	        }
	
	        if (ProcessResultConstants.REWORK.equals(result)) {
	            body.setCurrentProcess(BUFFING);
	            body.setStatus(REWORK);
	            return;
	        }
	
	        if (ProcessResultConstants.REJECTED.equals(result)) {
	            body.setCurrentProcess("製造終了");
	            body.setStatus(REJECTED);
	            return;
	        }
	
	        throw new BusinessException(
	                "塗装後検品の結果が不正です。");
	    }
	    
	    public List<ComponentProcessAverageTimeResponse>
	    getAverageProcessTimes() {
	
	Map<Long, Long> totalMinutesMap =
	        new LinkedHashMap<>();
	
	Map<Long, Long> completedCountMap =
	        new LinkedHashMap<>();
	
	for (BodyProcessHistory history
	        : historyRepository.findAll()) {
	
	    if (history.getStartTime() == null
	            || history.getEndTime() == null) {
	
	        continue;
	    }
	
	    long workMinutes =
	            Duration.between(
	                    history.getStartTime(),
	                    history.getEndTime())
	                    .toMinutes();
	
	    Long processId =
	            history.getProcessId();
	
	    totalMinutesMap.put(
	            processId,
	            totalMinutesMap.getOrDefault(
	                    processId,
	                    0L)
	            + workMinutes);
	
	    completedCountMap.put(
	            processId,
	            completedCountMap.getOrDefault(
	                    processId,
	                    0L)
	            + 1);
	}
	
	List<ComponentProcessAverageTimeResponse> responses =
	        new ArrayList<>();
	
	List<ManufacturingProcess> bodyProcesses =
	        processRepository
	                .findByTargetTypeOrderByProcessOrderAsc(
	                        ProcessTargetConstants.BODY);
	
	for (ManufacturingProcess process
	        : bodyProcesses) {
	
	    Long processId =
	            process.getId();
	
	    long completedCount =
	            completedCountMap.getOrDefault(
	                    processId,
	                    0L);
	
	    long averageMinutes = 0;
	
	    if (completedCount > 0) {
	
	        averageMinutes =
	                totalMinutesMap.getOrDefault(
	                        processId,
	                        0L)
	                / completedCount;
	    }
	
	    responses.add(
	            new ComponentProcessAverageTimeResponse(
	                    ProcessTargetConstants.BODY,
	                    process.getProcessName(),
	                    averageMinutes,
	                    completedCount));
	}

	return responses;
	}

}