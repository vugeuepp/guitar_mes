package com.example.guitarmes.assembly;

import static com.example.guitarmes.common.StatusConstants.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.body.Body;
import com.example.guitarmes.body.BodyRepository;
import com.example.guitarmes.common.DateTimeFormatterUtil;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.guitar.Guitar;
import com.example.guitarmes.guitar.GuitarService;
import com.example.guitarmes.neck.Neck;
import com.example.guitarmes.neck.NeckRepository;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.productionorder.ProductionOrderStatusConstants;
import com.example.guitarmes.productionschedule.ProductionSchedule;
import com.example.guitarmes.productionschedule.ProductionScheduleRepository;

@Service
public class AssemblyService {

    private final AssemblyRepository assemblyRepository;
    private final NeckRepository neckRepository;
    private final BodyRepository bodyRepository;
    private final ProductionOrderRepository
            productionOrderRepository;
    private final GuitarService guitarService;
    private final ProductionScheduleRepository productionScheduleRepository;

    public AssemblyService(
            AssemblyRepository assemblyRepository,
            NeckRepository neckRepository,
            BodyRepository bodyRepository,
            ProductionOrderRepository
                    productionOrderRepository,
            GuitarService guitarService,
            ProductionScheduleRepository productionScheduleRepository) {

        this.assemblyRepository =
                assemblyRepository;

        this.neckRepository =
                neckRepository;

        this.bodyRepository =
                bodyRepository;

        this.productionOrderRepository =
                productionOrderRepository;

        this.guitarService =
                guitarService;
        this.productionScheduleRepository =
                productionScheduleRepository;
    }

