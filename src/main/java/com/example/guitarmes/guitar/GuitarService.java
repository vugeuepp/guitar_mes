package com.example.guitarmes.guitar;

import static com.example.guitarmes.process.common.GuitarProcessConstants.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.assembly.AssemblyService;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.process.ProcessService;
import com.example.guitarmes.process.analysis.ProcessCountResponse;
import com.example.guitarmes.process.common.ProcessConstants;
import com.example.guitarmes.productionorder.ProductionOrder;

@Service
public class GuitarService {

    private final GuitarRepository guitarRepository;

    public GuitarService(
            GuitarRepository guitarRepository) {

        this.guitarRepository =
                guitarRepository;
    }

    /**
     * Guitarを全件取得する。
     */
    public List<Guitar> getGuitars() {

        return guitarRepository.findAll();
    }

    /**
     * Guitarのシリアル番号を自動採番する。
     *
     * 形式：
     * DY + 年下2桁 + 4桁連番
     */
    private String generateSerialNo() {

        String year =
                String.valueOf(
                        LocalDate.now().getYear())
                        .substring(2);

        String prefix =
                "DY" + year;

        Guitar lastGuitar =
                guitarRepository
                        .findTopBySerialNoStartingWithOrderBySerialNoDesc(
                                prefix)
                        .orElse(null);

        int nextNumber = 1;

        if (lastGuitar != null) {

            String lastSerial =
                    lastGuitar.getSerialNo();

            nextNumber =
                    Integer.parseInt(
                            lastSerial.substring(4))
                    + 1;
        }

        return prefix
                + String.format(
                        "%04d",
                        nextNumber);
    }

