package com.example.guitarmes.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.dto.ProcessHistoryResponse;
import com.example.guitarmes.dto.ProcessStatusResponse;
import com.example.guitarmes.entity.Guitar;
import com.example.guitarmes.entity.ManufacturingProcess;
import com.example.guitarmes.entity.ProcessHistory;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.GuitarRepository;
import com.example.guitarmes.repository.ManufacturingProcessRepository;
import com.example.guitarmes.repository.ProcessHistoryRepository;


@Service
public class ProcessService {
	private final ProcessHistoryRepository historyRepository;
	private final GuitarRepository guitarRepository;
	private final ManufacturingProcessRepository processRepository;
	
	public ProcessService(
			ProcessHistoryRepository historyRepository, 
			GuitarRepository guitarRepository, 
			ManufacturingProcessRepository processRepository) {
		this.historyRepository = historyRepository;
		this.guitarRepository = guitarRepository;
		this.processRepository = processRepository;
	}
	
	@Transactional
	public ProcessHistory startProcess(Long guitarId, Long processId, String workerName) {
		ProcessHistory history = new ProcessHistory(guitarId, processId, workerName, LocalDateTime.now());
		
		historyRepository.save(history);
		Guitar guitar = guitarRepository.findById(guitarId).orElseThrow(
				() -> new NotFoundException("指定されたギターが存在しません。"));
		ManufacturingProcess process = processRepository.findById(processId).orElseThrow();
		guitar.setCurrentProcess(process.getProcessName());
		guitarRepository.save(guitar);
		
		return history;
	}
	
	@Transactional
	public ProcessHistory endProcess(Long historyId) {
	    ProcessHistory history = historyRepository.findById(historyId)
	            .orElseThrow(() ->
	                    new NotFoundException("指定された工程履歴が存在しません。"));
	    
	    if (history.getEndTime() != null) {
	        throw new BusinessException("この工程はすでに終了しています。");
	    }

	    history.setEndTime(LocalDateTime.now());
	    ProcessHistory savedHistory = historyRepository.save(history);

	    ManufacturingProcess endedProcess = processRepository.findById(history.getProcessId())
	                    .orElseThrow(() -> new NotFoundException("指定された工程が存在しません。"));

	    List<ManufacturingProcess> processes = processRepository.findAllByOrderByProcessOrderAsc();

	    int maxOrder = 0;
	    for (ManufacturingProcess process : processes) {
	        if (process.getProcessOrder() > maxOrder) {
	            maxOrder = process.getProcessOrder();
	        }
	    }

	    if (endedProcess.getProcessOrder() == maxOrder) {
	        Guitar guitar = guitarRepository.findById(history.getGuitarId())
	                .orElseThrow(() ->
	                        new NotFoundException("指定されたギターが存在しません。"));
	        guitar.setCurrentProcess("完成");

	        guitarRepository.save(guitar);
	    }

	    return savedHistory;
	}
	
	public List<ProcessHistoryResponse> getHistory(Long guitarId) {
		return historyRepository.findByGuitarId(guitarId).stream().map(this::convertToResponse).toList();
	}
	
	private ProcessHistoryResponse convertToResponse(ProcessHistory history) {
		ManufacturingProcess process = processRepository.findById(history.getProcessId()).orElseThrow();
		
		ProcessHistoryResponse response = new ProcessHistoryResponse();
		
		response.setProcessName(process.getProcessName());
		response.setWorkerName(history.getWorkerName());
		response.setStartTime(history.getStartTime());
		response.setEndTime(history.getEndTime());
		if (history.getStartTime() != null && history.getEndTime() != null) {
			long minutes = Duration.between(history.getStartTime(), history.getEndTime()).toMinutes();
			response.setWorkMinutes(minutes);
		}
		
		return response;
	}
	
	public List<ProcessHistory> getRunningProcesses() {
		return historyRepository.findByEndTimeIsNull();
	}
	
