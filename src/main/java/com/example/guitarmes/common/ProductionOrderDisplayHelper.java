package com.example.guitarmes.common;

import org.springframework.stereotype.Component;

@Component("productionOrderDisplay")
public class ProductionOrderDisplayHelper {

    /**
     * ProductionOrderの状態を
     * 日本語の表示名へ変換する。
     */
    public String getLabel(
            String status) {

        if (status == null
                || status.isBlank()) {

            return "未設定";
        }

        return switch (status) {

            case ProductionOrderStatusConstants.PLANNED ->
                    "計画中";

            case ProductionOrderStatusConstants.IN_PROGRESS ->
                    "製造中";

            case ProductionOrderStatusConstants.COMPLETED ->
                    "完了";

            case ProductionOrderStatusConstants.CANCELLED ->
                    "中止";

            default ->
                    status;
        };
    }

    /**
     * ProductionOrder状態用CSSクラスを返す。
     */
    public String getCssClass(
            String status) {

        if (status == null
                || status.isBlank()) {

            return "status-unknown";
        }

        return switch (status) {

            case ProductionOrderStatusConstants.PLANNED ->
                    "status-planned";

            case ProductionOrderStatusConstants.IN_PROGRESS ->
                    "status-working";

            case ProductionOrderStatusConstants.COMPLETED ->
                    "status-available";

            case ProductionOrderStatusConstants.CANCELLED ->
                    "status-rejected";

            default ->
                    "status-unknown";
        };
    }
}