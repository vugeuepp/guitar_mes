package com.example.guitarmes.process;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.common.DateTimeFormatterUtil;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.process.analysis.ProcessAverageTimeResponse;
import com.example.guitarmes.process.common.GuitarProcessConstants;
import com.example.guitarmes.process.common.ProcessStatusConstants;
import com.example.guitarmes.process.common.ProcessTargetConstants;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.productionorder.ProductionOrderStatusConstants;

@Service
public class ProcessService {

    private final ProcessHistoryRepository historyRepository;
    private final GuitarRepository guitarRepository;
    private final ManufacturingProcessRepository processRepository;
    private final ProductionOrderRepository productionOrderRepository;

    public ProcessService(
            ProcessHistoryRepository historyRepository,
            GuitarRepository guitarRepository,
            ManufacturingProcessRepository processRepository,
            ProductionOrderRepository productionOrderRepository) {

        this.historyRepository =
                historyRepository;

        this.guitarRepository =
                guitarRepository;

        this.processRepository =
                processRepository;

        this.productionOrderRepository =
                productionOrderRepository;
    }

    /**
     * Guitar工程を開始する。
     */
    @Transactional
    public ProcessHistory startProcess(
            Long guitarId,
            Long processId,
            String workerName) {

        validateWorkerName(workerName);

        Guitar guitar =
                findGuitarOrThrow(guitarId);

        /*
         * ProductionOrderを持たないGuitarは
         * 新フロー導入前の旧データ。
         */
        if (guitar.getProductionOrder() == null) {

            throw new BusinessException(
                    "旧フローのギターデータでは"
                    + "現行工程を開始できません。");
        }

        if (GuitarProcessConstants.COMPLETED
                .equals(guitar.getCurrentProcess())) {

            throw new BusinessException(
                    "完成済みのギターでは"
                    + "工程を開始できません。");
        }

        if (hasRunningProcess(guitarId)) {

            throw new BusinessException(
                    "現在実施中の工程があります。"
                    + "先に工程終了してください。");
        }

        ManufacturingProcess selectedProcess =
                findProcessOrThrow(processId);

        validateGuitarProcess(
                selectedProcess);

        ManufacturingProcess nextProcess =
                getNextAvailableProcess(guitarId);

        if (!nextProcess.getId().equals(
                selectedProcess.getId())) {

            throw new BusinessException(
                    "現在開始可能な工程は「"
                    + nextProcess.getProcessName()
                    + "」です。");
        }

        ProcessHistory history =
                new ProcessHistory(
                        guitarId,
                        processId,
                        workerName.trim(),
                        LocalDateTime.now());

        guitar.setCurrentProcess(
                selectedProcess.getProcessName());

        guitarRepository.save(guitar);

        return historyRepository.save(history);
    }