    /**
     * ProductionOrderからGuitarを生成する。
     *
     * ネック取付が完了した時点で
     * Guitar個体が成立する。
     */
    @Transactional
    public Guitar createGuitar(
            ProductionOrder productionOrder) {

        if (productionOrder == null) {

            throw new BusinessException(
                    "生産計画が指定されていません。");
        }

        if (productionOrder.getProduct()
                == null) {

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
         * ネック取付完了後に開始する
         * 最初のGuitar工程。
         */
        guitar.setCurrentProcess(
                PARTS_INSTALLATION);

        return guitarRepository.save(
                guitar);
    }

    /**
     * IDを指定してGuitarを取得する。
     */
    public Guitar getGuitarById(
            Long id) {

        return findGuitarOrThrow(id);
    }

    /**
     * Guitarの現在工程を更新する。
     */
    @Transactional
    public Guitar updateGuitar(
            Long id,
            String currentProcess) {

        Guitar guitar =
                findGuitarOrThrow(id);

        guitar.setCurrentProcess(
                currentProcess);

        return guitarRepository.save(
                guitar);
    }

    /**
     * Guitarを取得する。
     */
    private Guitar findGuitarOrThrow(
            Long id) {

        return guitarRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定されたギターが"
                                + "存在しません。"));
    }

    /**
     * Guitar一覧・ダッシュボード用の
     * 進捗情報を作成する。
     */
    public List<GuitarProgressResponse>
            getGuitarProgressList(
                    ProcessService processService,
                    AssemblyService assemblyService) {

        List<GuitarProgressResponse> responses =
                new ArrayList<>();

        for (Guitar guitar
                : guitarRepository.findAll()) {

            Long guitarId =
                    guitar.getId();

            int progressRate =
                    processService
                            .getProgressRate(
                                    guitarId);

            boolean hasRunningProcess =
                    processService
                            .hasRunningProcess(
                                    guitarId);

            boolean hasNextProcess =
                    processService
                            .hasNextProcess(
                                    guitarId);

            String productName = "-";
            String color = "-";

            if (guitar.getProduct() != null) {

                productName =
                        guitar.getProduct()
                                .getProductName();
                color = guitar.getProduct().getColor();
            }

            GuitarProgressResponse response =
                    new GuitarProgressResponse(
                            guitar.getId(),
                            guitar.getSerialNo(),
                            productName,
                            color,
                            guitar.getCurrentProcess(),
                            progressRate,
                            hasRunningProcess,
                            hasNextProcess);

            responses.add(response);
        }

        return responses;
    }

    /**
     * 一覧表示用Guitarを検索条件で絞り込む。
     */
    public List<GuitarProgressResponse> filterGuitarProgressList(
            List<GuitarProgressResponse> guitars,
            String serial,
            String product,
            String currentProcess,
            String status) {
        String serialCondition = normalize(serial);
        String productCondition = normalize(product);
        String processCondition = normalize(currentProcess);
        String statusCondition = normalize(status);
        return guitars.stream()
                .filter(guitar -> serialCondition.isEmpty()
                        || normalize(guitar.getSerialNo())
                                .contains(serialCondition))
                .filter(guitar -> productCondition.isEmpty()
                        || Objects.equals(
                                productCondition,
                                normalize(guitar.getProductName())))
                .filter(guitar -> processCondition.isEmpty()
                        || Objects.equals(
                                processCondition,
                                normalize(guitar.getCurrentProcess())))
                .filter(guitar -> statusCondition.isEmpty()
                        || Objects.equals(
                                statusCondition,
                                getDisplayStatus(guitar)))
                .toList();
    }

    /** 一覧の製品選択肢を重複なしで返す。 */
    public List<String> getProductOptions(
            List<GuitarProgressResponse> guitars) {
        return guitars.stream()
                .map(GuitarProgressResponse::getProductName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty() && !"-".equals(value))
                .distinct()
                .sorted()
                .toList();
    }

    /** 検索条件が1つ以上指定されているか判定する。 */
    public boolean hasSearchCondition(
            String serial,
            String product,
            String currentProcess,
            String status) {
        return !normalize(serial).isEmpty()
                || !normalize(product).isEmpty()
                || !normalize(currentProcess).isEmpty()
                || !normalize(status).isEmpty();
    }

    private String getDisplayStatus(GuitarProgressResponse guitar) {
        if (guitar.isHasRunningProcess()) {
            return "working";
        }
        if (COMPLETED.equals(guitar.getCurrentProcess())) {
            return "completed";
        }
        return "waiting";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * Guitar総数を取得する。
     */
    public long getTotalGuitarCount() {

        return guitarRepository.count();
    }

    /**
     * 完成済みGuitar数を取得する。
     */
    public long getCompletedGuitarCount() {

        long count = 0;

        for (Guitar guitar
                : guitarRepository.findAll()) {

            if (ProcessConstants.COMPLETED
                    .equals(
                            guitar.getCurrentProcess())) {

                count++;
            }
        }

        return count;
    }

    /**
     * 製造中Guitar数を取得する。
     */
    public long getInProgressGuitarCount() {

        return getTotalGuitarCount()
                - getCompletedGuitarCount();
    }

    /**
     * 現在工程別のGuitar数を取得する。
     */
    public List<ProcessCountResponse>
            getProcessCounts() {

        Map<String, Long> countMap =
                new LinkedHashMap<>();

        for (Guitar guitar
                : guitarRepository.findAll()) {

            String currentProcess =
                    guitar.getCurrentProcess();

            if (currentProcess == null
                    || currentProcess.isBlank()) {

                currentProcess = "未設定";
            }

            countMap.put(
                    currentProcess,
                    countMap.getOrDefault(
                            currentProcess,
                            0L)
                    + 1);
        }

        List<ProcessCountResponse> responses =
                new ArrayList<>();

        for (Map.Entry<String, Long> entry
                : countMap.entrySet()) {

            responses.add(
                    new ProcessCountResponse(
                            entry.getKey(),
                            entry.getValue()));
        }

        return responses;
    }

    /**
     * Guitar完成率を取得する。
     */
    public int getCompletionRate() {

        long total =
                getTotalGuitarCount();

        if (total == 0) {
            return 0;
        }

        return (int) (
                getCompletedGuitarCount()
                * 100
                / total);
    }

    /**
     * Product IDでGuitarを取得する。
     */
    public List<Guitar> getGuitarsByProductId(
            Long productId) {

        return guitarRepository
                .findByProductId(
                        productId);
    }

    /**
     * ProductionOrder IDでGuitarを取得する。
     */
    public List<Guitar>
            getGuitarsByProductionOrderId(
                    Long productionOrderId) {

        return guitarRepository
                .findByProductionOrderIdOrderByIdAsc(
                        productionOrderId);
    }
}
