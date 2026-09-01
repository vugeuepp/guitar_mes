package com.example.guitarmes.productionschedule;

import static com.example.guitarmes.productionschedule.ProductionScheduleStatusConstants.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.body.BodyRepository;
import com.example.guitarmes.body.BodyService;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.neck.NeckRepository;
import com.example.guitarmes.neck.NeckService;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.productionorder.ProductionOrder;
import com.example.guitarmes.productionorder.ProductionOrderRepository;
import com.example.guitarmes.productionorder.ProductionOrderStatusConstants;

@Service
public class ProductionScheduleService {

    private final ProductionScheduleRepository
            productionScheduleRepository;

    private final ProductionOrderRepository
            productionOrderRepository;
    private final BodyRepository bodyRepository;
    private final BodyService bodyService;
    private final NeckRepository neckRepository;
    private final NeckService neckService;

    @Autowired
    public ProductionScheduleService(
            ProductionScheduleRepository productionScheduleRepository,
            ProductionOrderRepository productionOrderRepository,
            BodyRepository bodyRepository,
            BodyService bodyService,
            NeckRepository neckRepository,
            NeckService neckService) {
        this.productionScheduleRepository = productionScheduleRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.bodyRepository = bodyRepository;
        this.bodyService = bodyService;
        this.neckRepository = neckRepository;
        this.neckService = neckService;
    }

    ProductionScheduleService(
            ProductionScheduleRepository productionScheduleRepository,
            ProductionOrderRepository productionOrderRepository) {
        this(productionScheduleRepository, productionOrderRepository, null, null, null, null);
    }

    public ProductionSchedule getProductionScheduleById(
            Long id) {

        return productionScheduleRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された日産計画が存在しません。"));
    }

    public List<ProductionSchedule> getProductionSchedulesByOrderId(
            Long productionOrderId) {

        getProductionOrder(productionOrderId);

        return productionScheduleRepository
                .findByProductionOrderIdOrderByScheduleDateAsc(
                        productionOrderId);
    }

    public int getAllocatedQuantity(
            Long productionOrderId) {

        getProductionOrder(productionOrderId);

        return toInt(
                productionScheduleRepository.sumAllocatedQuantity(
                        productionOrderId,
                        CANCELLED));
    }

    public int getUnallocatedQuantity(
            Long productionOrderId) {

        ProductionOrder productionOrder =
                getProductionOrder(productionOrderId);

        int allocatedQuantity = toInt(
                productionScheduleRepository.sumAllocatedQuantity(
                        productionOrderId,
                        CANCELLED));

        return productionOrder.getPlannedQuantity()
                - allocatedQuantity;
    }

    @Transactional
    public ProductionSchedule createProductionSchedule(
            Long productionOrderId,
            LocalDate scheduleDate,
            Integer plannedQuantity) {

        ProductionOrder productionOrder =
                getProductionOrder(productionOrderId);

        validateProductionOrderAvailable(productionOrder);
        validateRequest(scheduleDate, plannedQuantity);
        validateScheduleMonth(productionOrder, scheduleDate);
        validateDuplicateForCreate(
                productionOrderId,
                scheduleDate);
        validateMonthlyCapacity(
                productionOrder,
                plannedQuantity,
                null);

        ProductionSchedule productionSchedule =
                new ProductionSchedule(
                        productionOrder,
                        scheduleDate,
                        plannedQuantity,
                        PLANNED);

        return productionScheduleRepository.save(
                productionSchedule);
    }

    @Transactional
    public ProductionSchedule updateProductionSchedule(
            Long id,
            LocalDate scheduleDate,
            Integer plannedQuantity) {

        ProductionSchedule productionSchedule =
                getProductionScheduleById(id);

        validateEditable(productionSchedule);
        validateRequest(scheduleDate, plannedQuantity);

        ProductionOrder productionOrder =
                productionSchedule.getProductionOrder();

        validateProductionOrderAvailable(productionOrder);
        validateScheduleMonth(productionOrder, scheduleDate);
        validateDuplicateForUpdate(
                productionOrder.getId(),
                scheduleDate,
                id);
        validateMonthlyCapacity(
                productionOrder,
                plannedQuantity,
                id);

        productionSchedule.setScheduleDate(scheduleDate);
        productionSchedule.setPlannedQuantity(plannedQuantity);

        return productionScheduleRepository.save(
                productionSchedule);
    }

    @Transactional
    public ProductionSchedule confirmProductionSchedule(
            Long id) {

        ProductionSchedule productionSchedule =
                getProductionScheduleById(id);

        if (!PLANNED.equals(productionSchedule.getStatus())) {
            throw new BusinessException(
                    "計画中の日産計画だけ確定できます。");
        }

        validateProductionOrderAvailable(
                productionSchedule.getProductionOrder());

        productionSchedule.setStatus(CONFIRMED);

        return productionScheduleRepository.save(
                productionSchedule);
    }

    @Transactional
    public ProductionSchedule cancelProductionSchedule(
            Long id) {

        ProductionSchedule productionSchedule =
                getProductionScheduleById(id);

        if (COMPLETED.equals(productionSchedule.getStatus())
                || CANCELLED.equals(productionSchedule.getStatus())) {

            throw new BusinessException(
                    "完了済みまたは取消済みの日産計画は取消できません。");
        }

        validateNotIssued(productionSchedule.getId());
        productionSchedule.setStatus(CANCELLED);

        return productionScheduleRepository.save(
                productionSchedule);
    }

