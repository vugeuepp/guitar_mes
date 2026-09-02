
package com.example.guitarmes.process;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/process")
public class ProcessWorkController {
	private final ProcessService processService;
	
	public ProcessWorkController(ProcessService processService) {
		this.processService = processService;
	}
	
	@PostMapping("/start")
	public ProcessHistory startProcess(@RequestBody ProcessStartRequest request) {
		return processService.startProcess(request.getGuitarId(), request.getProcessId(), request.getWorkerName());
	}
	
	@PostMapping("/end")
	public ProcessHistory endProcess(@RequestBody ProcessEndRequest request) {
		return processService.endProcess(request.getHistoryId());
	}
	
	@GetMapping("/history/{guitarId}")
	public List<ProcessHistoryResponse> getHistory(@PathVariable Long guitarId) {
		return processService.getHistory(guitarId);
	}

    @PostMapping("/bulk/start")
    public List<ProcessHistory> startProcesses(
            @RequestBody BulkProcessStartRequest request) {
        return processService.startProcesses(
                request.getGuitarIds(),
                request.getProcessId(),
                request.getWorkerName());
    }

    @PostMapping("/bulk/end")
    public List<ProcessHistory> endProcesses(
            @RequestBody BulkProcessEndRequest request) {
        return processService.endProcesses(request.getHistoryIds());
    }
}
