package com.example.guitarmes.neck.process;

import static com.example.guitarmes.common.StatusConstants.*;
import static com.example.guitarmes.neck.process.NeckProcessConstants.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.common.DateTimeFormatterUtil;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.neck.Neck;
import com.example.guitarmes.neck.NeckRepository;
import com.example.guitarmes.process.ManufacturingProcess;
import com.example.guitarmes.process.ManufacturingProcessRepository;
import com.example.guitarmes.process.analysis.ComponentProcessAverageTimeResponse;
import com.example.guitarmes.process.common.ProcessResultConstants;
import com.example.guitarmes.process.common.ProcessTargetConstants;

@Service
public class NeckProcessService {

    private final NeckProcessHistoryRepository historyRepository;
    private final NeckRepository neckRepository;
    private final ManufacturingProcessRepository processRepository;

    public NeckProcessService(
            NeckProcessHistoryRepository historyRepository,
            NeckRepository neckRepository,
            ManufacturingProcessRepository processRepository) {

        this.historyRepository = historyRepository;
        this.neckRepository = neckRepository;
        this.processRepository = processRepository;
    }

    /**
     * ネック工程を開始する。
     */
    @Transactional
    public NeckProcessHistory startProcess(
            Long neckId,
            Long processId,
            String workerName) {

        if (historyRepository
                .existsByNeckIdAndEndTimeIsNull(
                        neckId)) {

            throw new BusinessException(
                    "このネックには現在実施中の工程があります。");
        }

        Neck neck =
                neckRepository.findById(neckId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定されたネックが存在しません。"));

        ManufacturingProcess process =
                processRepository.findById(processId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定された工程が存在しません。"));

        if (!ProcessTargetConstants.NECK.equals(
                process.getTargetType())) {

            throw new BusinessException(
                    "指定された工程はネック工程ではありません。");
        }

        validateStartableProcess(
                neck,
                process.getProcessName());

        NeckProcessHistory history =
                new NeckProcessHistory(
                        neckId,
                        processId,
                        workerName,
                        LocalDateTime.now());

        neck.setCurrentProcess(
                process.getProcessName());

        neck.setStatus(WORKING);

        neckRepository.save(neck);

        return historyRepository.save(history);
    }

    /**
     * ネック工程を終了する。
     */
    @Transactional
    public NeckProcessHistory endProcess(
            Long historyId,
            String result,
            String note) {

        NeckProcessHistory history =
                historyRepository.findById(historyId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定された履歴が存在しません。"));

        if (history.getEndTime() != null) {

            throw new BusinessException(
                    "この工程はすでに終了しています。");
        }

        Neck neck =
                neckRepository.findById(
                                history.getNeckId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定されたネックが存在しません。"));

        ManufacturingProcess process =
                processRepository.findById(
                                history.getProcessId())
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定された工程が存在しません。"));

        if (!ProcessTargetConstants.NECK.equals(
                process.getTargetType())) {

            throw new BusinessException(
                    "指定された工程はネック工程ではありません。");
        }

        validateResult(
                process.getProcessName(),
                result);

        validateNgNote(
                result,
                note);

        history.setResult(result);
        history.setNote(note);
        history.setEndTime(
                LocalDateTime.now());

        updateNeckAfterProcess(
                neck,
                process.getProcessName(),
                result);

        neckRepository.save(neck);

        return historyRepository.save(history);
    }

    /**
     * 指定ネックの現在工程に対応する工程マスタを取得する。
     */
    public ManufacturingProcess getCurrentProcess(
            Long neckId) {

        Neck neck =
                neckRepository.findById(neckId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定されたネックが存在しません。"));

        validateProcessAvailable(neck);

        String currentProcessName =
                neck.getCurrentProcess();

        return processRepository
                .findByTargetTypeAndProcessName(
                        ProcessTargetConstants.NECK,
                        currentProcessName)
                .orElseThrow(() ->
                        new BusinessException(
                                "現在工程に対応する"
                                + "ネック工程マスタが存在しません。"));
    }

    /**
     * 指定ネックの実施中工程を取得する。
     */
    public NeckProcessHistory getRunningProcess(
            Long neckId) {

        return historyRepository
                .findFirstByNeckIdAndEndTimeIsNullOrderByStartTimeDesc(
                        neckId)
                .orElse(null);
    }

    /**
     * 指定ネックの全工程履歴を取得する。
     */
    public List<NeckProcessHistory> getHistory(
            Long neckId) {

        return historyRepository
                .findByNeckIdOrderByStartTimeAsc(
                        neckId);
    }

    /**
     * 選択された工程が現在工程と一致しているか確認する。
     */
    private void validateStartableProcess(
            Neck neck,
            String selectedProcessName) {

        validateProcessAvailable(neck);

        String currentProcess =
                neck.getCurrentProcess();

        if (!currentProcess.equals(
                selectedProcessName)) {

            throw new BusinessException(
                    "現在開始可能な工程は「"
                    + currentProcess
                    + "」です。");
        }
    }

    /**
     * 対象ネックが工程開始可能な状態か確認する。
     */
    private void validateProcessAvailable(
            Neck neck) {

        String currentProcess =
                neck.getCurrentProcess();

        if (currentProcess == null
                || currentProcess.isBlank()) {

            throw new BusinessException(
                    "このネックの現在工程が設定されていません。");
        }

        if (AVAILABLE.equals(
                neck.getStatus())) {

            throw new BusinessException(
                    "このネックはすでに組立使用可能です。");
        }

        if (ASSEMBLED.equals(
                neck.getStatus())) {

            throw new BusinessException(
                    "このネックはすでに組立済みです。");
        }

        if (RETURNED.equals(
                neck.getStatus())) {

            throw new BusinessException(
                    "このネックは塗装前工程へ"
                    + "差し戻されています。");
        }

        if (REJECTED.equals(
                neck.getStatus())) {

            throw new BusinessException(
                    "不合格のネックでは"
                    + "工程を開始できません。");
        }
    }

    /**
     * 工程ごとに有効な終了結果か確認する。
     */
    private void validateResult(
            String processName,
            String result) {

        if (result == null
                || result.isBlank()) {

            throw new BusinessException(
                    "工程結果を選択してください。");
        }

        /*
         * PLEKと擦り合わせでは、
         * COMPLETEDまたはNGを選択できる。
         */
        if (PLEK.equals(processName)
                || FRET_AND_NUT_FINISH.equals(
                        processName)) {

            boolean valid =
                    ProcessResultConstants.COMPLETED
                            .equals(result)
                    || ProcessResultConstants.NG
                            .equals(result);

            if (!valid) {

                throw new BusinessException(
                        "この工程では完了またはNGを"
                        + "指定してください。");
            }

            return;
        }

        /*
         * ネックパーツ付けでは、
         * COMPLETEDだけを許可する。
         */
        if (PARTS_INSTALLATION.equals(
                processName)) {

            if (!ProcessResultConstants.COMPLETED
                    .equals(result)) {

                throw new BusinessException(
                        "ネックパーツ付けでは"
                        + "完了を指定してください。");
            }

            return;
        }

        throw new BusinessException(
                "未対応のネック工程です。");
    }

    /**
     * NGの場合は差し戻し理由を必須とする。
     */
    private void validateNgNote(
            String result,
            String note) {

        if (ProcessResultConstants.NG
                .equals(result)
                && (note == null
                    || note.isBlank())) {

            throw new BusinessException(
                    "NGの場合は差し戻し理由を"
                    + "備考へ入力してください。");
        }
    }

    /**
     * 工程終了後のネック状態を更新する。
     */
    private void updateNeckAfterProcess(
            Neck neck,
            String processName,
            String result) {

        /*
         * PLEK
         */
        if (PLEK.equals(processName)) {

            if (ProcessResultConstants.COMPLETED
                    .equals(result)) {

                neck.setCurrentProcess(
                        FRET_AND_NUT_FINISH);

                neck.setStatus(WAITING);

                return;
            }

            if (ProcessResultConstants.NG
                    .equals(result)) {

                returnToPrePaint(neck);

                return;
            }
        }

        /*
         * フレット擦り合わせ・ナット手成形
         */
        if (FRET_AND_NUT_FINISH.equals(
                processName)) {

            if (ProcessResultConstants.COMPLETED
                    .equals(result)) {

                neck.setCurrentProcess(
                        PARTS_INSTALLATION);

                neck.setStatus(WAITING);

                return;
            }

            if (ProcessResultConstants.NG
                    .equals(result)) {

                returnToPrePaint(neck);

                return;
            }
        }

        /*
         * ネックパーツ付け
         */
        if (PARTS_INSTALLATION.equals(
                processName)
                && ProcessResultConstants.COMPLETED
                        .equals(result)) {

            neck.setCurrentProcess(
                    WAITING_FOR_ASSEMBLY);

            neck.setStatus(AVAILABLE);

            return;
        }

        throw new BusinessException(
                "未対応のネック工程または結果です。");
    }

    /**
     * PLEKまたは擦り合わせでNGとなったネックを、
     * 現在の管理範囲外である塗装前工程へ差し戻す。
     */
    private void returnToPrePaint(
            Neck neck) {

        neck.setCurrentProcess(
                RETURNED_TO_PRE_PAINT);

        neck.setStatus(RETURNED);
    }
    public List<NeckProcessHistoryResponse>
    getHistoryResponses(Long neckId) {

	List<NeckProcessHistory> histories =
	        historyRepository
	                .findByNeckIdOrderByStartTimeAsc(
	                        neckId);
	
	List<NeckProcessHistoryResponse> responses =
	        new ArrayList<>();
	
	for (NeckProcessHistory history : histories) {
	
	    ManufacturingProcess process =
	            processRepository
	                    .findById(
	                            history.getProcessId())
	                    .orElseThrow(() ->
	                            new NotFoundException(
	                                    "指定された工程が存在しません。"));
	
	    NeckProcessHistoryResponse response =
	            new NeckProcessHistoryResponse();
	
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
    public List<ComponentProcessAverageTimeResponse>
    getAverageProcessTimes() {

		Map<Long, Long> totalMinutesMap =
		        new LinkedHashMap<>();
		
		Map<Long, Long> completedCountMap =
		        new LinkedHashMap<>();
		
		for (NeckProcessHistory history
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
		
		List<ManufacturingProcess> neckProcesses =
		        processRepository
		                .findByTargetTypeOrderByProcessOrderAsc(
		                        ProcessTargetConstants.NECK);
		
		for (ManufacturingProcess process
		        : neckProcesses) {
		
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
		                    ProcessTargetConstants.NECK,
		                    process.getProcessName(),
		                    averageMinutes,
		                    completedCount));
		}
		
		return responses;
		}

}