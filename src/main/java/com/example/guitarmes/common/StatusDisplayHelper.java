package com.example.guitarmes.common;

import org.springframework.stereotype.Component;

@Component("statusDisplay")
public class StatusDisplayHelper {

    /**
     * Body・Neckの内部ステータスを
     * 画面表示用の日本語へ変換する。
     */
    public String getLabel(
            String status) {

        if (status == null
                || status.isBlank()) {

            return "未設定";
        }

        return switch (status) {

            case StatusConstants.WAITING ->
                    "工程待ち";

            case StatusConstants.WORKING ->
                    "作業中";

            case StatusConstants.WAITING_INSPECTION ->
                    "検品待ち";

            case StatusConstants.REWORK ->
                    "手直し待ち";

            case StatusConstants.AVAILABLE ->
                    "組立可能";

            case StatusConstants.RETURNED ->
                    "塗装前工程へ差し戻し";

            case StatusConstants.ASSEMBLED ->
                    "組立済み";

            case StatusConstants.REJECTED ->
                    "不合格";

            default ->
                    status;
        };
    }

    /**
     * ステータスに対応するCSSクラスを返す。
     */
    public String getCssClass(
            String status) {

        if (status == null
                || status.isBlank()) {

            return "status-unknown";
        }

        return switch (status) {

            case StatusConstants.WAITING ->
                    "status-waiting";

            case StatusConstants.WORKING ->
                    "status-working";

            case StatusConstants.WAITING_INSPECTION ->
                    "status-inspection";

            case StatusConstants.REWORK ->
                    "status-rework";

            case StatusConstants.AVAILABLE ->
                    "status-available";

            case StatusConstants.RETURNED ->
                    "status-returned";

            case StatusConstants.ASSEMBLED ->
                    "status-assembled";

            case StatusConstants.REJECTED ->
                    "status-rejected";

            default ->
                    "status-unknown";
        };
    }
}