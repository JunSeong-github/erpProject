package erp.backEnd.enumeration;

import lombok.Getter;

@Getter
public enum NotificationType {

    PO_REQUESTED("발주 승인 요청"),
    PO_APPROVED("발주 승인"),
    PO_REJECTED("발주 반려"),
    STOCK_USAGE_REQUESTED("재고사용 승인 요청"),
    STOCK_USAGE_APPROVED("재고사용 승인"),
    STOCK_USAGE_REJECTED("재고사용 반려");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }
}