    /**
     * Guitar工程を終了する。
     */
    @Transactional
    public ProcessHistory endProcess(
            Long historyId) {

        ProcessHistory history =
                historyRepository.findById(historyId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定された工程履歴が存在しません。"));

        if (history.getEndTime() != null) {

            throw new BusinessException(
                    "この工程はすでに終了しています。");
        }

        ManufacturingProcess endedProcess =
                findProcessOrThrow(
                        history.getProcessId());

        validateGuitarProcess(
                endedProcess);

        Guitar guitar =
                findGuitarOrThrow(
                        history.getGuitarId());

        history.setEndTime(
                LocalDateTime.now());

        ProcessHistory savedHistory =
                historyRepository.save(history);

        ManufacturingProcess nextProcess =
                findNextProcessAfter(
                        endedProcess);

        if (nextProcess == null) {

            completeGuitar(guitar);

        } else {

            /*
             * 工程終了後は、次に開始する工程を
             * currentProcessへ設定する。
             */
            guitar.setCurrentProcess(
                    nextProcess.getProcessName());

            guitarRepository.save(guitar);
        }

        return savedHistory;
    }

    /**
     * 終了した工程より後のGuitar工程を取得する。
     * 最終工程の場合はnullを返す。
     */
    private ManufacturingProcess findNextProcessAfter(
            ManufacturingProcess endedProcess) {

        List<ManufacturingProcess> processes =
                getGuitarProcesses();

        for (ManufacturingProcess process
                : processes) {

            if (process.getProcessOrder()
                    > endedProcess.getProcessOrder()) {

                return process;
            }
        }

        return null;
    }

    /**
     * Guitarを完成状態へ変更する。
     */
    private void completeGuitar(
            Guitar guitar) {

        if (GuitarProcessConstants.COMPLETED
                .equals(guitar.getCurrentProcess())) {

            throw new BusinessException(
                    "このギターはすでに完成しています。");
        }

        guitar.setCurrentProcess(
                GuitarProcessConstants.COMPLETED);

        guitarRepository.save(guitar);

        updateProductionOrderAfterCompletion(
                guitar);
    }

    /**
     * Guitar完成時にProductionOrderの完成数を更新する。
     */
    private void updateProductionOrderAfterCompletion(
            Guitar guitar) {

        ProductionOrder productionOrder =
                guitar.getProductionOrder();

        /*
         * ProductionOrder導入前に作成された
         * 既存Guitarへの移行対応。
         */
        if (productionOrder == null) {
            return;
        }

        Integer completedQuantity =
                productionOrder
                        .getCompletedQuantity();

        Integer startedQuantity =
                productionOrder
                        .getStartedQuantity();

        Integer plannedQuantity =
                productionOrder
                        .getPlannedQuantity();

        if (completedQuantity == null
                || startedQuantity == null
                || plannedQuantity == null) {

            throw new BusinessException(
                    "生産計画の数量情報が不正です。");
        }

        if (plannedQuantity <= 0) {

            throw new BusinessException(
                    "生産計画の計画数が不正です。");
        }

        /*
         * 完成数は着手数を超えられない。
         */
        if (completedQuantity
                >= startedQuantity) {

            throw new BusinessException(
                    "完成数が着手数以上になっているため、"
                    + "完成数を更新できません。");
        }

        int nextCompletedQuantity =
                completedQuantity + 1;

        if (nextCompletedQuantity
                > plannedQuantity) {

            throw new BusinessException(
                    "完成数が計画数を超えるため、"
                    + "更新できません。");
        }

        productionOrder.setCompletedQuantity(
                nextCompletedQuantity);

        if (nextCompletedQuantity
                == plannedQuantity) {

            productionOrder.setStatus(
                    ProductionOrderStatusConstants
                            .COMPLETED);

        } else {

            productionOrder.setStatus(
                    ProductionOrderStatusConstants
                            .IN_PROGRESS);
        }

        productionOrderRepository.save(
                productionOrder);
    }

    /**
     * Guitar工程履歴を画面表示用DTOへ変換する。
     */
    public List<ProcessHistoryResponse> getHistory(
            Long guitarId) {

        Set<Long> guitarProcessIds =
                getGuitarProcessIds();

        return historyRepository
                .findByGuitarId(guitarId)
                .stream()
                /*
                 * 旧LEGACY_GUITAR履歴を除外する。
                 */
                .filter(history ->
                        guitarProcessIds.contains(
                                history.getProcessId()))
                .map(this::convertToResponse)
                .toList();
    }

    private ProcessHistoryResponse convertToResponse(
            ProcessHistory history) {

        ManufacturingProcess process =
                findProcessOrThrow(
                        history.getProcessId());

        ProcessHistoryResponse response =
                new ProcessHistoryResponse();

        response.setProcessName(
                process.getProcessName());

        response.setWorkerName(
                history.getWorkerName());

        response.setStartTimeText(
                DateTimeFormatterUtil.format(
                        history.getStartTime()));

        response.setEndTimeText(
                DateTimeFormatterUtil.format(
                        history.getEndTime()));

        if (history.getStartTime() != null
                && history.getEndTime() != null) {

            long minutes =
                    Duration.between(
                            history.getStartTime(),
                            history.getEndTime())
                            .toMinutes();

            response.setWorkMinutesText(
                    minutes + "分");

        } else {

            response.setWorkMinutesText("-");
        }

        return response;
    }

    /**
     * 全Guitarの実施中工程を取得する。
     * 旧LEGACY_GUITAR工程は除外する。
     */
    public List<ProcessHistory>
            getRunningProcesses() {

        Set<Long> guitarProcessIds =
                getGuitarProcessIds();

        return historyRepository
                .findByEndTimeIsNull()
                .stream()
                .filter(history ->
                        guitarProcessIds.contains(
                                history.getProcessId()))
                .toList();
    }

    /**
     * 指定Guitarの工程別状態を取得する。
     */
    public List<ProcessStatusResponse>
            getProcessStatuses(
                    Long guitarId) {

        List<ManufacturingProcess> processes =
                getGuitarProcesses();

        List<ProcessHistory> histories =
                getCurrentGuitarHistories(
                        guitarId);

        List<ProcessStatusResponse> responses =
                new ArrayList<>();

        for (ManufacturingProcess process
                : processes) {

            ProcessHistory targetHistory =
                    findHistoryByProcessId(
                            histories,
                            process.getId());

            String status;
            String workerName = "-";
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            Long workMinutes = null;
            Long historyId = null;

            if (targetHistory == null) {

                status =
                        ProcessStatusConstants
                                .NOT_STARTED;

            } else if (targetHistory.getEndTime()
                    == null) {

                status =
                        ProcessStatusConstants
                                .IN_PROGRESS;

                workerName =
                        targetHistory
                                .getWorkerName();

                startTime =
                        targetHistory
                                .getStartTime();

                historyId =
                        targetHistory.getId();

            } else {

                status =
                        ProcessStatusConstants
                                .COMPLETED;

                workerName =
                        targetHistory
                                .getWorkerName();

                startTime =
                        targetHistory
                                .getStartTime();

                endTime =
                        targetHistory
                                .getEndTime();

                historyId =
                        targetHistory.getId();

                if (startTime != null
                        && endTime != null) {

                    workMinutes =
                            Duration.between(
                                    startTime,
                                    endTime)
                                    .toMinutes();
                }
            }

            ProcessStatusResponse response =
                    new ProcessStatusResponse(
                            process.getProcessName(),
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

    /**
     * 同一工程の履歴を取得する。
     *
     * 現在のGuitar工程は一本道であり、
     * 各工程は原則1回のみ実施する。
     */
    private ProcessHistory findHistoryByProcessId(
            List<ProcessHistory> histories,
            Long processId) {

        for (ProcessHistory history
                : histories) {

            if (history.getProcessId()
                    .equals(processId)) {

                return history;
            }
        }

        return null;
    }

    /**
     * 指定Guitarで次に開始可能な工程を取得する。
     */
    public ManufacturingProcess
            getNextAvailableProcess(
                    Long guitarId) {

        Guitar guitar =
                findGuitarOrThrow(guitarId);

        if (GuitarProcessConstants.COMPLETED
                .equals(guitar.getCurrentProcess())) {

            throw new BusinessException(
                    "このギターはすでに完成しています。");
        }

        List<ManufacturingProcess> processes =
                getGuitarProcesses();

        if (processes.isEmpty()) {

            throw new BusinessException(
                    "Guitar工程マスタが登録されていません。");
        }

        List<ProcessHistory> histories =
                getCurrentGuitarHistories(
                        guitarId);

        for (ProcessHistory history
                : histories) {

            if (history.getEndTime() == null) {

                throw new BusinessException(
                        "現在実施中の工程があります。"
                        + "先に工程終了してください。");
            }
        }

        Set<Long> completedProcessIds =
                histories.stream()
                        .filter(history ->
                                history.getEndTime()
                                        != null)
                        .map(
                                ProcessHistory
                                        ::getProcessId)
                        .collect(
                                Collectors.toSet());

        /*
         * 工程順に確認し、未完了の最初の工程を返す。
         *
         * 旧LEGACY_GUITAR履歴のprocessOrderが
         * 新工程判定へ影響しない。
         */
        for (ManufacturingProcess process
                : processes) {

            if (!completedProcessIds.contains(
                    process.getId())) {

                return process;
            }
        }

        throw new BusinessException(
                "開始可能な工程がありません。");
    }

    /**
     * 進捗率を計算する。
     */
    public int getProgressRate(
            Long guitarId) {

        List<ProcessStatusResponse> statuses =
                getProcessStatuses(guitarId);

        int totalCount =
                statuses.size();

        if (totalCount == 0) {
            return 0;
        }

        double completedCount = 0;

        for (ProcessStatusResponse status
                : statuses) {

            if (ProcessStatusConstants.COMPLETED
                    .equals(status.getStatus())) {

                completedCount += 1;

            } else if (ProcessStatusConstants
                    .IN_PROGRESS
                    .equals(status.getStatus())) {

                completedCount += 0.5;
            }
        }

        return (int) (
                (completedCount / totalCount)
                * 100);
    }

    /**
     * 指定Guitarの実施中工程を取得する。
     */
    public ProcessHistory
            getRunningProcessByGuitarId(
                    Long guitarId) {

        Set<Long> guitarProcessIds =
                getGuitarProcessIds();

        for (ProcessHistory history
                : historyRepository
                        .findByEndTimeIsNull()) {

            if (history.getGuitarId()
                    .equals(guitarId)
                    && guitarProcessIds.contains(
                            history.getProcessId())) {

                return history;
            }
        }

        return null;
    }

    public boolean hasRunningProcess(
            Long guitarId) {

        return getRunningProcessByGuitarId(
                guitarId) != null;
    }

    public boolean hasNextProcess(
            Long guitarId) {

        try {

            getNextAvailableProcess(
                    guitarId);

            return true;

        } catch (BusinessException e) {

            return false;
        }
    }

    /**
     * Guitar工程別平均作業時間を取得する。
     * 旧LEGACY_GUITAR工程は集計対象外。
     */
    public List<ProcessAverageTimeResponse>
            getAverageProcessTimes() {

        Map<Long, Long> totalMinutesMap =
                new HashMap<>();

        Map<Long, Long> countMap =
                new HashMap<>();

        Set<Long> guitarProcessIds =
                getGuitarProcessIds();

        for (ProcessHistory history
                : historyRepository.findAll()) {

            if (!guitarProcessIds.contains(
                    history.getProcessId())) {

                continue;
            }

            if (history.getStartTime() == null
                    || history.getEndTime()
                            == null) {

                continue;
            }

            long minutes =
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
                    + minutes);

            countMap.put(
                    processId,
                    countMap.getOrDefault(
                            processId,
                            0L)
                    + 1);
        }

        List<ProcessAverageTimeResponse> responses =
                new ArrayList<>();

        /*
         * 工程マスタ順に表示する。
         * 実績0件の工程も0分として表示する。
         */
        for (ManufacturingProcess process
                : getGuitarProcesses()) {

            long count =
                    countMap.getOrDefault(
                            process.getId(),
                            0L);

            long averageMinutes = 0;

            if (count > 0) {

                averageMinutes =
                        totalMinutesMap
                                .getOrDefault(
                                        process.getId(),
                                        0L)
                        / count;
            }

            responses.add(
                    new ProcessAverageTimeResponse(
                            process.getProcessName(),
                            averageMinutes));
        }

        return responses;
    }

    /**
     * 現行Guitar工程マスタを取得する。
     */
    private List<ManufacturingProcess>
            getGuitarProcesses() {

        return processRepository
                .findByTargetTypeOrderByProcessOrderAsc(
                        ProcessTargetConstants
                                .GUITAR);
    }

    /**
     * 現行Guitar工程IDを取得する。
     */
    private Set<Long> getGuitarProcessIds() {

        return getGuitarProcesses()
                .stream()
                .map(
                        ManufacturingProcess::getId)
                .collect(
                        Collectors.toSet());
    }

    /**
     * 指定Guitarについて、
     * 現行Guitar工程に属する履歴だけを取得する。
     */
    private List<ProcessHistory>
            getCurrentGuitarHistories(
                    Long guitarId) {

        Set<Long> guitarProcessIds =
                getGuitarProcessIds();

        return historyRepository
                .findByGuitarId(guitarId)
                .stream()
                .filter(history ->
                        guitarProcessIds.contains(
                                history.getProcessId()))
                .toList();
    }

    private Guitar findGuitarOrThrow(
            Long guitarId) {

        return guitarRepository
                .findById(guitarId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定されたギターが存在しません。"));
    }

    private ManufacturingProcess
            findProcessOrThrow(
                    Long processId) {

        return processRepository
                .findById(processId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された工程が存在しません。"));
    }

    private void validateGuitarProcess(
            ManufacturingProcess process) {

        if (!ProcessTargetConstants.GUITAR
                .equals(
                        process.getTargetType())) {

            throw new BusinessException(
                    "指定された工程は"
                    + "Guitar工程ではありません。");
        }
    }

    private void validateWorkerName(
            String workerName) {

        if (workerName == null
                || workerName.isBlank()) {

            throw new BusinessException(
                    "作業者を入力してください。");
        }
    }

    @Transactional
    public List<ProcessHistory> startProcesses(
            List<Long> guitarIds,
            Long processId,
            String workerName) {
        validateIds(guitarIds, "工程開始対象を選択してください。");
        validateWorkerName(workerName);
        ManufacturingProcess selectedProcess = findProcessOrThrow(processId);
        validateGuitarProcess(selectedProcess);
        List<Long> uniqueIds = guitarIds.stream().distinct().toList();
        List<Guitar> guitars = uniqueIds.stream()
                .map(this::findGuitarOrThrow)
                .toList();
        for (Guitar guitar : guitars) {
            validateStartable(guitar, selectedProcess);
        }
        LocalDateTime now = LocalDateTime.now();
        List<ProcessHistory> histories = new ArrayList<>();
        for (Guitar guitar : guitars) {
            guitar.setCurrentProcess(selectedProcess.getProcessName());
            histories.add(new ProcessHistory(
                    guitar.getId(), processId, workerName.trim(), now));
        }
        guitarRepository.saveAll(guitars);
        return historyRepository.saveAll(histories);
    }

    @Transactional
    public List<ProcessHistory> endProcesses(List<Long> historyIds) {
        validateIds(historyIds, "工程終了対象を選択してください。");
        List<Long> uniqueIds = historyIds.stream().distinct().toList();
        List<ProcessHistory> histories = uniqueIds.stream()
                .map(id -> historyRepository.findById(id)
                        .orElseThrow(() -> new NotFoundException(
                                "指定された工程履歴が存在しません。ID: " + id)))
                .toList();
        List<Guitar> guitars = new ArrayList<>();
        List<ManufacturingProcess> processes = new ArrayList<>();
        for (ProcessHistory history : histories) {
            if (history.getEndTime() != null) {
                throw new BusinessException(
                        "すでに終了した工程が含まれています。履歴ID: " + history.getId());
            }
            ManufacturingProcess process = findProcessOrThrow(history.getProcessId());
            validateGuitarProcess(process);
            processes.add(process);
            guitars.add(findGuitarOrThrow(history.getGuitarId()));
        }
        Long firstProcessId = histories.get(0).getProcessId();
        if (histories.stream().anyMatch(h -> !firstProcessId.equals(h.getProcessId()))) {
            throw new BusinessException("異なる工程をまとめて終了することはできません。");
        }
        LocalDateTime now = LocalDateTime.now();
        histories.forEach(history -> history.setEndTime(now));
        for (int i = 0; i < guitars.size(); i++) {
            ManufacturingProcess nextProcess = findNextProcessAfter(processes.get(i));
            if (nextProcess == null) {
                completeGuitar(guitars.get(i));
            } else {
                guitars.get(i).setCurrentProcess(nextProcess.getProcessName());
            }
        }
        guitarRepository.saveAll(guitars);
        return historyRepository.saveAll(histories);
    }

    public List<ManufacturingProcess> getAvailableGuitarProcesses() {
        return getGuitarProcesses();
    }

    private void validateStartable(
            Guitar guitar,
            ManufacturingProcess selectedProcess) {
        if (guitar.getProductionOrder() == null) {
            throw new BusinessException(
                    "旧フローのギターデータが含まれています。ID: " + guitar.getId());
        }
        if (GuitarProcessConstants.COMPLETED.equals(guitar.getCurrentProcess())) {
            throw new BusinessException(
                    "完成済みのギターが含まれています。ID: " + guitar.getId());
        }
        if (hasRunningProcess(guitar.getId())) {
            throw new BusinessException(
                    "実施中工程のあるギターが含まれています。ID: " + guitar.getId());
        }
        ManufacturingProcess nextProcess = getNextAvailableProcess(guitar.getId());
        if (!nextProcess.getId().equals(selectedProcess.getId())) {
            throw new BusinessException(
                    "開始可能工程が一致しないギターが含まれています。ID: " + guitar.getId());
        }
    }

    private void validateIds(List<Long> ids, String message) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(message);
        }
        if (ids.stream().anyMatch(id -> id == null)) {
            throw new BusinessException("対象IDに不正な値が含まれています。");
        }
    }

    public ProcessRunningResponse getRunningProcessResponseByGuitarId(
            Long guitarId) {
        ProcessHistory history = getRunningProcessByGuitarId(guitarId);
        return history == null ? null : toRunningResponse(history);
    }

    public List<ProcessRunningResponse> getRunningProcessResponses() {
        return getRunningProcesses().stream()
                .map(this::toRunningResponse)
                .toList();
    }

    private ProcessRunningResponse toRunningResponse(
            ProcessHistory history) {
        Guitar guitar = findGuitarOrThrow(history.getGuitarId());
        ManufacturingProcess process = findProcessOrThrow(history.getProcessId());
        return new ProcessRunningResponse(
                history.getId(),
                guitar.getId(),
                guitar.getSerialNo(),
                process.getId(),
                process.getProcessName(),
                history.getWorkerName(),
                history.getStartTime());
    }
}
