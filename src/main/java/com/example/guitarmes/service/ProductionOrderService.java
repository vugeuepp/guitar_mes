package com.example.guitarmes.service;

import static com.example.guitarmes.common
        .ProductionOrderStatusConstants.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.guitarmes.entity.Product;
import com.example.guitarmes.entity.ProductionOrder;
import com.example.guitarmes.exception.BusinessException;
import com.example.guitarmes.exception.NotFoundException;
import com.example.guitarmes.repository.ProductRepository;
import com.example.guitarmes.repository.ProductionOrderRepository;

@Service
public class ProductionOrderService {

    private final ProductionOrderRepository
            productionOrderRepository;

    private final ProductRepository
            productRepository;

    public ProductionOrderService(
            ProductionOrderRepository
                    productionOrderRepository,
            ProductRepository productRepository) {

        this.productionOrderRepository =
                productionOrderRepository;

        this.productRepository =
                productRepository;
    }

    public List<ProductionOrder>
            getProductionOrders() {

        return productionOrderRepository
                .findAllByOrderByIdDesc();
    }

    public ProductionOrder getProductionOrderById(
            Long id) {

        return productionOrderRepository
                .findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "指定された生産指示が存在しません。"));
    }

    @Transactional
    public ProductionOrder createProductionOrder(
            Long productId,
            Integer plannedQuantity,
            LocalDate plannedStartDate,
            LocalDate dueDate) {

        validateCreateRequest(
                plannedQuantity,
                plannedStartDate,
                dueDate);

        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "指定された製品が存在しません。"));

        ProductionOrder productionOrder =
                new ProductionOrder(
                        generateOrderNo(),
                        product,
                        plannedQuantity,
                        plannedStartDate,
                        dueDate,
                        PLANNED);

        return productionOrderRepository
                .save(productionOrder);
    }

    private void validateCreateRequest(
            Integer plannedQuantity,
            LocalDate plannedStartDate,
            LocalDate dueDate) {

        if (plannedQuantity == null
                || plannedQuantity <= 0) {

            throw new BusinessException(
                    "計画数は1以上で入力してください。");
        }

        if (plannedStartDate != null
                && dueDate != null
                && dueDate.isBefore(
                        plannedStartDate)) {

            throw new BusinessException(
                    "納期は生産開始予定日以降にしてください。");
        }
    }

    private String generateOrderNo() {

        String year =
                String.valueOf(
                        LocalDate.now().getYear())
                        .substring(2);

        String prefix =
                "PO" + year;

        ProductionOrder lastOrder =
                productionOrderRepository
                        .findTopByOrderNoStartingWithOrderByOrderNoDesc(
                                prefix)
                        .orElse(null);

        int nextNumber = 1;

        if (lastOrder != null) {

            String lastOrderNo =
                    lastOrder.getOrderNo();

            nextNumber =
                    Integer.parseInt(
                            lastOrderNo.substring(4))
                    + 1;
        }

        return prefix
                + String.format(
                        "%04d",
                        nextNumber);
    }
}