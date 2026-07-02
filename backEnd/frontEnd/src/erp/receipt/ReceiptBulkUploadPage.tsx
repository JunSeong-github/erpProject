import { useRef, useState, type CSSProperties } from "react";
import { useNavigate } from "react-router-dom";
import * as XLSX from "xlsx";
import {
    BulkPreviewResponse,
    previewReceiptBulk,
    uploadReceiptBulk,
} from "../api";

// 백엔드 파서가 인식하는 헤더명(한글). 템플릿과 순서를 맞춘다.
// 발주 라인은 '품목코드'로 지정한다(같은 발주에 동일 품목이 여러 라인이면 '발주라인번호'로 지정).
const TEMPLATE_HEADERS = ["발주번호", "품목코드", "입고수량", "입고일", "입고비고", "라인비고"];

/** 빈 템플릿(.xlsx) 다운로드 */
function downloadTemplate() {
    const aoa: (string | number)[][] = [
        TEMPLATE_HEADERS,
        // 예시 행(사용자가 지우고 실제 데이터 입력)
        [1001, "ITEM-001", 10, "2026-07-02", "7월 정기입고", "정상 입고"],
    ];
    const ws = XLSX.utils.aoa_to_sheet(aoa);
    ws["!cols"] = [{ wch: 12 }, { wch: 14 }, { wch: 10 }, { wch: 14 }, { wch: 18 }, { wch: 18 }];
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, "대량입고");
    XLSX.writeFile(wb, "대량입고_양식.xlsx");
}

