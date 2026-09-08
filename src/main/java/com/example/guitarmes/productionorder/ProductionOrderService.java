package com.example.guitarmes.productionorder;

import static com.example.guitarmes.productionorder.ProductionOrderStatusConstants.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.guitar.GuitarRepository;
import com.example.guitarmes.product.Product;
import com.example.guitarmes.product.ProductRepository;

@Service
public class ProductionOrderService {

    private final ProductionOrderRepository productionOrderRepository;
    private final ProductRepository productRepository;
    private final GuitarRepository guitarRepository;

    public ProductionOrderService(
            ProductionOrderRepository productionOrderRepository,
            ProductRepository productRepository,
            GuitarRepository guitarRepository) {
        this.productionOrderRepository = productionOrderRepository;
        this.productRepository = productRepository;
        this.guitarRepository = guitarRepository;
    }

    public List<ProductionOrder> getProductionOrders() {
        return productionOrderRepository.findAllByOrderByIdDesc();
    }

    /** 未指定・空・未知のカテゴリは進行中へ正規化する。 */
    public String normalizeCategory(String category) {
        String normalized = normalizeSearch(category);
        return "completed".equals(normalized) || "cancelled".equals(normalized)
                ? normalized : "active";
    }

    public List<String> getCategoryStatuses(String category) {
        return switch (normalizeCategory(category)) {
            case "completed" -> List.of(COMPLETED);
            case "cancelled" -> List.of(CANCELLED);
            default -> List.of(PLANNED, IN_PROGRESS);
        };
    }

    /** 日付や数量ではなく既存statusだけで分類する。 */
    public List<ProductionOrder> filterByCategory(List<ProductionOrder> orders, String category) {
        var statuses = getCategoryStatuses(category).stream().map(this::normalizeSearch).toList();
        return orders.stream()
                .filter(order -> order != null && statuses.contains(normalizeSearch(order.getStatus())))
                .toList();
    }

    public List<ProductionOrder> filterProductionOrders(List<ProductionOrder> orders,
            String orderNo, String product, String status, String planMonth,
            String dueFrom, String dueTo) {
        String number = normalizeSearch(orderNo);
        String productText = normalizeSearch(product);
        String state = normalizeSearch(status);
        YearMonth month;
        LocalDate from;
        LocalDate to;
        try {
            month = normalizeSearch(planMonth).isEmpty() ? null : YearMonth.parse(planMonth.trim());
            from = normalizeSearch(dueFrom).isEmpty() ? null : LocalDate.parse(dueFrom.trim());
            to = normalizeSearch(dueTo).isEmpty() ? null : LocalDate.parse(dueTo.trim());
        } catch (java.time.format.DateTimeParseException exception) {
            throw new BusinessException("計画月はyyyy-MM、納期はyyyy-MM-dd形式で正しく入力してください。");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("納期の開始日は終了日以前にしてください。");
        }
        return orders.stream()
                .filter(order -> number.isEmpty() || normalizeSearch(order.getOrderNo()).contains(number))
                .filter(order -> productText.isEmpty() || (order.getProduct() != null
                        && (normalizeSearch(order.getProduct().getProductName()).contains(productText)
                        || normalizeSearch(order.getProduct().getModelNo()).contains(productText))))
                .filter(order -> state.isEmpty() || normalizeSearch(order.getStatus()).equals(state))
                .filter(order -> month == null || month.equals(order.getPlanMonth()))
                .filter(order -> from == null || (order.getDueDate() != null && !order.getDueDate().isBefore(from)))
                .filter(order -> to == null || (order.getDueDate() != null && !order.getDueDate().isAfter(to)))
                .toList();
    }