	public List<ProcessStatusResponse> getProcessStatuses(Long guitarId) {
		
		List<ManufacturingProcess> processes = processRepository.findAllByOrderByProcessOrderAsc();
		List<ProcessHistory> histories = historyRepository.findByGuitarId(guitarId);
		List<ProcessStatusResponse> responses = new ArrayList<>();
		
		for (ManufacturingProcess process : processes) {
		    ProcessHistory targetHistory = findHistoryByProcessId(histories, process.getId());
		    String status;
		    String workerName = "-";
		    LocalDateTime startTime = null;
		    LocalDateTime endTime = null;
		    Long workMinutes = null;
		    Long historyId = null;
		    
		    if (targetHistory == null) {
		        status = "未実施";
		    } else if (targetHistory.getEndTime() == null) {
		        status = "実施中";
		        workerName = targetHistory.getWorkerName();
		        startTime = targetHistory.getStartTime();
		        historyId = targetHistory.getId();
		    } else {
		        status = "完了";
		        workerName = targetHistory.getWorkerName();
		        startTime = targetHistory.getStartTime();
		        endTime = targetHistory.getEndTime();
		        historyId = targetHistory.getId();
		        workMinutes = Duration.between(targetHistory.getStartTime(), targetHistory.getEndTime()).toMinutes();
		    }
		    ProcessStatusResponse response = new ProcessStatusResponse(process.getProcessName(), 
		    														   status, 
		    														   workerName, 
		    														   startTime, 
		    														   endTime, 
		    														   workMinutes, 
		    														   historyId);

		    responses.add(response);
		}
		return responses;
	}
	
	private ProcessHistory findHistoryByProcessId(List<ProcessHistory> histories, Long processId) {
	    for (ProcessHistory history : histories) {
	        if (history.getProcessId().equals(processId)) {
	            return history;
	        }
	    }
	    return null;
	}
	
	public ManufacturingProcess getNextAvailableProcess(Long guitarId) {
	    List<ManufacturingProcess> processes = processRepository.findAllByOrderByProcessOrderAsc();
	    List<ProcessHistory> histories = historyRepository.findByGuitarId(guitarId);

	    for (ProcessHistory history : histories) {
	        if (history.getEndTime() == null) {
	            throw new BusinessException("現在実施中の工程があります。先に工程終了してください。");
	        }
	    }
	    int completedMaxOrder = 0;

	    for (ProcessHistory history : histories) {
	        ManufacturingProcess process = processRepository.findById(history.getProcessId()).orElseThrow(
	                		() -> new NotFoundException("指定された工程が存在しません。"));

	        if (history.getEndTime() != null
	                && process.getProcessOrder() > completedMaxOrder) {
	            completedMaxOrder = process.getProcessOrder();
	        }
	    }

	    int nextOrder = completedMaxOrder + 1;

	    for (ManufacturingProcess process : processes) {
	        if (process.getProcessOrder() == nextOrder) {
	            return process;
	        }
	    }
	    throw new BusinessException("開始可能な工程がありません。");
	}
	/*
	 * 進捗率計算
	 */
	public int getProgressRate(Long guitarId) {
		List<ProcessStatusResponse> statuses = getProcessStatuses(guitarId);
		
		int totalCount = statuses.size();
		
		double completedCount = 0;
		
		for (ProcessStatusResponse status : statuses) {
			if ("完了".equals(status.getStatus())) {
				completedCount += 1;
			} else if ("実施中".equals(status.getStatus())) {
				completedCount += 0.5;
			}
		}
		return (int)((completedCount / totalCount) * 100);
	}
	
	public ProcessHistory getRunningProcessByGuitarId(Long guitarId) {
		List<ProcessHistory> histories = historyRepository.findByEndTimeIsNull();
		
		 for (ProcessHistory history : histories) {
			 if (history.getGuitarId().equals(guitarId)) {
				 return history;
			 }
		 }
		 return null;
	}
	
	public boolean hasRunningProcess(Long guitarId) {
		ProcessHistory runningHistory = getRunningProcessByGuitarId(guitarId);
		return runningHistory != null;
	}
	
	public boolean hasNextProcess(Long guitarId) {
		try {
			getNextAvailableProcess(guitarId);
			return true;
		} catch (BusinessException e) {
			return false;
		}
	}
}
