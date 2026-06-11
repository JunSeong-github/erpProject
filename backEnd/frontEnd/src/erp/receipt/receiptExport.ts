import * as XLSX from "xlsx";

/** 입고 전표 내보내기에 필요한 데이터 형태 */
export type ReceiptExportLine = {
    itemName: string;
    unitPrice: number;
    orderedQty: number;
    amount: number;
    totalReceivedQty: number;
    remainingQty: number;
    lineRemark: string;
};

export type ReceiptExportData = {
    poId: number | string;
    vendorName: string;
    vendorCode: string;
    deliveryDate: string;
    poStatusLabel: string;
    poEtc: string;
    headerRemark: string;
    lines: ReceiptExportLine[];
};

const TABLE_HEADERS = [
    "품목",
    "발주당시가격",
    "요청수량",
    "합계가격",
    "누적입고수량",
    "잔량",
    "라인비고",
];

const fileBaseName = (data: ReceiptExportData) =>
    `입고전표_PO${data.poId}_${data.vendorName || ""}`.replace(/[\\/:*?"<>|]/g, "_");

/** 엑셀(.xlsx) 다운로드 */
export function downloadReceiptExcel(data: ReceiptExportData) {
    const headerRows: (string | number)[][] = [
        ["입고 전표"],
        [],
        ["PO ID", data.poId, "", "상태", data.poStatusLabel],
        ["공급사", `${data.vendorName} (${data.vendorCode})`, "", "납기요청일", data.deliveryDate],
        ["발주 비고", data.poEtc, "", "입고 비고", data.headerRemark],
        [],
    ];

    const tableRows = data.lines.map((l) => [
        l.itemName,
        l.unitPrice,
        l.orderedQty,
        l.amount,
        l.totalReceivedQty,
        l.remainingQty,
        l.lineRemark,
    ]);

    const aoa = [...headerRows, TABLE_HEADERS, ...tableRows];

    const ws = XLSX.utils.aoa_to_sheet(aoa);
    ws["!cols"] = [
        { wch: 18 },
        { wch: 14 },
        { wch: 12 },
        { wch: 14 },
        { wch: 14 },
        { wch: 10 },
        { wch: 20 },
    ];

    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "입고전표");
    XLSX.writeFile(wb, `${fileBaseName(data)}.xlsx`);
}

const esc = (v: unknown) =>
    String(v ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");

/** PDF 다운로드 (브라우저 인쇄 → PDF로 저장) */
export function printReceiptPdf(data: ReceiptExportData) {
    const rows = data.lines
        .map(
            (l) => `
            <tr>
                <td>${esc(l.itemName)}</td>
                <td class="num">${esc(l.unitPrice)}</td>
                <td class="num">${esc(l.orderedQty)}</td>
                <td class="num">${esc(l.amount)}</td>
                <td class="num">${esc(l.totalReceivedQty)}</td>
                <td class="num">${esc(l.remainingQty)}</td>
                <td>${esc(l.lineRemark)}</td>
            </tr>`
        )
        .join("");

    const html = `<!doctype html>
<html lang="ko">
<head>
<meta charset="utf-8" />
<title>${esc(fileBaseName(data))}</title>
<style>
    * { font-family: "Malgun Gothic", "맑은 고딕", sans-serif; }
    body { margin: 24px; color: #222; }
    h1 { font-size: 20px; text-align: center; margin: 0 0 16px; }
    .info { width: 100%; border-collapse: collapse; margin-bottom: 16px; }
    .info th, .info td { border: 1px solid #999; padding: 6px 8px; font-size: 12px; text-align: left; }
    .info th { background: #f2f2f2; width: 110px; white-space: nowrap; }
    table.lines { width: 100%; border-collapse: collapse; }
    table.lines th, table.lines td { border: 1px solid #999; padding: 6px; font-size: 12px; }
    table.lines th { background: #f2f2f2; }
    table.lines td.num { text-align: right; }
    @media print { body { margin: 0; } }
</style>
</head>
<body>
    <h1>입고 전표</h1>
    <table class="info">
        <tr><th>PO ID</th><td>${esc(data.poId)}</td><th>상태</th><td>${esc(data.poStatusLabel)}</td></tr>
        <tr><th>공급사</th><td>${esc(data.vendorName)} (${esc(data.vendorCode)})</td><th>납기요청일</th><td>${esc(data.deliveryDate)}</td></tr>
        <tr><th>발주 비고</th><td>${esc(data.poEtc)}</td><th>입고 비고</th><td>${esc(data.headerRemark)}</td></tr>
    </table>
    <table class="lines">
        <thead>
            <tr>${TABLE_HEADERS.map((h) => `<th>${esc(h)}</th>`).join("")}</tr>
        </thead>
        <tbody>${rows}</tbody>
    </table>
</body>
</html>`;

    const win = window.open("", "_blank");
    if (!win) {
        alert("팝업이 차단되어 PDF 인쇄 창을 열 수 없습니다. 팝업 차단을 해제해 주세요.");
        return;
    }
    win.document.open();
    win.document.write(html);
    win.document.close();
    win.focus();
    win.onload = () => {
        win.print();
    };
}