    @Transactional
    public void issueComponents(Long id) {
        ProductionSchedule schedule = getProductionScheduleById(id);
        if (!CONFIRMED.equals(schedule.getStatus())) {
            throw new BusinessException("確定済みの日産計画のみ部品を発行できます。");
        }
        ProductionOrder order = schedule.getProductionOrder();
        validateProductionOrderAvailable(order);
        Product product = order.getProduct();
        if (product == null) { throw new BusinessException("日産計画に対応する製品が指定されていません。"); }
        if (product.getBodyMaster() == null) { throw new BusinessException("製品に対応するボディマスタが設定されていません。"); }
        if (product.getNeckMaster() == null) { throw new BusinessException("製品に対応するネックマスタが設定されていません。"); }
        validateNotIssued(id);
        for (int i = 0; i < schedule.getPlannedQuantity(); i++) {
            bodyService.createBody(product.getBodyMaster().getId(), order, schedule);
            neckService.createNeck(product.getNeckMaster().getId(), product, order, schedule);
        }
    }

    public long getIssuedBodyCount(Long productionScheduleId) {
        return bodyRepository.countByProductionSchedule_Id(
                productionScheduleId);
    }

    public long getIssuedNeckCount(Long productionScheduleId) {
        return neckRepository.countByProductionSchedule_Id(
                productionScheduleId);
    }

    public boolean isComponentsIssued(
            ProductionSchedule productionSchedule) {
        if (productionSchedule == null
                || productionSchedule.getId() == null) {
            return false;
        }
        long bodyCount = getIssuedBodyCount(
                productionSchedule.getId());
        long neckCount = getIssuedNeckCount(
                productionSchedule.getId());
        return bodyCount >= productionSchedule.getPlannedQuantity()
                && neckCount >= productionSchedule.getPlannedQuantity();
    }

    private void validateNotIssued(Long id) {
        if (bodyRepository == null || neckRepository == null) { return; }
        if (bodyRepository.countByProductionSchedule_Id(id) > 0
                || neckRepository.countByProductionSchedule_Id(id) > 0) {
            throw new BusinessException("既に部品発行済みです。");
        }
    }

    private ProductionOrder getProductionOrder(
            Long productionOrderId) {

        return productionOrderRepository
                .findById(productionOrderId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された生産計画が存在しません。"));
    }
    
    private void validateEditable(
            ProductionSchedule productionSchedule) {

        if (!PLANNED.equals(
                productionSchedule.getStatus())) {

            throw new BusinessException(
                    "計画中の日産計画のみ編集できます。");
        }
    }

    private void validateProductionOrderAvailable(
            ProductionOrder productionOrder) {

        if (ProductionOrderStatusConstants.CANCELLED.equals(
                productionOrder.getStatus())) {

            throw new BusinessException(
                    "中止された生産計画には日産計画を登録・変更できません。");
        }

        if (ProductionOrderStatusConstants.COMPLETED.equals(
                productionOrder.getStatus())) {

            throw new BusinessException(
                    "完了済みの生産計画には日産計画を登録・変更できません。");
        }
    }

    private void validateRequest(
            LocalDate scheduleDate,
            Integer plannedQuantity) {

        if (scheduleDate == null) {
            throw new BusinessException(
                    "日産計画日を入力してください。");
        }

        if (plannedQuantity == null
                || plannedQuantity <= 0) {

            throw new BusinessException(
                    "日産計画数は1以上にしてください。");
        }
    }

    private void validateScheduleMonth(
            ProductionOrder productionOrder,
            LocalDate scheduleDate) {

        YearMonth scheduleMonth =
                YearMonth.from(scheduleDate);

        if (!scheduleMonth.equals(
                productionOrder.getPlanMonth())) {

            throw new BusinessException(
                    "日産計画日は生産計画の対象月内にしてください。");
        }
    }

    private void validateDuplicateForCreate(
            Long productionOrderId,
            LocalDate scheduleDate) {

        if (productionScheduleRepository
                .existsByProductionOrderIdAndScheduleDate(
                        productionOrderId,
                        scheduleDate)) {

            throw new BusinessException(
                    "同じ日付の日産計画がすでに登録されています。");
        }
    }

    private void validateDuplicateForUpdate(
            Long productionOrderId,
            LocalDate scheduleDate,
            Long id) {

        if (productionScheduleRepository
                .existsByProductionOrderIdAndScheduleDateAndIdNot(
                        productionOrderId,
                        scheduleDate,
                        id)) {

            throw new BusinessException(
                    "同じ日付の日産計画がすでに登録されています。");
        }
    }

    private void validateMonthlyCapacity(
            ProductionOrder productionOrder,
            Integer plannedQuantity,
            Long excludedId) {

        long allocatedQuantity;

        if (excludedId == null) {
            allocatedQuantity =
                    productionScheduleRepository
                            .sumAllocatedQuantity(
                                    productionOrder.getId(),
                                    CANCELLED);
        } else {
            allocatedQuantity =
                    productionScheduleRepository
                            .sumAllocatedQuantityExcludingId(
                                    productionOrder.getId(),
                                    CANCELLED,
                                    excludedId);
        }

        if (allocatedQuantity + plannedQuantity
                > productionOrder.getPlannedQuantity()) {

            throw new BusinessException(
                    "日産計画の合計が月間計画数を超えています。");
        }
    }

    private int toInt(
            Long value) {

        if (value == null) {
            return 0;
        }

        return Math.toIntExact(value);
    }
}