export default function ReceiptBulkUploadPage() {
    const navigate = useNavigate();
    const fileInputRef = useRef<HTMLInputElement>(null);

    const [file, setFile] = useState<File | null>(null);
    const [preview, setPreview] = useState<BulkPreviewResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [errorMsg, setErrorMsg] = useState<string | null>(null);

    const onSelectFile = (f: File | null) => {
        setFile(f);
        setPreview(null);
        setErrorMsg(null);
    };

    const handlePreview = async () => {
        if (!file) {
            alert("엑셀 파일을 먼저 선택해 주세요.");
            return;
        }
        setLoading(true);
        setErrorMsg(null);
        setPreview(null);
        try {
            const res = await previewReceiptBulk(file);
            setPreview(res);
        } catch (e: any) {
            setErrorMsg(
                e?.response?.headers?.["x-error-detail"] ??
                e?.response?.data?.message ??
                e?.message ??
                "미리보기 중 오류가 발생했습니다."
            );
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = async () => {
        if (!file || !preview?.confirmable) return;
        if (!window.confirm(`정상 ${preview.validRows}건을 입고 등록합니다. 진행할까요?`)) return;

        setSaving(true);
        setErrorMsg(null);
        try {
            const res = await uploadReceiptBulk(file);
            if (res.failRows > 0) {
                // 미리보기 이후 데이터가 바뀌어 서버 재검증에서 걸린 경우
                const detail = res.errors.map((x) => `${x.row}행: ${x.message}`).join("\n");
                setErrorMsg("확정 중 검증 오류가 발생하여 저장되지 않았습니다.\n" + detail);
                // 최신 상태로 미리보기 갱신
                await handlePreview();
                return;
            }
            alert(`입고 등록 완료: ${res.successRows}건 저장, 입고건 ${res.createdReceipts}건 생성`);
            navigate("/erp/po");
        } catch (e: any) {
            setErrorMsg(
                e?.response?.headers?.["x-error-detail"] ??
                e?.response?.data?.message ??
                e?.message ??
                "저장 중 오류가 발생했습니다."
            );
        } finally {
            setSaving(false);
        }
    };

    const cell: CSSProperties = { border: "1px solid #ccc", padding: "4px 6px", fontSize: 13 };
    const th: CSSProperties = { ...cell, background: "#f2f2f2", whiteSpace: "nowrap" };

    return (
        <div>
            <h2>대량 입고 엑셀 업로드</h2>

            <div style={{ marginBottom: 10, display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
                <button type="button" onClick={downloadTemplate}>
                    양식 다운로드
                </button>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept=".xlsx,.xls"
                    onChange={(e) => onSelectFile(e.target.files?.[0] ?? null)}
                />
                <button type="button" onClick={handlePreview} disabled={!file || loading}>
                    {loading ? "미리보기 중..." : "미리보기"}
                </button>
                <button type="button" onClick={() => navigate("/erp/po")}>
                    목록으로
                </button>
            </div>

            <div style={{ marginBottom: 10, color: "#666", fontSize: 13 }}>
                ※ 필수 컬럼: <b>발주번호</b>, <b>품목코드</b>(또는 <b>발주라인번호</b>), <b>입고수량</b> / 선택: 입고일(미입력 시 오늘), 입고비고, 라인비고
                <br />
                ※ 같은 발주에 동일 품목코드가 여러 라인이면 품목코드로는 특정할 수 없으므로 <b>발주라인번호</b> 컬럼으로 지정해 주세요.
            </div>

            {errorMsg && (
                <div style={{ marginBottom: 10, padding: 8, background: "#fdecea", border: "1px solid #e74c3c", color: "#a93226", whiteSpace: "pre-wrap" }}>
                    {errorMsg}
                </div>
            )}

            {preview && (
                <>
                    <div style={{ marginBottom: 8, display: "flex", gap: 16, alignItems: "center" }}>
                        <span>전체 <b>{preview.totalRows}</b>건</span>
                        <span style={{ color: "#2e7d32" }}>정상 <b>{preview.validRows}</b>건</span>
                        <span style={{ color: "#c0392b" }}>오류 <b>{preview.errorRows}</b>건</span>
                        <button
                            type="button"
                            onClick={handleConfirm}
                            disabled={!preview.confirmable || saving}
                            style={{ marginLeft: "auto", padding: "6px 14px" }}
                            title={preview.confirmable ? "" : "오류 행이 있어 확정할 수 없습니다. 엑셀을 수정 후 다시 미리보기 하세요."}
                        >
                            {saving ? "저장 중..." : "확정(저장)"}
                        </button>
                    </div>

                    {!preview.confirmable && preview.errorRows > 0 && (
                        <div style={{ marginBottom: 8, color: "#c0392b", fontSize: 13 }}>
                            ※ 오류 행이 있어 저장할 수 없습니다. 빨간색으로 표시된 행을 수정한 뒤 다시 업로드해 주세요.
                        </div>
                    )}

                    <table style={{ borderCollapse: "collapse", width: "100%" }}>
                        <thead>
                            <tr>
                                <th style={th}>행</th>
                                <th style={th}>발주번호</th>
                                <th style={th}>품목코드</th>
                                <th style={th}>발주라인번호</th>
                                <th style={th}>품목명</th>
                                <th style={th}>입고수량</th>
                                <th style={th}>입고일</th>
                                <th style={th}>입고비고</th>
                                <th style={th}>라인비고</th>
                                <th style={th}>결과</th>
                            </tr>
                        </thead>
                        <tbody>
                            {preview.rows.map((r) => (
                                <tr key={r.rowNo} style={{ background: r.valid ? undefined : "#fdecea" }}>
                                    <td style={{ ...cell, textAlign: "center" }}>{r.rowNo}</td>
                                    <td style={cell}>{r.poId ?? ""}</td>
                                    <td style={cell}>{r.itemCode ?? ""}</td>
                                    <td style={cell}>{r.poItemId ?? ""}</td>
                                    <td style={cell}>{r.itemName ?? ""}</td>
                                    <td style={{ ...cell, textAlign: "right" }}>{r.receivedQty ?? ""}</td>
                                    <td style={cell}>{r.receiptDate ?? ""}</td>
                                    <td style={cell}>{r.remark ?? ""}</td>
                                    <td style={cell}>{r.lineRemark ?? ""}</td>
                                    <td style={{ ...cell, color: r.valid ? "#2e7d32" : "#c0392b" }}>
                                        {r.valid ? "정상" : r.error}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </>
            )}
        </div>
    );
}