    /**
     * ネック取付実績を全件取得する。
     */
    public List<AssemblyResponse> getAssemblies() {

        return assemblyRepository
                .findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * ネック取付実績を画面表示用DTOへ変換する。
     */
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

    /**
     * IDを指定してネック取付実績を取得する。
     */
    public AssemblyResponse getAssemblyById(Long assemblyId) {
        Assembly assembly = assemblyRepository.findById(assemblyId).orElseThrow(
        		() -> new NotFoundException("指定されたネック取付実績が" + "存在しません。"));
        return convertToResponse(assembly);
    }

    /**
     * 生産計画に対してネック取付を登録する。
     *
     * この処理で以下を同時に実行する。
     *
     * 1. ProductionOrder取得
     * 2. 組立可能なNeck取得
     * 3. 組立可能なBody取得
     * 4. Guitar生成・DYシリアル採番
     * 5. Assembly保存
     * 6. NeckをASSEMBLEDへ更新
     * 7. BodyをASSEMBLEDへ更新
     * 8. ProductionOrderの着手数を加算
     */
    @Transactional
    public Assembly createAssembly(
            Long productionOrderId,
            Long productionScheduleId,
            Long neckId,
            Long bodyId,
            String workerName) {

        ProductionOrder productionOrder = findProductionOrderOrThrow(productionOrderId);
        ProductionSchedule productionSchedule =
                findProductionScheduleOrThrow(productionScheduleId);
        Neck targetNeck = findNeckOrThrow(neckId);
        Body targetBody = findBodyOrThrow(bodyId);

        validateWorkerName(workerName);
        validateProductionOrder(productionOrder);
        validateProductionSchedule(
                productionOrder,
                productionSchedule);
        validateNeckAvailable(targetNeck);
        validateBodyAvailable(targetBody);
        validateComponentCompatibility(
                productionOrder,
                targetNeck,
                targetBody);
        validateComponentProductionSchedule(
                productionOrder,
                productionSchedule,
                targetNeck,
                targetBody);
        validateProductionOrder(productionOrder);

        /*
         * ネック取付完了時点で、
         * Guitar個体を初めて生成する。
         *
         * ProductとProductionOrderの関連付け、
         * DYシリアル番号の採番、
         * 最初のGuitar工程の設定は
         * GuitarServiceに任せる。
         */
        Guitar guitar = guitarService.createGuitar(productionOrder);

        Assembly assembly = new Assembly(guitar, targetNeck, targetBody, LocalDateTime.now(), workerName.trim());

        Assembly savedAssembly = assemblyRepository.save(assembly);

        /*
         * 使用済み部材へ変更する。
         */
        targetNeck.setStatus(ASSEMBLED);
        targetBody.setStatus(ASSEMBLED);
        neckRepository.save(targetNeck);
        bodyRepository.save(targetBody);

        /*
         * 生産計画の着手数を更新する。
         */
        int nextStartedQuantity = productionOrder.getStartedQuantity() + 1;
        productionOrder.setStartedQuantity(nextStartedQuantity);
        productionOrder.setStatus(ProductionOrderStatusConstants.IN_PROGRESS);
        productionOrderRepository.save(productionOrder);
        return savedAssembly;
    }
    
    private ProductionSchedule findProductionScheduleOrThrow(
            Long productionScheduleId) {
        return productionScheduleRepository
                .findById(productionScheduleId)
                .orElseThrow(() -> new NotFoundException(
                        "指定された日産計画が存在しません。"));
    }

    private void validateProductionSchedule(
            ProductionOrder productionOrder,
            ProductionSchedule productionSchedule) {
        if (productionSchedule.getProductionOrder() == null
                || !productionOrder.getId().equals(
                        productionSchedule.getProductionOrder().getId())) {
            throw new BusinessException(
                    "日産計画が生産計画と一致していません。");
        }
    }

    private void validateComponentProductionSchedule(
            ProductionOrder productionOrder,
            ProductionSchedule productionSchedule,
            Neck neck,
            Body body) {
        if (body.getProductionOrder() == null
                || neck.getProductionOrder() == null
                || body.getProductionSchedule() == null
                || neck.getProductionSchedule() == null) {
            throw new BusinessException(
                    "日産計画から発行されたボディとネックを選択してください。");
        }
        Long orderId = productionOrder.getId();
        Long scheduleId = productionSchedule.getId();
        if (!orderId.equals(body.getProductionOrder().getId())
                || !orderId.equals(neck.getProductionOrder().getId())
                || !scheduleId.equals(body.getProductionSchedule().getId())
                || !scheduleId.equals(neck.getProductionSchedule().getId())) {
            throw new BusinessException(
                    "選択されたボディとネックは同じ日産計画に属していません。");
        }
    }

    private void validateComponentCompatibility(
            ProductionOrder productionOrder,
            Neck neck,
            Body body) {

        if (productionOrder.getProduct() == null) {
            throw new BusinessException("生産計画に製品が設定されていません。");
        }

        if (body.getBodyMaster() == null) {
            throw new BusinessException("選択されたボディにボディマスタが設定されていません。");
        }

        if (neck.getNeckMaster() == null) {
            throw new BusinessException("選択されたネックにネックマスタが設定されていません。");
        }

        if (productionOrder.getProduct().getBodyMaster() == null) {
            throw new BusinessException("対象製品に対応するボディマスタが設定されていません。");
        }

        if (productionOrder.getProduct().getNeckMaster() == null) {
            throw new BusinessException("対象製品に対応するネックマスタが設定されていません。");
        }

        Long expectedBodyMasterId = productionOrder.getProduct().getBodyMaster().getId();
        Long selectedBodyMasterId = body.getBodyMaster().getId();

        if (!expectedBodyMasterId.equals(selectedBodyMasterId)) {
            throw new BusinessException(
                    "選択されたボディはこの生産計画の製品と一致しません。");
        }

        Long expectedNeckMasterId = productionOrder.getProduct().getNeckMaster().getId();
        Long selectedNeckMasterId = neck.getNeckMaster().getId();

        if (!expectedNeckMasterId.equals(selectedNeckMasterId)) {
            throw new BusinessException(
                    "選択されたネックはこの生産計画の製品と一致しません。");
        }
    }

    /**
     * ProductionOrderを取得する。
     */
    private ProductionOrder findProductionOrderOrThrow(Long productionOrderId) {

        return productionOrderRepository.findById(productionOrderId).orElseThrow(
        		() -> new NotFoundException("指定された生産計画が存在しません。"));
    }

    /**
     * Neckを取得する。
     */
    private Neck findNeckOrThrow(Long neckId) {
        return neckRepository.findById(neckId).orElseThrow(
        		() -> new NotFoundException("指定されたネックが存在しません。"));
    }

    /**
     * Bodyを取得する。
     */
    private Body findBodyOrThrow(
            Long bodyId) {

        return bodyRepository.findById(bodyId).orElseThrow(
        		() -> new NotFoundException("指定されたボディが存在しません。"));
    }

    /**
     * 生産計画がネック取付可能な状態か確認する。
     */
    private void validateProductionOrder(ProductionOrder productionOrder) {

        if (ProductionOrderStatusConstants.CANCELLED.equals(productionOrder.getStatus())) {
            throw new BusinessException(
                    "中止された生産計画ではネック取付を登録できません。");
        }

        if (ProductionOrderStatusConstants.COMPLETED.equals(productionOrder.getStatus())) {
            throw new BusinessException(
                    "完了済みの生産計画ではネック取付を登録できません。");
        }

        Integer plannedQuantity = productionOrder.getPlannedQuantity();
        Integer startedQuantity = productionOrder.getStartedQuantity();

        if (plannedQuantity == null || plannedQuantity <= 0) {
            throw new BusinessException("生産計画の計画数が不正です。");
        }

        if (startedQuantity == null) {
            throw new BusinessException("生産計画の着手数が設定されていません。");
        }

        if (startedQuantity >= plannedQuantity) {
            throw new BusinessException("この生産計画は計画数に達しています。");
        }

        if (productionOrder.getProduct() == null) {
            throw new BusinessException("生産計画に製品が設定されていません。");
        }
    }

    /**
     * Neckが組立可能か確認する。
     */
    private void validateNeckAvailable(Neck neck) {
        if (!AVAILABLE.equals(neck.getStatus())) {
            throw new BusinessException("組立可能なネックを選択してください。");
        }
    }

    /**
     * Bodyが組立可能か確認する。
     */
    private void validateBodyAvailable(Body body) {
        if (!AVAILABLE.equals(body.getStatus())) {
            throw new BusinessException(
                    "組立可能なボディを選択してください。");
        }
    }

    /**
     * 作業者名を確認する。
     */
    private void validateWorkerName(String workerName) {
        if (workerName == null || workerName.isBlank()) {
            throw new BusinessException("作業者を入力してください。");
        }
    }

    /**
     * Guitar IDからネック取付実績を取得する。
     */
    public AssemblyResponse getAssemblyByGuitarId(Long guitarId) {
        Assembly assembly = assemblyRepository.findByGuitar_Id(guitarId).orElse(null);
        if (assembly == null) {
            return null;
        }
        return convertToResponse(assembly);
    }

    @Transactional
    public List<Assembly> createAssemblies(
            Long productionOrderId,
            Long productionScheduleId,
            List<Long> neckIds,
            List<Long> bodyIds,
            String workerName) {
        validateWorkerName(workerName);
        validateBulkIds(neckIds, bodyIds);

        ProductionOrder productionOrder =
                findProductionOrderOrThrow(productionOrderId);
        ProductionSchedule productionSchedule =
                findProductionScheduleOrThrow(productionScheduleId);
        validateProductionOrder(productionOrder);
        validateProductionSchedule(productionOrder, productionSchedule);

        int count = neckIds.size();
        if (productionOrder.getStartedQuantity() + count
                > productionOrder.getPlannedQuantity()) {
            throw new BusinessException(
                    "一括登録件数が生産計画の残り数量を超えています。");
        }

        List<Long> uniqueNeckIds = neckIds.stream().distinct().toList();
        List<Long> uniqueBodyIds = bodyIds.stream().distinct().toList();
        if (uniqueNeckIds.size() != count) {
            throw new BusinessException("同じネックを複数回選択できません。");
        }
        if (uniqueBodyIds.size() != count) {
            throw new BusinessException("同じボディを複数回選択できません。");
        }

        List<Neck> necks = neckIds.stream()
                .map(this::findNeckOrThrow)
                .toList();
        List<Body> bodies = bodyIds.stream()
                .map(this::findBodyOrThrow)
                .toList();

        for (int i = 0; i < count; i++) {
            Neck neck = necks.get(i);
            Body body = bodies.get(i);
            validateNeckAvailable(neck);
            validateBodyAvailable(body);
            validateComponentCompatibility(productionOrder, neck, body);
            validateComponentProductionSchedule(
                    productionOrder, productionSchedule, neck, body);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Assembly> assemblies = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            Neck neck = necks.get(i);
            Body body = bodies.get(i);
            Guitar guitar = guitarService.createGuitar(productionOrder);
            assemblies.add(new Assembly(
                    guitar, neck, body, now, workerName.trim()));
            neck.setStatus(ASSEMBLED);
            body.setStatus(ASSEMBLED);
        }

        List<Assembly> saved = assemblyRepository.saveAll(assemblies);
        neckRepository.saveAll(necks);
        bodyRepository.saveAll(bodies);
        productionOrder.setStartedQuantity(
                productionOrder.getStartedQuantity() + count);
        productionOrder.setStatus(
                ProductionOrderStatusConstants.IN_PROGRESS);
        productionOrderRepository.save(productionOrder);
        return saved;
    }

    private void validateBulkIds(
            List<Long> neckIds,
            List<Long> bodyIds) {
        if (neckIds == null || bodyIds == null
                || neckIds.isEmpty() || bodyIds.isEmpty()) {
            throw new BusinessException("登録予定を1件以上追加してください。");
        }
        if (neckIds.size() != bodyIds.size()) {
            throw new BusinessException(
                    "ネックとボディの登録件数が一致していません。");
        }
        if (neckIds.stream().anyMatch(java.util.Objects::isNull)
                || bodyIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new BusinessException("登録予定に不正なIDが含まれています。");
        }
    }
}