    public boolean hasSearchCondition(String... conditions) {
        return java.util.Arrays.stream(conditions).anyMatch(value -> !normalizeSearch(value).isEmpty());
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public ProductionOrder getProductionOrderById(Long id) {
        return productionOrderRepository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された生産指示が存在しません。"));
    }

    public ProductionOrderUpdateRequest
            getProductionOrderUpdateRequest(Long id) {
        ProductionOrder order = getProductionOrderById(id);
        validateEditable(order);

        ProductionOrderUpdateRequest request =
                new ProductionOrderUpdateRequest();
        request.setProductId(order.getProduct().getId());
        request.setPlannedQuantity(order.getPlannedQuantity());
        request.setPlanMonth(order.getPlanMonth());
        request.setPlannedStartDate(order.getPlannedStartDate());
        request.setDueDate(order.getDueDate());
        return request;
    }

    @Transactional
    public ProductionOrder createProductionOrder(
            Long productId,
            Integer plannedQuantity,
            YearMonth planMonth,
            LocalDate plannedStartDate,
            LocalDate dueDate) {
        validateRequest(
                productId,
                plannedQuantity,
                planMonth,
                plannedStartDate,
                dueDate);
        Product product = getProduct(productId);
        ProductionOrder productionOrder =
                new ProductionOrder(
                        generateOrderNo(),
                        product,
                        plannedQuantity,
                        planMonth,
                        plannedStartDate,
                        dueDate,
                        PLANNED);
        return productionOrderRepository.save(productionOrder);
    }

    @Transactional
    public ProductionOrder updateProductionOrder(
            Long id,
            ProductionOrderUpdateRequest request) {
        if (request == null) {
            throw new BusinessException(
                    "生産計画の更新内容が指定されていません。");
        }

        ProductionOrder order = getProductionOrderById(id);
        validateEditable(order);
        validateRequest(
                request.getProductId(),
                request.getPlannedQuantity(),
                request.getPlanMonth(),
                request.getPlannedStartDate(),
                request.getDueDate());
        Product product = getProduct(request.getProductId());

        order.setProduct(product);
        order.setPlannedQuantity(request.getPlannedQuantity());
        order.setPlanMonth(request.getPlanMonth());
        order.setPlannedStartDate(request.getPlannedStartDate());
        order.setDueDate(request.getDueDate());
        return productionOrderRepository.save(order);
    }

    @Transactional
    public ProductionOrder cancelProductionOrder(Long id) {
        ProductionOrder order = getProductionOrderById(id);
        validateEditable(order);
        order.setStatus(CANCELLED);
        return productionOrderRepository.save(order);
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された製品が存在しません。"));
    }

    private void validateEditable(ProductionOrder order) {
        if (!PLANNED.equals(order.getStatus())) {
            throw new BusinessException(
                    "計画中の生産指示だけ編集・取消できます。");
        }
        if (!Integer.valueOf(0).equals(order.getStartedQuantity())
                || !Integer.valueOf(0).equals(order.getCompletedQuantity())) {
            throw new BusinessException(
                    "着手済みまたは完成実績がある生産指示は編集・取消できません。");
        }
        if (guitarRepository.existsByProductionOrderId(order.getId())) {
            throw new BusinessException(
                    "ギター個体が発行済みの生産指示は編集・取消できません。");
        }
    }

    private void validateRequest(
            Long productId,
            Integer plannedQuantity,
            YearMonth planMonth,
            LocalDate plannedStartDate,
            LocalDate dueDate) {
        if (productId == null) {
            throw new BusinessException(
                    "対象製品を選択してください。");
        }
        if (plannedQuantity == null || plannedQuantity <= 0) {
            throw new BusinessException(
                    "計画数は1以上で入力してください。");
        }
        if (planMonth == null) {
            throw new BusinessException(
                    "対象月を入力してください。");
        }
        if (plannedStartDate == null) {
            throw new BusinessException(
                    "生産開始予定日を入力してください。");
        }
        if (dueDate == null) {
            throw new BusinessException(
                    "納期を入力してください。");
        }
        LocalDate today = LocalDate.now();
        LocalDate maximumDate = today.plusYears(5);
        if (plannedStartDate.isBefore(today)) {
            throw new BusinessException(
                    "生産開始予定日は当日以降にしてください。");
        }
        if (plannedStartDate.isAfter(maximumDate)
                || dueDate.isAfter(maximumDate)) {
            throw new BusinessException(
                    "日付は当日から5年以内で入力してください。");
        }

        if (plannedStartDate != null
                && dueDate != null
                && dueDate.isBefore(plannedStartDate)) {
            throw new BusinessException(
                    "納期は生産開始予定日以降にしてください。");
        }
    }

    private String generateOrderNo() {
        String year = String.valueOf(LocalDate.now().getYear())
                .substring(2);
        String prefix = "PO" + year;
        ProductionOrder lastOrder = productionOrderRepository
                .findTopByOrderNoStartingWithOrderByOrderNoDesc(prefix)
                .orElse(null);
        int nextNumber = 1;
        if (lastOrder != null) {
            nextNumber = Integer.parseInt(
                    lastOrder.getOrderNo().substring(4)) + 1;
        }
        return prefix + String.format("%04d", nextNumber);
    }
}
